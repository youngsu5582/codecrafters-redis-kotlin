class Cache {
    private val cache = hashMapOf<String, String>()

    fun put(key: String, value: String) {
        cache[key] = value
    }

    fun get(key: String): String? {
        return cache[key]
    }
}