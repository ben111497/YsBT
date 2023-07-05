package com.ys.bt

import java.nio.charset.StandardCharsets

class Binary (private var data: ByteArray) {
    fun toUTF(): String = String(data, StandardCharsets.UTF_8)
    fun `val`(): ByteArray = data
    fun toHEX(): String = bytesToHex(data)

    private fun bytesToHex(data: ByteArray): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("0x")
        for (byte in data) {
            val hex = String.format("%02X", byte.toInt() and 0xFF)
            stringBuilder.append(hex)
        }
        return stringBuilder.toString()
    }
}