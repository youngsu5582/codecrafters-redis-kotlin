import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

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

    @Nested
    inner class LeftRangeTest {
        val cache = Cache()

        @BeforeEach
        fun setup() {
            cache.rightPush("key", listOf("a", "b", "c", "d", "e"))
        }

        @Test
        fun `LRANGE 는 왼쪽부터 오른쪽으로 해당하는 범위를 반환한다`() {
            assertTrue { cache.leftRange("key", 0, 1) == listOf("a", "b") }
            assertTrue { cache.leftRange("key", 2, 4) == listOf("c", "d", "e") }
        }

        @Test
        fun `start 가 길이보다 크거나 같으면, 빈 배열을 반환한다`() {
            assertTrue { cache.leftRange("key", 5, 5) == emptyList<String>() }
        }

        @Test
        fun `stop 이 길이보다 크거나 같으면, 마지막 인덱스로 간주한다`() {
            assertTrue { cache.leftRange("key", 2, 1000) == listOf("c", "d", "e") }
        }

        @Test
        fun `start 가 stop 보다 크면, 빈 배열을 반환한다`() {
            assertTrue { cache.leftRange("key", 4, 2) == emptyList<String>() }
        }

        @Test
        fun `start stop 의 index 가 음수이면 배열의 크기에서 역으로 처리한다`() {
            assertTrue { cache.leftRange("key", -2, -1) == listOf("d", "e") }
            assertTrue { cache.leftRange("key", 0, -3) == listOf("a", "b", "c") }
        }
    }

    @Nested
    inner class LeftPushTest {

        @Test
        fun `LPUSH 를 하면, 배열 내 요소의 숫자를 반환한다`() {
            val cache = Cache()
            assertTrue { cache.leftPush("key", listOf("value1")) == 1 }
            assertTrue { cache.leftPush("key", listOf("value2")) == 2 }
        }

        @Test
        fun `LPUSH 는 여러개의 요소를 한번에 삽입할 수 있다`() {
            val cache = Cache()
            assertTrue { cache.leftPush("key", listOf("value1", "value2")) == 2 }
        }

        @Test
        fun `LPUSH 는 리스트 앞에 삽입한다`() {
            val cache = Cache()
            val key = "list_key"
            cache.leftPush(key, listOf("a", "b", "c"))
            assertTrue { cache.leftRange(key, 0, -1) == listOf("c", "b", "a") }
        }
    }

    @Nested
    inner class LeftLengthTest {
        @Test
        fun `LLEN 은 길이를 반환한다`() {
            val cache = Cache()
            val key = "list_key"
            cache.leftPush(key, listOf("a", "b", "c"))
            val length = cache.leftLength(key)
            assertTrue { length == 3 }
        }

        @Test
        fun `키가 없으면 0을 반환한다`() {
            val cache = Cache()
            val key = "not_exist_list_key"
            val length = cache.leftLength(key)
            assertTrue { length == 0 }
        }
    }

    @Nested
    inner class LeftPopTest {
        @Test
        fun `LPOP 은 첫번째 요소를 제거 및 반환한다`() {
            val cache = Cache()
            val key = "list_key"
            cache.rightPush(key, listOf("one", "two", "three", "four", "five"))
            val element = cache.leftPop(key)
            assertTrue { element == "one" }
            assertTrue { cache.leftRange(key, 0, -1) == listOf("two", "three", "four", "five") }
        }
    }
}
