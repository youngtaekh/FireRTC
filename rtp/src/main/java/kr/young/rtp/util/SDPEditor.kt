package kr.young.rtp.util

import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList

class SDPEditor {
    fun addIceCandidate(sdp: String, candidates: ArrayList<String>): String {
        var find = false
        val builder = StringBuilder()
        val lines: Array<String> = sdp.split(NEW_LINE.toRegex()).toTypedArray()
        for (line in lines) {
            if (line.isEmpty()) { continue }
            if (line.startsWith(PREFIX, true)) {
                find = true
                builder.append(line).append(NEW_LINE)
                continue
            }
            if (find && !line.startsWith(PREFIX, true)) {
                find = false
                for (candidate in candidates) {
                    builder.append("a=").append(candidate).append(NEW_LINE)
                }
            }
            builder.append(line).append(NEW_LINE)
        }
        return builder.toString()
    }

    fun addIceCandidates(sdp: String, candidates: String): String {
        var find = false
        val builder = StringBuilder()
        val lines: Array<String> = sdp.split(NEW_LINE.toRegex()).toTypedArray()

        var i = 0
        for (line in lines) {
            i++
            if (line.startsWith(PREFIX, true)) {
                find = true
                builder.append(line).append(NEW_LINE)
                continue
            }
            if (find && !line.startsWith(PREFIX, true)) {
                find = false
                val tokenizer = StringTokenizer(candidates, ";")
                while (tokenizer.hasMoreTokens()) {
                    builder.append("a=").append(tokenizer.nextToken()).append(NEW_LINE)
                }
            }
            if (i == lines.size) {
                builder.append(line)
            } else {
                builder.append(line).append(NEW_LINE)
            }
        }
        return builder.toString()
    }

    companion object {
        private const val PREFIX = "a=ice"
        private const val NEW_LINE = "\r\n"
        private const val TAG = "SDPEditor"
    }
}