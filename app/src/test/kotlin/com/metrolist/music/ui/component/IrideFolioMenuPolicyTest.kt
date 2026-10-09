package com.metrolist.music.ui.component

import com.metrolist.music.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.BeforeClass
import org.junit.Test

class IrideFolioMenuPolicyTest {
    companion object {
        @JvmStatic
        @BeforeClass
        fun initializeCompileOnlyResources() {
            // AGP's compile-only R jar has zero-valued, non-final fields. Give those stubs
            // distinct IDs in JVM tests; real Android/Robolectric resource IDs stay untouched.
            val fields = R.string::class.java.fields.filter { it.type == Integer.TYPE }
            if (fields.all { it.getInt(null) == 0 }) {
                fields.forEachIndexed { index, field -> field.setInt(null, index + 1) }
            }
        }
    }

    private data class Entry(val id: Int?, val section: Int? = null, val label: String = "")

    private fun ordered(vararg entries: Entry) =
        folioContextItems(entries.toList(), { it.id }, { it.section })

    @Test
    fun collectionMenusNeverOfferRadioOrDuplicatePlaybackControls() {
        val entries =
            ordered(
                Entry(R.string.play),
                Entry(R.string.shuffle),
                Entry(R.string.radio),
                Entry(R.string.start_radio),
                Entry(R.string.play_next),
                Entry(R.string.add_to_queue),
            )
        assertEquals(listOf(R.string.play_next, R.string.add_to_queue), entries.map { it.id })
    }

    @Test
    fun duplicateActionsKeepTheirFirstCallbackEntry() {
        val entries =
            ordered(
                Entry(R.string.add_to_playlist, label = "first"),
                Entry(R.string.add_to_playlist, label = "duplicate"),
            )
        assertEquals(listOf("first"), entries.map { it.label })
    }

    @Test
    fun navigationAndSharingPrecedeManagementAndDeletion() {
        val entries =
            ordered(
                Entry(R.string.delete),
                Entry(R.string.edit),
                Entry(R.string.share),
                Entry(R.string.view_artist),
                Entry(R.string.action_download),
                Entry(R.string.add_to_queue),
            )
        assertEquals(
            listOf(
                R.string.add_to_queue,
                R.string.action_download,
                R.string.view_artist,
                R.string.share,
                R.string.edit,
                R.string.delete,
            ),
            entries.map { it.id },
        )
    }

    @Test
    fun downloadStateVariantsStayInTheSameSection() {
        val variants =
            listOf(R.string.action_download, R.string.downloading, R.string.remove_download)
        assertEquals(1, variants.map { folioActionOrder(it) }.distinct().size)
    }

    @Test
    fun choiceMenusPreserveTheirOriginalSectionAndOptionOrder() {
        val entries =
            listOf(Entry(null, 1, "third"), Entry(null, 0, "first"), Entry(null, 0, "second"))
        assertEquals(
            listOf("first", "second", "third"),
            ordered(*entries.toTypedArray()).map { it.label },
        )
    }

    @Test
    fun differentAnonymousChoicesAreNotDeduplicated() {
        assertEquals(2, ordered(Entry(null, label = "HTTP"), Entry(null, label = "SOCKS")).size)
    }

    @Test
    fun removingUnavailableActionsDoesNotLeaveEmptyGroups() {
        val entries = ordered(Entry(R.string.radio), Entry(R.string.share))
        assertFalse(entries.any { folioActionOrder(it.id).first == 0 })
        assertEquals(listOf(R.string.share), entries.map { it.id })
    }
}
