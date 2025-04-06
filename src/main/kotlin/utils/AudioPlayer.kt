package utils

import ui.Chat
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

class AudioPlayer {
    companion object{
        fun playEffect(path: String) {
            Thread {
                try {
                    // Load the sound file from resources
                    val soundStream = Chat::class.java.getResourceAsStream(path)
                    if (soundStream != null) {
                        // Convert to byte array first to avoid mark/reset issues
                        val soundBytes = soundStream.readBytes()
                        soundStream.close()

                        val audioStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(soundBytes))
                        val clip = AudioSystem.getClip()
                        clip.open(audioStream)
                        clip.start()
                    } else {
                        // If sound file doesn't exist yet, use a generated tone
                        val clip = AudioSystem.getClip()
                        val audioFormat = AudioFormat(
                            44100f,  // Sample rate
                            16,      // Sample size in bits
                            1,       // Channels (mono)
                            true,    // Signed
                            false    // Big-endian
                        )

                        // Generate a simple "ping" sound
                        val duration = 0.1  // seconds
                        val sampleRate = 44100.0f
                        val numSamples = (duration * sampleRate).toInt()
                        val buffer = ByteArray(numSamples * 2)  // 16-bit = 2 bytes per sample
                        val frequency = 1000.0  // Hz

                        for (i in 0 until numSamples) {
                            val angle = i.toDouble() / sampleRate * frequency * 2.0 * Math.PI
                            val amplitude =
                                if (i < numSamples / 2) 32767.0 else 32767.0 * (1 - (i - numSamples / 2.0) / (numSamples / 2.0))
                            val sample = (amplitude * Math.sin(angle)).toInt().toShort()
                            buffer[i * 2] = (sample.toInt() and 0xff).toByte()
                            buffer[i * 2 + 1] = (sample.toInt() shr 8 and 0xff).toByte()
                        }

                        val audioStream = AudioInputStream(
                            ByteArrayInputStream(buffer),
                            audioFormat,
                            buffer.size.toLong() / audioFormat.frameSize
                        )

                        clip.open(audioStream)
                        clip.start()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fail silently - sound is not critical functionality
                }
            }.start()
        }
    }
}
