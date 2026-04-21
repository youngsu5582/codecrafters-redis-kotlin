import java.util.TreeMap

data class StreamEntry(
    val value: Map<String, String> = mapOf()
)

data class StreamEntries(
    val value: TreeMap<EntryKey, StreamEntry> = TreeMap<EntryKey, StreamEntry>()
) {
    fun lastKey(): EntryKey {
        return value.lastKey()
    }

    fun put(key: EntryKey, streamEntry: StreamEntry) {
        value[key] = streamEntry
    }
}