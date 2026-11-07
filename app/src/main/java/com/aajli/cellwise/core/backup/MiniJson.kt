/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.core.backup

/**
 * Small dependency-free JSON reader/writer (pure Kotlin, unit-testable on the JVM).
 * Values: Map<String, Any?>, List<Any?>, String, Long, Double, Boolean, null.
 */
object MiniJson {
    class JsonException(message: String) : Exception(message)

    fun parse(text: String): Any? {
        val p = Parser(text)
        return try {
            val v = p.value(0)
            p.ws()
            if (p.i != text.length) throw JsonException("trailing data at ${p.i}")
            v
        } catch (e: NumberFormatException) {
            throw JsonException("bad number or escape")
        }
    }

    fun write(value: Any?, pretty: Boolean = true): String = StringBuilder().also { put(value, it, pretty, 0) }.toString()

    private class Parser(val s: String) {
        var i = 0
        fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
        fun fail(m: String): Nothing = throw JsonException("$m at $i")

        fun value(depth: Int): Any? {
            if (depth > 64) fail("too deep")
            ws()
            if (i >= s.length) fail("unexpected end")
            val c = s[i]
            return when {
                c == '{' -> obj(depth)
                c == '[' -> arr(depth)
                c == '"' -> str()
                s.startsWith("true", i) -> { i += 4; true }
                s.startsWith("false", i) -> { i += 5; false }
                s.startsWith("null", i) -> { i += 4; null }
                c == '-' || c.isDigit() -> num()
                else -> fail("unexpected '$c'")
            }
        }

        fun obj(depth: Int): Map<String, Any?> {
            i++
            val m = LinkedHashMap<String, Any?>()
            ws()
            if (i < s.length && s[i] == '}') { i++; return m }
            while (true) {
                ws()
                if (i >= s.length || s[i] != '"') fail("expected key")
                val k = str()
                ws()
                if (i >= s.length || s[i] != ':') fail("expected ':'")
                i++
                m[k] = value(depth + 1)
                ws()
                if (i >= s.length) fail("unexpected end")
                when (s[i++]) { ',' -> {} ; '}' -> return m; else -> fail("expected ',' or '}'") }
            }
        }

        fun arr(depth: Int): List<Any?> {
            i++
            val l = ArrayList<Any?>()
            ws()
            if (i < s.length && s[i] == ']') { i++; return l }
            while (true) {
                l.add(value(depth + 1))
                ws()
                if (i >= s.length) fail("unexpected end")
                when (s[i++]) { ',' -> {} ; ']' -> return l; else -> fail("expected ',' or ']'") }
            }
        }

        fun str(): String {
            i++
            val sb = StringBuilder()
            while (true) {
                if (i >= s.length) fail("unterminated string")
                val c = s[i++]
                when {
                    c == '"' -> return sb.toString()
                    c == '\\' -> {
                        if (i >= s.length) fail("bad escape")
                        when (val e = s[i++]) {
                            '"' -> sb.append('"'); '\\' -> sb.append('\\'); '/' -> sb.append('/')
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000C'); 'n' -> sb.append('\n')
                            'r' -> sb.append('\r'); 't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 > s.length) fail("bad unicode escape")
                                sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4
                            }
                            else -> fail("bad escape '$e'")
                        }
                    }
                    c < ' ' -> fail("control character in string")
                    else -> sb.append(c)
                }
            }
        }

        fun num(): Any {
            val st = i
            if (s[i] == '-') i++
            while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == 'e' || s[i] == 'E' || s[i] == '+' || s[i] == '-')) i++
            val t = s.substring(st, i)
            return t.toLongOrNull() ?: t.toDoubleOrNull() ?: fail("bad number '$t'")
        }
    }

    private fun nl(sb: StringBuilder, pretty: Boolean, level: Int) {
        if (pretty) { sb.append('\n'); repeat(level) { sb.append("  ") } }
    }

    private fun put(v: Any?, sb: StringBuilder, pretty: Boolean, level: Int) {
        when (v) {
            null -> sb.append("null")
            is Boolean -> sb.append(v.toString())
            is Double -> sb.append(if (v.isFinite()) v.toString() else "null")
            is Number -> sb.append(v.toString())
            is String -> quote(v, sb)
            is Map<*, *> -> {
                if (v.isEmpty()) { sb.append("{}"); return }
                sb.append('{')
                var first = true
                for ((k, x) in v) {
                    if (!first) sb.append(',')
                    first = false
                    nl(sb, pretty, level + 1)
                    quote(k.toString(), sb)
                    sb.append(if (pretty) ": " else ":")
                    put(x, sb, pretty, level + 1)
                }
                nl(sb, pretty, level)
                sb.append('}')
            }
            is List<*> -> {
                if (v.isEmpty()) { sb.append("[]"); return }
                sb.append('[')
                var first = true
                for (x in v) {
                    if (!first) sb.append(',')
                    first = false
                    nl(sb, pretty, level + 1)
                    put(x, sb, pretty, level + 1)
                }
                nl(sb, pretty, level)
                sb.append(']')
            }
            else -> throw IllegalArgumentException("unsupported JSON type ${v::class}")
        }
    }

    private fun quote(s: String, sb: StringBuilder) {
        sb.append('"')
        for (c in s) when {
            c == '"' -> sb.append("\\\"")
            c == '\\' -> sb.append("\\\\")
            c == '\n' -> sb.append("\\n")
            c == '\r' -> sb.append("\\r")
            c == '\t' -> sb.append("\\t")
            c < ' ' -> sb.append("\\u%04x".format(c.code))
            else -> sb.append(c)
        }
        sb.append('"')
    }
}
