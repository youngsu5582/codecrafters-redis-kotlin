package clock

class SystemClock : Clock {
    override fun putTime(): Long {
        return getTime()
    }

    override fun expiredTime(): Long {
        return getTime()
    }

    fun getTime(): Long {
        return System.currentTimeMillis()
    }
}