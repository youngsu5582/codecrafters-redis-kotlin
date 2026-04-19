data class StreamEntry(
    val value: Map<String, String> = mapOf()
)

data class StreamEntries(
    val value: MutableMap<String, StreamEntry> = mutableMapOf<String, StreamEntry>()
)