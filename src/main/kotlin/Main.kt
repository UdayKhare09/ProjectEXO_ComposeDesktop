import Themes.DarkColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ui.Chat
import ui.LoginScreen
import javax.imageio.ImageIO
import kotlin.system.exitProcess


fun main() = application {
    var isLoggedIn by remember { mutableStateOf(false) }

    Window(
        onCloseRequest = { exitProcess(0) },
        title = "Secure ui.Chat Application",
        state = rememberWindowState(width = 1000.dp, height = 700.dp),
        // Icon is in resources folder
        icon = BitmapPainter(
            ImageIO.read(javaClass.getResourceAsStream("/logo.png")!!).toComposeImageBitmap()
        )
    ) {
        MaterialTheme(
            colorScheme = DarkColors
        ) {
            Surface(color = MaterialTheme.colorScheme.background) {
                if (isLoggedIn) {
                    Chat.ChatScreen()
                } else {
                    LoginScreen(
                        onLoginSuccess = {
                            isLoggedIn = true
                        }
                    )
                }
            }
        }
    }
}