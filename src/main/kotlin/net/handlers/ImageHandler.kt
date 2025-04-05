package net.handlers

import ui.Chat
import javax.swing.ImageIcon

class ImageHandler {
    companion object {
        fun handleImagePacket(packetData: ByteArray) {
            val packetType = packetData[0]
            val imageData = packetData.copyOfRange(1, packetData.size)
            println("Received image packet (type: $packetType, size: ${imageData.size} bytes)")

            when (packetType) {
                0.toByte() -> handleGeneralImage(imageData)
                1.toByte() -> handlePrivateImage(imageData)
                else -> println("Unknown image packet type: $packetType")
            }
        }

        private fun handlePrivateImage(imageData: ByteArray) {
            val sender = extractSender(imageData)
            val imageBytes = imageData.copyOfRange(30, imageData.size)
            val image = ImageIcon(imageBytes)
            Chat.handleImage(image, sender, true)
        }

        private fun handleGeneralImage(imageData: ByteArray) {
            val sender = extractSender(imageData)
            val imageBytes = imageData.copyOfRange(30, imageData.size)
            val image = ImageIcon(imageBytes)
            Chat.handleImage(image, sender, false)
        }

        private fun extractSender(imageData: ByteArray): String {
            // Find the position of the first null byte or take all 30 bytes
            val nullTerminatorPos = imageData.take(30).indexOfFirst { it == 0.toByte() }
            val senderLength = if (nullTerminatorPos >= 0) nullTerminatorPos else 30

            // Extract just the relevant bytes for the username
            return String(imageData, 0, senderLength).trim()
        }
    }
}