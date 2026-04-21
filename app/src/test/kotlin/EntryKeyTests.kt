import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EntryKeyTests {

    @Test
    fun `EntryKey 는 0-0 를 넣을순 없다`() {
        assertThrows<CustomException> {
            EntryKey(0, 0)
        }
    }

    @Test
    fun `EntryKey 는 timestamp 를 먼저 비교한다`() {
        assertTrue { EntryKey(3, 0) > EntryKey(2, 0) }
    }


    @Test
    fun `EntryKey 는 timestamp 가 동일하면, sequenceNumber 를 비교한다`() {
        assertTrue { EntryKey(2, 1) > EntryKey(2, 0) }
    }

}