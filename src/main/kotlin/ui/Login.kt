package ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import net.ClientSocket
import javax.imageio.ImageIO

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = BitmapPainter(
                ImageIO.read(javaClass.getResourceAsStream("/logo.png")!!).toComposeImageBitmap()
            ),
            contentDescription = "Logo",
            modifier = Modifier.size(250.dp)
        )
        Text(
            text = "ProjectEXO",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(0.5f),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = androidx.compose.ui.text.input.ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(0.5f),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = androidx.compose.ui.text.input.ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = {
                    if (!isLoading && username.isNotBlank() && password.isNotBlank()) {
                        attemptLogin(username, password,
                            onLoadingChange = { isLoading = it },
                            onErrorChange = { errorMessage = it },
                            onLoginSuccess = onLoginSuccess
                        )
                    }
                }
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

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
                    attemptLogin(username, password,
                        onLoadingChange = { isLoading = it },
                        onErrorChange = { errorMessage = it },
                        onLoginSuccess = onLoginSuccess
                    )
                }
            },
            enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(0.3f)
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

private fun attemptLogin(
    username: String,
    password: String,
    onLoadingChange: (Boolean) -> Unit,
    onErrorChange: (String?) -> Unit,
    onLoginSuccess: () -> Unit
) {
    onLoadingChange(true)
    onErrorChange(null)

    // Use a separate thread for network operations
    Thread {
        val success = ClientSocket.init(username, password)

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