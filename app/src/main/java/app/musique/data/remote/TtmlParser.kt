package app.musique.data.remote

import app.musique.core.model.LyricLine
import app.musique.core.model.LyricRole
import app.musique.core.model.LyricWord

/**
 * Apple Music style TTML, the format the AMLL database and BetterLyrics both serve. Ported from the
 * desktop app's tolerant scanner: per word timings, backing vocals (`ttm:role="x-bg"`), duet voices
 * (`ttm:agent`), translations and romanisations. Everything outside `<p>` is ignored.
 */
object TtmlParser {

    fun parse(raw: String): List<LyricLine> {
        val groups = ArrayList<List<LyricLine>>()
        val agentRoles = HashMap<String, LyricRole>()
        var i = 0
        while (true) {
            val open = raw.indexOf("<p", i).takeIf { it >= 0 } ?: break
            val next = raw.getOrNull(open + 2) ?: break
            if (!next.isWhitespace() && next != '>') {
                i = open + 2
                continue
            }
            val gt = raw.indexOf('>', open).takeIf { it >= 0 } ?: break
            val close = raw.indexOf("</p>", gt).takeIf { it >= 0 } ?: break
            val tag = raw.substring(open + 1, gt)
            val inner = raw.substring(gt + 1, close)
            i = close + 4

            val parsed = scan(inner)
            val begin = attr(tag, "begin")?.let(::parseTime)
            val end = attr(tag, "end")?.let(::parseTime)
            val agent = attr(tag, "agent")?.takeIf { it.isNotBlank() }
            val role = agent?.let { a ->
                agentRoles.getOrPut(a) {
                    // the first voice and group voices ("v1000") sit on the left; any other is a duet
                    if (agentRoles.isEmpty() || a.endsWith("000")) LyricRole.MAIN else LyricRole.DUET
                }
            }

            val main = when {
                parsed.words.isNotEmpty() -> worded(begin ?: parsed.words.first().startMs, parsed.words, end)
                begin != null -> {
                    val text = parsed.text.trim().toString()
                    if (text.isEmpty() && parsed.bg == null) continue
                    LyricLine(timestampMs = begin, text = text, endMs = end?.takeIf { it > begin })
                }
                else -> continue
            }.copy(role = role, agent = agent, translation = parsed.translation, roman = parsed.roman)

            groups += listOfNotNull(main, parsed.bg?.copy(agent = agent))
        }
        // sort lead lines only, so each backing line stays right after the line it belongs to
        return groups.sortedBy { it.first().timestampMs }.flatten()
    }

    /** Untimed documents still carry Apple's text. */
    fun parsePlain(raw: String): List<String> {
        val lines = ArrayList<String>()
        var i = 0
        while (true) {
            val open = raw.indexOf("<p", i).takeIf { it >= 0 } ?: break
            val gt = raw.indexOf('>', open).takeIf { it >= 0 } ?: break
            val close = raw.indexOf("</p>", gt).takeIf { it >= 0 } ?: break
            i = close + 4
            val parsed = scan(raw.substring(gt + 1, close))
            val text = (parsed.words.joinToString("") { it.text } + parsed.text).trim()
            if (text.isNotEmpty()) lines += text
        }
        return lines
    }

    private fun worded(start: Long, rawWords: List<LyricWord>, end: Long?): LyricLine {
        // the exporter's indentation after the last span is not part of the line
        val words = rawWords.toMutableList().also { it[it.lastIndex] = it.last().copy(text = it.last().text.trimEnd()) }
        val text = words.joinToString("") { it.text }.trim()
        val lastEnd = words.maxOf { it.endMs }
        return LyricLine(
            timestampMs = start,
            text = text,
            endMs = (end ?: lastEnd).takeIf { it > start },
            words = words,
        )
    }

    private class Parsed {
        val words = ArrayList<LyricWord>()
        val text = StringBuilder()
        var translation: String? = null
        var roman: String? = null
        var bg: LyricLine? = null
    }

    private fun scan(inner: String): Parsed {
        val p = Parsed()
        val pending = StringBuilder()
        var i = 0
        while (i < inner.length) {
            val lt = inner.indexOf('<', i)
            if (lt < 0) {
                pending.append(inner, i, inner.length)
                break
            }
            pending.append(inner, i, lt)
            val gt = inner.indexOf('>', lt).takeIf { it >= 0 } ?: break
            val tag = inner.substring(lt + 1, gt)
            i = gt + 1

            val bare = tag.trimStart('/')
            val name = bare.substring(0, bare.indexOfFirst { it.isWhitespace() || it == '/' }.let { if (it < 0) bare.length else it })
            if (name == "br") {
                pending.append(' ')
                continue
            }
            if (name != "span" || tag.startsWith('/') || tag.endsWith('/')) continue

            val (contentEnd, after) = spanContent(inner, i)
            val content = inner.substring(i, contentEnd)
            i = after

            when (attr(tag, "role")) {
                "x-bg" -> if (p.bg == null) p.bg = bgLine(tag, content)
                "x-translation" -> p.translation = plainText(content).ifBlank { null }
                "x-roman" -> p.roman = plainText(content).ifBlank { null }
                else -> {
                    val start = attr(tag, "begin")?.let(::parseTime)
                    if (start != null) {
                        val text = plainText(content)
                        if (text.isEmpty()) continue
                        val end = attr(tag, "end")?.let(::parseTime) ?: start
                        flush(p, pending)
                        p.words += LyricWord(startMs = start, endMs = maxOf(end, start), text = text)
                    } else {
                        val nested = scan(content)
                        flush(p, pending)
                        p.words += nested.words
                        p.text.append(nested.text)
                        p.translation = p.translation ?: nested.translation
                        p.roman = p.roman ?: nested.roman
                    }
                }
            }
        }
        flush(p, pending)
        return p
    }

    /** Whitespace between two word spans belongs to the word before it ("I " not " I"). */
    private fun flush(p: Parsed, pending: StringBuilder) {
        val text = collapseWs(unescape(pending.toString()))
        pending.setLength(0)
        if (text.isEmpty()) return
        val last = p.words.lastOrNull()
        if (last != null) p.words[p.words.lastIndex] = last.copy(text = last.text + text) else p.text.append(text)
    }

    private fun collapseWs(s: String): String {
        val out = StringBuilder(s.length)
        var inWs = false
        for (c in s) {
            if (c.isWhitespace()) {
                inWs = true
                continue
            }
            if (inWs && out.isNotEmpty()) out.append(' ')
            inWs = false
            out.append(c)
        }
        if (inWs && out.isNotEmpty()) out.append(' ')
        if (out.isEmpty() && s.isNotEmpty()) out.append(' ')
        return out.toString()
    }

    private fun bgLine(tag: String, content: String): LyricLine? {
        val parsed = scan(content)
        val begin = attr(tag, "begin")?.let(::parseTime)
        val end = attr(tag, "end")?.let(::parseTime)
        val line = if (parsed.words.isEmpty()) {
            val text = parsed.text.trim().toString().ifEmpty { return null }
            LyricLine(timestampMs = begin ?: return null, text = text, endMs = end)
        } else {
            worded(begin ?: parsed.words.first().startMs, parsed.words, end)
        }
        return unwrapParens(
            line.copy(role = LyricRole.BG, isBackground = true, translation = parsed.translation, roman = parsed.roman)
        )
    }

    /** Backing vocals come wrapped in parens by convention; the UI already styles them. */
    private fun unwrapParens(line: LyricLine): LyricLine {
        val open = charArrayOf('(', '（')
        val close = charArrayOf(')', '）')
        val text = line.text.trim()
        if (!(text.isNotEmpty() && text.first() in open && text.last() in close && text.length > 2)) return line
        val words = line.words
        if (words.isNullOrEmpty()) return line.copy(text = text.trimStart(*open).trimEnd(*close).trim())
        val fixed = words.toMutableList()
        fixed[0] = fixed[0].copy(text = fixed[0].text.trimStart().trimStart(*open))
        fixed[fixed.lastIndex] = fixed.last().copy(text = fixed.last().text.trimEnd().trimEnd(*close))
        val kept = fixed.filter { it.text.isNotEmpty() }
        return line.copy(words = kept, text = kept.joinToString("") { it.text }.trim())
    }

    private fun spanContent(s: String, from: Int): Pair<Int, Int> {
        var depth = 1
        var i = from
        while (true) {
            val close = s.indexOf("</span>", i)
            if (close < 0) return s.length to s.length
            val open = s.indexOf("<span", i).takeIf { it in 0 until close }
            if (open != null) {
                val gt = s.indexOf('>', open).takeIf { it >= 0 } ?: close
                if (!s.substring(open, gt).endsWith('/')) depth++
                i = gt + 1
            } else {
                depth--
                if (depth == 0) return close to close + 7
                i = close + 7
            }
        }
    }

    private fun plainText(s: String): String {
        val out = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val lt = s.indexOf('<', i)
            if (lt < 0) {
                out.append(s, i, s.length)
                break
            }
            out.append(s, i, lt)
            val gt = s.indexOf('>', lt).takeIf { it >= 0 } ?: break
            i = gt + 1
        }
        return unescape(out.toString())
    }

    /** Attribute value with or without a namespace prefix (`ttm:role` and `role`). */
    internal fun attr(tag: String, name: String): String? {
        val pat = "$name=\""
        var from = 0
        while (true) {
            val at = tag.indexOf(pat, from).takeIf { it >= 0 } ?: return null
            val startsAttr = at == 0 || tag[at - 1].let { it.isWhitespace() || it == ':' }
            val value = at + pat.length
            val end = tag.indexOf('"', value).takeIf { it >= 0 } ?: return null
            if (startsAttr) return unescape(tag.substring(value, end))
            from = end + 1
        }
    }

    /** `HH:MM:SS.mmm`, `MM:SS.mmm`, `SS.mmm`, and offsets like `12.34s` / `250ms`. */
    internal fun parseTime(raw: String): Long? {
        val t = raw.trim()
        if (t.isEmpty()) return null
        if (':' !in t) {
            val num = t.trimEnd { it.isLetter() }
            val unit = t.substring(num.length)
            val v = num.toDoubleOrNull() ?: return null
            val ms = when (unit) {
                "h" -> v * 3_600_000
                "m" -> v * 60_000
                "ms" -> v
                "f", "t" -> return null
                else -> v * 1000
            }
            return Math.round(ms)
        }
        val parts = t.split(':')
        if (parts.size > 3) return null
        val last = parts.last()
        val ss = last.substringBefore('.')
        val frac = if ('.' in last) last.substringAfter('.') else ""
        var ms = (ss.trim().toLongOrNull() ?: return null) * 1000 + fracMs(frac)
        ms += (parts[parts.size - 2].trim().toLongOrNull() ?: 0L) * 60_000
        if (parts.size == 3) ms += (parts[0].trim().toLongOrNull() ?: return null) * 3_600_000
        return ms
    }

    private fun fracMs(frac: String): Long {
        val digits = frac.filter { it.isDigit() }.take(3)
        val v = digits.toLongOrNull() ?: 0L
        return when (digits.length) {
            0 -> 0
            1 -> v * 100
            2 -> v * 10
            else -> v
        }
    }

    private fun unescape(s: String): String {
        if ('&' !in s) return s
        return Regex("&(#x[0-9a-fA-F]+|#[0-9]+|amp|lt|gt|quot|apos);").replace(s) { m ->
            when (val e = m.groupValues[1]) {
                "amp" -> "&"
                "lt" -> "<"
                "gt" -> ">"
                "quot" -> "\""
                "apos" -> "'"
                else -> {
                    val code = if (e.startsWith("#x")) e.drop(2).toIntOrNull(16) else e.drop(1).toIntOrNull()
                    code?.let { String(Character.toChars(it)) } ?: m.value
                }
            }
        }
    }
}
