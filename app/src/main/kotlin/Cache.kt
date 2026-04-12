import clock.Clock
import clock.SystemClock
import util.CustomLogger
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class Cache(
    private val clock: Clock = SystemClock()
) {

    // time set 도 필요..?
    // -> Expiry 단계 진행하며, 테이블 추가
    private val timeTable = TreeMap<Long, MutableSet<String>>()
    private val cache = hashMapOf<String, String>()
    private val arrayCache = hashMapOf<String, Deque<String>>()

    // 시간 테스팅 용이하게 하기 위한 인터페이스 주입

    fun rightPush(key: String, elements: List<String>): Int {
        val array: Deque<String> = arrayCache.getOrDefault(key, ArrayDeque())
        elements.forEach { array.addLast(it) }
        arrayCache[key] = array
        return array.size
    }

    fun leftPush(key: String, elements: List<String>): Int {
        val array: Deque<String> = arrayCache.getOrDefault(key, ArrayDeque())
        elements.forEach { array.addFirst(it) }
        arrayCache[key] = array
        return array.size
    }

    /**
     * list 의 start ~ stop 반환
     * - list 가 존재하지 않으면, 빈 배열 반환
     * - start 가 길이보다 크거나 같으면, 빈 배열 반환
     * - stop 이 길이보다 크거나 같으면, 마지막 인덱스로 간주
     * - start 가 stop 보다 크면, 빈 배열 반환
     */
    fun leftRange(key: String, start: Int, stop: Int): List<String> {
        val queue: Deque<String> = arrayCache.getOrDefault(key, ArrayDeque())
        val array = mutableListOf<String>()
        val startIndex = if (start >= 0) start else queue.size + start
        val stopIndex = if (stop >= 0) stop else queue.size + stop

        for ((i, element) in queue.withIndex()) {
            if (i > stopIndex) break
            if (i >= startIndex) array.add(element)
        }
        return array
    }

    fun leftLength(key: String): Int {
        val queue: Deque<String> = arrayCache.getOrDefault(key, ArrayDeque())
        return queue.size
    }

    fun leftPop(key: String): String? {
        removeExpiryData()
        val queue: Deque<String> = arrayCache.getOrDefault(key, ArrayDeque())
        if (queue.isEmpty()) return null
        return queue.removeFirst()
    }

    fun put(key: String, value: String) {
        // TTL 지정하지 않았으면, LONG 의 최대값으로 처리
        CustomLogger.info("$key 에 $value 를 넣습니다. 만료시간: X")
        putImpl(key, value, Long.MAX_VALUE)
    }

    @OptIn(ExperimentalTime::class)
    fun put(key: String, value: String, ttl: Long) {
        val time = clock.putTime() + ttl
        CustomLogger.info("$key 에 $value 를 넣습니다. 만료시간: ${Instant.fromEpochMilliseconds(time)}")
        putImpl(key, value, time)
    }

    // 맞네... 이거, 시간 확인하려면 또 새로운 MAP 이 필요해진다;
    fun ttl(key: String): Long? {
        TODO("해당하는 Task 나오면 구현")
    }

    private fun putImpl(key: String, value: String, time: Long) {
        cache[key] = value

        if (time == Long.MAX_VALUE) {
            return
        }

        val entity = timeTable[time]

        // 없으면 새로운 set 만들어서 추가
        if (entity == null) {
            CustomLogger.info("$time 에 해당하는 값이 없어서 새로 추가합니다")
            timeTable[time] = mutableSetOf(key)
            return
        }

        // 존재하면, 기존에 추가
        entity.add(key)
    }

    fun get(key: String): String? {
        removeExpiryData()
        return cache[key]
    }

    @OptIn(ExperimentalTime::class)
    fun removeExpiryData() {
        val checkTime = clock.expiredTime()
        // 체크하는 타임보다 작은 경우 목록 반환
        val expired = timeTable.headMap(checkTime, true)
        CustomLogger.info("만료된 시간 목록: ${expired.map { Instant.fromEpochMilliseconds(it.key) }}")
        expired.values.forEach { keys ->
            keys.forEach {
                CustomLogger.info("$it 를 제거합니다.")
                cache.remove(it)
            }
        }
        expired.clear()
    }
}