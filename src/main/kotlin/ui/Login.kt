package ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import net.ClientSocket
import net.ServerBroadcastReceiver
import javax.imageio.ImageIO

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var serverIP by remember { mutableStateOf("localhost") }
    var serverPort by remember { mutableStateOf(2005) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Create a state for the server list that updates when the map changes
    val serverList = remember { mutableStateListOf<ServerBroadcastReceiver.ServerInfo>() }
    var selectedServer by remember { mutableStateOf<ServerBroadcastReceiver.ServerInfo?>(null) }

    // Start server discovery when the component mounts
    DisposableEffect(Unit) {
        val receiver = ServerBroadcastReceiver()
        val thread = Thread(receiver)
        thread.start()

        // Update the server list every second
        val updateThread = Thread {
            while (true) {
                try {
                    Thread.sleep(1000)
                    // We need to update on the UI thread
                    java.awt.EventQueue.invokeLater {
                        val availableServers = ServerBroadcastReceiver.availableServers.values.toList()
                        serverList.clear()
                        serverList.addAll(availableServers)
                    }
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
        updateThread.isDaemon = true
        updateThread.start()

        onDispose {
            receiver.stop()
            updateThread.interrupt()
        }
    }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Server list panel (left side)
        Card(
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Available Servers",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (serverList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Searching for servers...")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(serverList) { server ->
                            ServerListItem(
                                server = server,
                                isSelected = selectedServer == server,
                                onClick = {
                                    selectedServer = server
                                    serverIP = server.ipAddress
                                    serverPort = server.port.toIntOrNull() ?: 2005
                                }
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        serverIP = "localhost"
                        serverPort = 2005
                        selectedServer = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use Manual Settings")
                }
            }
        }

        // Login panel (right side)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = BitmapPainter(
                    ImageIO.read(javaClass.getResourceAsStream("/logo.png")!!).toComposeImageBitmap()
                ),
                contentDescription = "Logo",
                modifier = Modifier.size(200.dp)
            )

            Text(
                text = "ProjectEXO",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = serverIP,
                onValueChange = {
                    serverIP = it
                    selectedServer = null
                },
                label = { Text("Server IP") },
                modifier = Modifier.fillMaxWidth(0.7f),
                singleLine = true,
                enabled = selectedServer == null,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = serverPort.toString(),
                onValueChange = {
                    serverPort = it.toIntOrNull() ?: 2005
                    selectedServer = null
                },
                label = { Text("Server Port") },
                modifier = Modifier.fillMaxWidth(0.7f),
                singleLine = true,
                enabled = selectedServer == null,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(0.7f),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(0.7f),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = {
                        if (!isLoading && username.isNotBlank() && password.isNotBlank()) {
                            attemptLogin(username, password, serverIP, serverPort,
                                onLoadingChange = { isLoading = it },
                                onErrorChange = { errorMessage = it },
                                onLoginSuccess = onLoginSuccess
                            )
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (!isLoading && username.isNotBlank() && password.isNotBlank()) {
                        attemptLogin(username, password, serverIP, serverPort,
                            onLoadingChange = { isLoading = it },
                            onErrorChange = { errorMessage = it },
                            onLoginSuccess = onLoginSuccess
                        )
                    }
                },
                enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Login")
                }
            }
        }
    }
}

@Composable
private fun ServerListItem(
    server: ServerBroadcastReceiver.ServerInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.serverName,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${server.ipAddress}:${server.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isSelected) {
            RadioButton(
                selected = true,
                onClick = null
            )
        }
    }
}

private fun attemptLogin(
    username: String,
    password: String,
    serverIP: String,
    serverPort: Int,
    onLoadingChange: (Boolean) -> Unit,
    onErrorChange: (String?) -> Unit,
    onLoginSuccess: () -> Unit
) {
    onLoadingChange(true)
    onErrorChange(null)

    // Use a separate thread for network operations
    Thread {
        val success = ClientSocket.init(username, password, serverIP, serverPort)

        // Update UI on the main thread
        java.awt.EventQueue.invokeLater {
            onLoadingChange(false)

            if (success) {
                onLoginSuccess()
            } else {
                onErrorChange("Invalid username or password")
            }
        }
    }.start()
}