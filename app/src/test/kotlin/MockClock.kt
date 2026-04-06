import clock.Clock

class MockClock(
    private val putTimeQueue: ArrayDeque<Long> = ArrayDeque(),
    private val expiredTimeQueue: ArrayDeque<Long> = ArrayDeque()
) : Clock {


    override fun putTime(): Long {
        return putTimeQueue.removeFirst()
    }

    override fun expiredTime(): Long {
        return expiredTimeQueue.removeFirst()
    }
}