import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.ClientSocket
import net.MsgHandler

object Chat {
    // Chat message data class
    data class ChatMessage(
        val sender: String,
        val content: String,
        val isPrivate: Boolean,
        val isOutgoing: Boolean = false
    )

    // Store messages by chat channel (user or "general")
    val messages = mutableStateMapOf<String, List<ChatMessage>>()
    val onlineUsers = mutableStateListOf<String>()

    // Currently selected chat (default is "general")
    private val currentChat = mutableStateOf("general")

    fun receiveMessage(sender: String, message: String, isPrivate: Boolean) {
        val chatKey = if (isPrivate) sender else "general"
        val currentMessages = messages.getOrDefault(chatKey, emptyList())
        messages[chatKey] = currentMessages + ChatMessage(sender, message, isPrivate, false)
    }

    fun updateOnlineUsers(users: List<String>) {
        onlineUsers.clear()
        onlineUsers.addAll(users)
        // Ensure general channel exists
        if (!messages.containsKey("general")) {
            messages["general"] = emptyList()
        }
    }

    @Composable
    fun ChatScreen() {
        var messageInput by remember { mutableStateOf("") }
        val selectedChat = remember { currentChat }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Text(
                text = "Secure Chat",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(modifier = Modifier.fillMaxSize()) {
                // Online users panel
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

                // Chat panel
                Column(
                    modifier = Modifier.weight(3f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    // Chat header
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

                    // Chat messages
                    val chatMessages = messages[selectedChat.value] ?: emptyList()
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        items(chatMessages) { message ->
                            MessageBubble(message)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Input field
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                    if (messageInput.isNotBlank()) {
                                        MsgHandler.sendMessage(messageInput, selectedChat.value)

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

                                        messageInput = ""
                                    }
                                }
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                if (messageInput.isNotBlank()) {
                                    MsgHandler.sendMessage(messageInput, selectedChat.value)

                                    // Add outgoing message to local state
                                    val chatKey = selectedChat.value
                                    val currentMessages = messages.getOrDefault(chatKey, emptyList())
                                    val newMessage = ChatMessage(
                                        sender = "Me",  // Will be replaced with actual username when implemented
                                        content = messageInput,
                                        isPrivate = chatKey != "general",
                                        isOutgoing = true
                                    )
                                    messages[chatKey] = currentMessages + newMessage

                                    messageInput = ""
                                }
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