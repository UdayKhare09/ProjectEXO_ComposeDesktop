package ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ClientSocket
import net.handlers.MsgHandler
import utils.AudioPlayer
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

object Chat {
    // ui.Chat message data class
    // Modify the ChatMessage data class to support images
    data class ChatMessage(
        val sender: String,
        val content: String,
        val isPrivate: Boolean,
        val isOutgoing: Boolean = false,
        val image: ImageIcon? = null
    )

    private fun sendImage(recipient: String) {
        // Start a new thread for sending the image to avoid UI freezing
        Thread {
            ImageSender.sendImage(recipient)
        }.start()
    }

    // Store messages by chat channel (user or "general")
    val messages = mutableStateMapOf<String, List<ChatMessage>>()
    val onlineUsers = mutableStateListOf<String>()

    // Currently selected chat (default is "general")
    private val currentChat = mutableStateOf("general")

    fun receiveMessage(sender: String, message: String, isPrivate: Boolean) {
        val chatKey = if (isPrivate) sender else "general"
        val currentMessages = messages.getOrDefault(chatKey, emptyList())
        messages[chatKey] = currentMessages + ChatMessage(sender, message, isPrivate, false)
        playReceiveSound()
    }

    private fun playReceiveSound() {
        AudioPlayer.playEffect("/sound/receive.wav")
    }

    fun updateOnlineUsers(users: List<String>) {
        onlineUsers.clear()
        // Add AI to the online users list
        onlineUsers.add("AI")
        onlineUsers.addAll(users)
        // Ensure general channel exists
        if (!messages.containsKey("general")) {
            messages["general"] = emptyList()
        }
    }

    @Composable
    fun ChatScreen(onLogout: () -> Unit, onBack: () -> Unit) {
        var messageInput by remember { mutableStateOf("") }
        val selectedChat = remember { currentChat }
        val chatMessages = messages[selectedChat.value] ?: emptyList()

        val listState = rememberLazyListState()

        LaunchedEffect(chatMessages.size) {
            if (chatMessages.isNotEmpty()) {
                listState.animateScrollToItem(chatMessages.size - 1)
            }
        }

        // File chooser dialog

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header remains unchanged...
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Back")
                }

                // Title
                Text(
                    text = "Secure Chat",
                    style = MaterialTheme.typography.headlineMedium
                )

                // Logout button
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Logout")
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                // Online users panel remains unchanged...
                Column(
                    modifier = Modifier.weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Chats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // General chat option
                    ChatListItem(
                        name = "general",
                        isSelected = selectedChat.value == "general",
                        onClick = { selectedChat.value = "general" }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Online Users",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    LazyColumn {
                        // Display online users except the current user
                        items(onlineUsers.filter { it != ClientSocket.username }) { user ->
                            ChatListItem(
                                name = user,
                                isSelected = selectedChat.value == user,
                                onClick = {
                                    selectedChat.value = user
                                    // Load chat history for the selected user
                                    if (!messages.containsKey(user)) {
                                        messages[user] = emptyList()
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // ui.Chat panel
                Column(
                    modifier = Modifier.weight(3f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    // ui.Chat header and messages remain unchanged...
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedChat.value == "general") "General Chat" else "Chat with ${selectedChat.value}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // ui.Chat messages
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        items(chatMessages) { message ->
                            MessageBubble(message)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Modified input field area with attachment button
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Attachment button
                        if (selectedChat.value != "AI") {
                            IconButton(
                                onClick = { sendImage(selectedChat.value) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Attach File",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Text input field
                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Send
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = {
                                    sendMsg(messageInput, selectedChat)
                                    messageInput = "" // Clear input after sending
                                }
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Send button
                        Button(
                            onClick = {
                                sendMsg(messageInput, selectedChat)
                                messageInput = "" // Clear input after sending
                            },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Send")
                        }
                    }
                }
            }
        }
    }

    private fun sendMsg(messageInput: String, selectedChat: MutableState<String>) {
        if (selectedChat.value != "AI") {
            if (messageInput.isNotBlank()) {
                MsgHandler.sendMessage(messageInput, selectedChat.value)
                playSendSound()

                // Add outgoing message to local state
                val chatKey = selectedChat.value
                val currentMessages = messages.getOrDefault(chatKey, emptyList())
                val newMessage = ChatMessage(
                    sender = "Me",
                    content = messageInput,
                    isPrivate = chatKey != "general",
                    isOutgoing = true
                )
                messages[chatKey] = currentMessages + newMessage
            }
        } else {
            sendAIMessage(messageInput)
        }
    }

    private fun sendAIMessage(messageInput: String) {
        val packetType = 9.toByte()
        val msgType = 1.toByte()

        val messageBytes = messageInput.toByteArray()
        val packet = ByteArray(2 + messageBytes.size)
        packet[0] = packetType
        packet[1] = msgType
        System.arraycopy(messageBytes, 0, packet, 2, messageBytes.size)
        ClientSocket.sendPacket(packet)
        playSendSound()

        // Add outgoing message to local state
        val chatKey = "AI"
        val currentMessages = messages.getOrDefault(chatKey, emptyList())
        val newMessage = ChatMessage(
            sender = "Me",
            content = messageInput,
            isPrivate = true,
            isOutgoing = true
        )
        messages[chatKey] = currentMessages + newMessage
    }

    // Add this function to play the sound effect
    fun playSendSound() {
        AudioPlayer.playEffect("/sound/send.wav")
    }



    @Composable
    private fun ChatListItem(name: String, isSelected: Boolean, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }

    @Composable
    private fun MessageBubble(message: ChatMessage) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (message.isOutgoing) Alignment.End else Alignment.Start
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (message.isOutgoing) 16.dp else 4.dp,
                            bottomEnd = if (message.isOutgoing) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (message.isOutgoing)
                            MaterialTheme.colorScheme.primary
                        else if (message.isPrivate)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                    .padding(12.dp)
            ) {
                if (!message.isOutgoing) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (message.isOutgoing)
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        else if (message.isPrivate)
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Display either text or image
                if (message.image != null) {
                    // Convert ImageIcon to Compose Image
                    val imageBytes = message.image.image.let {
                        val baos = java.io.ByteArrayOutputStream()
                        BufferedImage(it.getWidth(null), it.getHeight(null), BufferedImage.TYPE_INT_ARGB).apply {
                            val g2d = createGraphics()
                            g2d.drawImage(it, 0, 0, null)
                            g2d.dispose()
                            ImageIO.write(this, "png", baos)
                        }
                        baos.toByteArray()
                    }

                    Image(
                        bitmap = org.jetbrains.skia.Image.makeFromEncoded(imageBytes).toComposeImageBitmap(),
                        contentDescription = "Sent image",
                        modifier = Modifier.size(200.dp)
                            .clickable {
                                saveImage(message.image)
                            }
                    )
                } else {
                    // Check if it's an AI message and should use markdown
                    if (message.sender == "AI") {
                        MarkdownText(
                            content = message.content,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    } else {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (message.isOutgoing)
                                MaterialTheme.colorScheme.onPrimary
                            else if (message.isPrivate)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MarkdownText(content: String, color: Color) {
        val parsedMarkdown = remember(content) {
            parseMarkdownToAnnotatedString(content, color)
        }

        Text(
            text = parsedMarkdown,
            style = MaterialTheme.typography.bodyMedium
        )
    }

    private fun parseMarkdownToAnnotatedString(markdown: String, defaultColor: Color): AnnotatedString {
        // A simple markdown parser for basic formatting
        return buildAnnotatedString {
            withStyle(SpanStyle(color = defaultColor)) {
                // Process lines to handle different markdown elements
                val lines = markdown.split("\n")

                lines.forEachIndexed { index, line ->
                    // Headers
                    if (line.startsWith("# ")) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp)) {
                            append(line.substring(2))
                        }
                    } else if (line.startsWith("## ")) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                            append(line.substring(3))
                        }
                    } else if (line.startsWith("### ")) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                            append(line.substring(4))
                        }
                    }
                    // Code block
                    else if (line.startsWith("```")) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color.DarkGray.copy(alpha = 0.2f)
                            )
                        ) {
                            append(line.substring(3))
                        }
                    }
                    // Bold
                    else if (line.contains("**")) {
                        val parts = line.split("**")
                        for (i in parts.indices) {
                            if (i % 2 == 1) { // Odd indices are inside ** markers
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(parts[i])
                                }
                            } else {
                                append(parts[i])
                            }
                        }
                    }
                    // Italic
                    else if (line.contains("*")) {
                        val parts = line.split("*")
                        for (i in parts.indices) {
                            if (i % 2 == 1) { // Odd indices are inside * markers
                                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                    append(parts[i])
                                }
                            } else {
                                append(parts[i])
                            }
                        }
                    }
                    // Bullet points
                    else if (line.startsWith("- ")) {
                        append("• ${line.substring(2)}")
                    }
                    // Regular text
                    else {
                        append(line)
                    }

                    // Add newline if not the last line
                    if (index < lines.size - 1) {
                        append("\n")
                    }
                }
            }
        }
    }

    fun handleImage(image: ImageIcon, sender: String, b: Boolean) {
        if (b) {
            // Private image
            val chatKey = sender
            val currentMessages = messages.getOrDefault(chatKey, emptyList())
            println("Received private image from $sender")
            messages[chatKey] = currentMessages + ChatMessage(
                sender = sender,
                content = "[Image]",
                isPrivate = true,
                isOutgoing = false,
                image = image
            )
        } else {
            // General image
            val chatKey = "general"  // Images are being sent to general chat
            val currentMessages = messages.getOrDefault(chatKey, emptyList())
            messages[chatKey] = currentMessages + ChatMessage(
                sender = sender,
                content = "[Image]",
                isPrivate = false,
                isOutgoing = false,
                image = image
            )
        }
        playReceiveSound()
    }

    private fun saveImage(image: ImageIcon) {
        Thread {
            try {
                // Create BufferedImage from ImageIcon
                val bufferedImage = BufferedImage(
                    image.iconWidth,
                    image.iconHeight,
                    BufferedImage.TYPE_INT_RGB
                )
                val g = bufferedImage.createGraphics()
                image.paintIcon(null, g, 0, 0)
                g.dispose()

                // Create file chooser dialog
                val fileChooser = JFileChooser()
                fileChooser.dialogTitle = "Save Image"
                fileChooser.fileFilter = object : javax.swing.filechooser.FileFilter() {
                    override fun accept(f: File): Boolean {
                        return f.isDirectory || f.name.lowercase().endsWith(".png")
                    }

                    override fun getDescription(): String {
                        return "PNG Images (*.png)"
                    }
                }
                fileChooser.selectedFile = File("image.png")

                val result = fileChooser.showSaveDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    var selectedFile = fileChooser.selectedFile

                    // Add .png extension if not present
                    if (!selectedFile.name.lowercase().endsWith(".png")) {
                        selectedFile = File(selectedFile.absolutePath + ".png")
                    }

                    ImageIO.write(bufferedImage, "png", selectedFile)

                    // Show success dialog
                    SwingUtilities.invokeLater {
                        JOptionPane.showMessageDialog(
                            null,
                            "Image saved successfully",
                            "Save Complete",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                SwingUtilities.invokeLater {
                    JOptionPane.showMessageDialog(
                        null,
                        "Failed to save image: ${e.message}",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }.start()
    }
}