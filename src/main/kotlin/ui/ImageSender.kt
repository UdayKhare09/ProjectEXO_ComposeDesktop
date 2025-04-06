package ui

import net.ClientSocket
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileFilter
import kotlin.math.min

class ImageSender {
    companion object {
        private fun getJFileChooser(): JFileChooser {
            val fileChooser = JFileChooser()
            fileChooser.fileFilter = object : FileFilter() {
                override fun accept(f: File): Boolean {
                    return f.isDirectory || f.name.lowercase(Locale.getDefault()).endsWith(".jpg")
                            || f.name.lowercase(Locale.getDefault()).endsWith(".jpeg")
                            || f.name.lowercase(Locale.getDefault()).endsWith(".png")
                            || f.name.lowercase(Locale.getDefault()).endsWith(".gif")
                }

                override fun getDescription(): String {
                    return "Image Files"
                }
            }
            return fileChooser
        }

        fun sendImage(recipient: String) {
            val worker: SwingWorker<Void?, Void?> = object : SwingWorker<Void?, Void?>() {
                @Throws(java.lang.Exception::class)
                override fun doInBackground(): Void? {
                    val fileChooser: JFileChooser = getJFileChooser()

                    val returnValue = fileChooser.showOpenDialog(null)
                    if (returnValue != JFileChooser.APPROVE_OPTION) {
                        return null
                    }

                    val selectedFile = fileChooser.selectedFile
                    try {
                        // Read and scale the image
                        val originalIcon = ImageIcon(selectedFile.path)
                        val originalImage = originalIcon.image

                        // Scale image to fit 800x600 while maintaining aspect ratio
                        val originalWidth = originalImage.getWidth(null)
                        val originalHeight = originalImage.getHeight(null)

                        val scaleX = 800.0 / originalWidth
                        val scaleY = 600.0 / originalHeight
                        val scale = min(scaleX, scaleY)

                        val scaledWidth = (originalWidth * scale).toInt()
                        val scaledHeight = (originalHeight * scale).toInt()

                        // Create scaled image
                        val scaledImage = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB)
                        val g2 = scaledImage.createGraphics()
                        g2.setRenderingHint(
                            RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR
                        )
                        g2.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null)
                        g2.dispose()

                        // Convert to byte array
                        val baos = ByteArrayOutputStream()
                        ImageIO.write(scaledImage, "png", baos)
                        val fileContent = baos.toByteArray()

                        // Create ImageIcon for local display
                        val sentImageIcon = ImageIcon(scaledImage)

                        // Update UI with sent image
                        SwingUtilities.invokeLater {
                            // Add to local messages
                            val chatKey = recipient
                            val currentMessages = Chat.messages.getOrDefault(chatKey, emptyList())
                            val newMessage = Chat.ChatMessage(
                                sender = "Me",
                                content = "[Image]",
                                isPrivate = chatKey != "general",
                                isOutgoing = true,
                                image = sentImageIcon
                            )
                            Chat.messages[chatKey] = currentMessages + newMessage
                        }

                        if ("general" == recipient) {
                            println("Sending general image")
                            val packetData = ByteArray(fileContent.size + 2)
                            packetData[0] = 3 // PacketType type
                            packetData[1] = 0 // General message type
                            System.arraycopy(fileContent, 0, packetData, 2, fileContent.size)
                            ClientSocket.sendPacket(packetData)
                        } else {
                            println("Sending private image to $recipient")
                            val packetData = ByteArray(fileContent.size + 32)
                            packetData[0] = 3 // PacketType type
                            packetData[1] = 1 // Private message type
                            val usernameBytes: ByteArray = recipient.toByteArray()
                            System.arraycopy(usernameBytes, 0, packetData, 2, usernameBytes.size)
                            for (i in usernameBytes.size + 2..31) {
                                packetData[i] = 0
                            }
                            System.arraycopy(fileContent, 0, packetData, 32, fileContent.size)
                            ClientSocket.sendPacket(packetData)
                        }
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                        SwingUtilities.invokeLater {
                            JOptionPane.showMessageDialog(
                                null,
                                "Error processing image: " + e.message,
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                            )
                        }
                    }

                    return null
                }
            }

            worker.execute()
        }
    }
}