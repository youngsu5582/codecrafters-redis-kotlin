enum class TimeOption(val unit: Int) {
    /**
     * seconds
     */
    EX(1000),

    /**
     * milliseconds
     */
    PX(1);

    fun toMills(value: Long) = value * unit

    companion object {
        fun from(value: String) = entries.first { it.name == value }
    }
}