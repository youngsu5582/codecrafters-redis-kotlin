import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

class CacheTests {
    @Test
    fun `데이터를 PUT 을 통해 넣고 GET 을 통해 각져온다`() {
        val cache = Cache()
        cache.put("key", "value")
        assertEquals("value", cache.get("key"))
    }

    @Test
    fun `없는 데이터는 NULL 을 반환한다`() {
        val cache = Cache()
        assertNull(cache.get("missing"))
    }

    @Test
    fun `만료시간 이전이면, 데이터가 존재한다`() {
        val putQueue = ArrayDeque<Long>()
        val expiredQueue = ArrayDeque<Long>()

        val time = System.currentTimeMillis()
        putQueue.add(time)
        expiredQueue.add(time + 5)

        // ttl 은 5ms 로 지정
        // 만료 시간은 10ms 로 설정

        val clock = MockClock(
            putTimeQueue = putQueue,
            expiredTimeQueue = expiredQueue
        )

        val cache = Cache(
            clock
        )

        cache.put("ttl-key", "1234", 10)

        val result = cache.get("ttl-key")
        assertNotNull(result)
    }

    @Test
    fun `만료시간이 지나면, 데이터를 제거한다`() {
        val putQueue = ArrayDeque<Long>()
        val expiredQueue = ArrayDeque<Long>()

        val time = System.currentTimeMillis()
        putQueue.add(time)
        expiredQueue.add(time + 5)

        // ttl 은 4ms 로 지정
        // 만료 시간은 5ms 로 설정

        val clock = MockClock(
            putTimeQueue = putQueue,
            expiredTimeQueue = expiredQueue
        )

        val cache = Cache(
            clock
        )

        cache.put("ttl-key", "1234", 4)

        val result = cache.get("ttl-key")
        assertNull(result)
    }

    @Test
    fun `RPUSH 를 하면, 배열 내 요소의 숫자를 반환한다`() {
        val cache = Cache()
        assertTrue { cache.rightPush("key", listOf("value1")) == 1 }
        assertTrue { cache.rightPush("key", listOf("value2")) == 2 }
    }

    @Test
    fun `RPUSH 는 여러개의 요소를 한번에 삽입할 수 있다`() {
        val cache = Cache()
        assertTrue { cache.rightPush("key", listOf("value1", "value2")) == 2 }
    }
}
