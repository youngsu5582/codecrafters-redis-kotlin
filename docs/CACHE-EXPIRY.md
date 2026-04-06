## Cache 만료(Expiry) 설계

### 문제: timeTable과 cache 미연결

기존 `SortedSet<Long>`에는 만료 시간만 저장되고, 어떤 key가 만료되는지 연결이 없었음.
→ `removeExpiryData()`가 timeTable을 정리해도 cache에서는 아무것도 삭제되지 않는 버그.

### 자료구조 선택

| 방식 | 장점 | 단점 |
|------|------|------|
| `SortedSet<Long>` | 만료 시간 정렬 | key와 연결 안 됨 |
| `Deque<Pair<Long, String>>` | 삽입 순서 = 만료 순서일 때 효율적 | TTL이 key마다 다르면 순서 보장 안 됨 |
| `HashMap<String, Long>` | 단순하고 정확 | 만료 스캔 시 전체 순회 O(N) |
| **`TreeMap<Long, MutableSet<String>>`** | 정렬 + 같은 시간 여러 key 처리 | - |

**결론**: `TreeMap<Long, MutableSet<String>>` 채택

- SortedSet처럼 앞에서부터 확인하고 early break 가능
- data class + `compareTo` 방식은 같은 만료 시간 시 `SortedSet`이 `compareTo == 0`을 동일 원소로 취급하는 문제
- TreeMap은 같은 시간에 여러 key를 `MutableSet`으로 자연스럽게 처리

---

### Redis 실제 구현

#### 자료구조

```
db->dict    : 메인 keyspace (key → value)
db->expires : 만료 전용 dict (key → 만료 시각)
```

- 만료가 설정된 key만 `expires` dict에 존재
- key의 SDS는 메인 dict과 공유하여 메모리 절약

#### 2가지 전략 병행

**1) Passive (Lazy) — `expireIfNeeded()`**

- GET 시 해당 key의 만료 시각 확인, 만료됐으면 삭제 후 nil 반환
- O(1), 접근 시에만 동작
- 한계: 아무도 접근하지 않는 key는 메모리에 영원히 남음

**2) Active — `activeExpireCycle()`**

주기적으로 (default `hz=10`, 초당 10회) 실행:

```
loop:
  1. expires dict에서 랜덤 20개 key 샘플링
  2. 만료된 key 전부 삭제
  3. 만료 비율 > 25% → loop 반복
  4. 만료 비율 ≤ 25% 또는 시간 예산 초과 → 종료
```

- **25% 임계값**이 핵심 — "아직 많이 남아있으면 계속 정리"
- CPU 시간 예산이 있어서 클라이언트 응답을 블로킹하지 않음

#### Redis 6+ 개선

- Radix Tree로 곧 만료될 key 우선 탐색
- Redis 5에서 최대 25%까지 만료 key가 메모리에 잔존하던 문제 해결

#### 우리 구현과의 매핑

| Redis | 우리 Cache |
|-------|-----------|
| `db->expires` (key → 만료시각) | `HashMap<String, Long>` — get 시 lazy 체크 |
| `activeExpireCycle` (주기적 정리) | `TreeMap<Long, MutableSet<String>>` — headMap으로 만료 구간 정리 |

---

### headMap 활용

`TreeMap.headMap(toKey)`는 toKey보다 작은 모든 entry의 **view**를 반환한다.

```
TreeMap: [100, 200, 300, 500, 800]
headMap(350) → [100, 200, 300]
```

view이므로 `clear()` 하면 원본에서도 제거된다.

```kotlin
fun removeExpiryData() {
    val checkTime = clock.putTime()
    val expired = timeTable.headMap(checkTime, true)
    expired.values.forEach { keys -> keys.forEach { cache.remove(it) } }
    expired.clear()
}
```

#### headMap 없이 구현

```kotlin
fun removeExpiryData() {
    val checkTime = clock.putTime()
    val iterator = timeTable.iterator()
    while (iterator.hasNext()) {
        val entry = iterator.next()
        if (entry.key > checkTime) break
        entry.value.forEach { cache.remove(it) }
        iterator.remove()
    }
}
```

순회 중 `map.remove()`는 `ConcurrentModificationException` 발생 → `iterator.remove()` 사용해야 함.

#### SortedMap vs NavigableMap

```
Map
 └─ SortedMap        ← headMap(toKey) 정의
     └─ NavigableMap  ← headMap(toKey, inclusive) 오버로드 추가
         └─ TreeMap
```

- `sortedMapOf()` → `SortedMap` 타입 → `headMap(toKey)`만 노출
- `TreeMap()` 직접 생성 → `NavigableMap` 메서드까지 사용 가능

---

### 시간 처리

| | `System.nanoTime()` | `System.currentTimeMillis()` |
|---|---|---|
| 기준점 | JVM 시작 시점 (임의) | 1970-01-01 UTC (epoch) |
| 단위 | 나노초 | 밀리초 |
| 용도 | 경과 시간 측정 (stopwatch) | 절대 시각 |

- TTL은 "언제 만료되는가"이므로 절대 시각 → `System.currentTimeMillis()` 사용
- `LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli()`는 불필요한 오버헤드
- Redis도 내부적으로 milliseconds long 사용

---

### 테스트: MockClock

```kotlin
class MockClock(
    private val putTimeQueue: ArrayDeque<Long> = ArrayDeque(),
    private val expiredTimeQueue: ArrayDeque<Long> = ArrayDeque()
) : Clock { ... }
```

테스트에서는 Long 직접 사용이 Instant보다 나음 — deterministic하고 변환 불필요.

```kotlin
@Test
fun `만료시간이 지나면, 데이터를 제거한다`() {
    val putQueue = ArrayDeque(listOf(1000L))
    val expiredQueue = ArrayDeque(listOf(1006L))
    val clock = MockClock(putQueue, expiredQueue)
    val cache = Cache(clock)

    cache.put("ttl-key", "1234", 5)  // 만료 시각: 1000 + 5 = 1005
    assertNull(cache.get("ttl-key")) // get 시점: 1006 > 1005 → 만료
}
```

---

### synchronized

- `cache`와 `timeTable`을 동시에 보호하려면 하나의 lock 객체 사용
- lock 분리 시 데드락 + 두 자료구조 간 불일치 위험
- Redis는 싱글 스레드라 lock 자체 불필요
