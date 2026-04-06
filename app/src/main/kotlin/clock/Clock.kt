package clock

interface Clock {
    /**
     * 데이터를 캐시에 PUT 할때, 처리할 시간을 반환하는 함수
     */
    fun putTime(): Long

    /**
     * 데이터를 캐시에서 Eviction 할때, 처리할 시간을 반환하는 함수
     */
    fun expiredTime(): Long
}