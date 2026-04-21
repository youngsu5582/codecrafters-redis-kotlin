import ErrorCode.MUST_BE_GT_ZERO

data class EntryKey(
    val timestamp: Long,
    val sequenceNumber: Long
) : Comparable<EntryKey> {

    init {
        if (timestamp == 0L && sequenceNumber == 0L) {
            throw CustomException(MUST_BE_GT_ZERO)
        }

        if (timestamp < 0 || sequenceNumber < 0) {
            throw CustomException(MUST_BE_GT_ZERO)
        }
    }

    override fun compareTo(other: EntryKey): Int =
        compareValuesBy(this, other, { it.timestamp }, { it.sequenceNumber })

    fun toFormatString() = "$timestamp-$sequenceNumber"
}