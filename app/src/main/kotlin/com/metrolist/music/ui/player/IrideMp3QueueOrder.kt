package com.metrolist.music.ui.player

/** Remembers manual order independently of temporary mood priorities and timeline indices. */
internal class IrideMp3QueueOrder<K> {
    private val baseline = mutableListOf<K>()

    fun setManualOrder(keys: List<K>) {
        baseline.clear()
        baseline.addAll(keys)
    }

    /** Returns true when a different queue replaced all surviving entries. */
    fun synchronize(keys: List<K>): Boolean {
        val live = keys.toSet()
        val replaced = baseline.isNotEmpty() && keys.isNotEmpty() && baseline.none { it in live }
        baseline.retainAll(live)
        val known = baseline.toHashSet()
        baseline.addAll(keys.filter { known.add(it) })
        return replaced
    }

    fun ordered(keys: List<K>, preferred: Set<K>? = null): List<K> {
        val live = keys.toSet()
        val canonical = baseline.filter { it in live }
        return if (preferred == null) canonical
        else canonical.filter { it in preferred } + canonical.filterNot { it in preferred }
    }

    fun move(from: K, to: K) {
        val source = baseline.indexOf(from)
        val target = baseline.indexOf(to)
        if (source >= 0 && target >= 0 && source != target)
            baseline.add(target, baseline.removeAt(source))
    }
}
