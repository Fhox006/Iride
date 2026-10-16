package com.metrolist.music.ui.component

/** A tile and its detail page share one source while either remains visible. */
internal class ArtistArtworkPresentation(private val now: () -> Long = System::nanoTime) {
    private data class Entry(val url: String, var users: Int, var expiresAt: Long)
    private val entries = LinkedHashMap<String, Entry>(128, 0.75f, true)
    private val retention = 10_000_000_000L

    /** Promote a provisional fallback to the persisted choice, keeping visible surfaces in sync. */
    @Synchronized
    fun select(id: String, url: String) {
        if (entries[id]?.url == url) return
        entries[id] = Entry(url, 0, now() + retention)
    }

    @Synchronized
    fun current(id: String): String? {
        val entry = entries[id] ?: return null
        if (entry.users == 0 && entry.expiresAt <= now()) {
            entries.remove(id)
            return null
        }
        return entry.url
    }

    @Synchronized
    fun choose(id: String, suggested: String?): String? {
        current(id)?.let { return it }
        if (suggested == null) return null
        entries[id] = Entry(suggested, 0, now() + retention)
        if (entries.size > 128) entries.entries.firstOrNull { it.value.users == 0 }?.let { entries.remove(it.key) }
        return suggested
    }

    @Synchronized
    fun retain(id: String, url: String) {
        val entry = entries[id]
        if (entry?.url == url) entry.users++
        else if (entry == null || entry.users == 0) entries[id] = Entry(url, 1, now() + retention)
    }

    @Synchronized
    fun release(id: String, url: String) {
        entries[id]?.takeIf { it.url == url }?.let {
            it.users = (it.users - 1).coerceAtLeast(0)
            it.expiresAt = now() + retention
        }
    }
}
