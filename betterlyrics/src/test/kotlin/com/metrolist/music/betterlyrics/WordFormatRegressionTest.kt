package com.metrolist.music.betterlyrics

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the word-by-word contract end to end (parser side): TTML word spans must
 * come out of [TTMLParser.toLRC] as inline <MM:SS.cc> tags, which is exactly what
 * LyricsUtils.detectTier/parseLyrics recognize as SYNCED_WORD. If this breaks,
 * karaoke silently degrades to line-by-line.
 */
class WordFormatRegressionTest {
    private val ttml = """
        <tt xmlns="http://www.w3.org/ns/ttml">
          <body>
            <div>
              <p begin="4.284" end="7.478"><span begin="4.284" end="4.392">Yeah,</span> <span begin="4.392" end="4.618">they</span> <span begin="4.618" end="4.900">wishin'</span></p>
              <p begin="1:00.402" end="1:01.179"><span begin="1:00.402" end="1:00.563">And</span> <span begin="1:00.563" end="1:01.179">still</span></p>
            </div>
          </body>
        </tt>
    """.trimIndent()

    @Test
    fun ttmlWordSpans_becomeInlineWordTags() {
        val parsed = TTMLParser.parseTTML(ttml)
        assertTrue("expected 2 parsed lines", parsed.size == 2)
        assertTrue("first line must carry word timings", parsed[0].words.isNotEmpty())
        val lrc = TTMLParser.toLRC(parsed)
        assertTrue("word tag <00:04.28> missing in:\n$lrc", lrc.contains("<00:04.28>Yeah,"))
        assertTrue("minute-spanning tag <01:00.40> missing in:\n$lrc", lrc.contains("<01:00.40>And"))
    }
}
