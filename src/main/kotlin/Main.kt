import Themes.DarkColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import javax.imageio.ImageIO
import kotlin.system.exitProcess


fun main() = application {
    Window(
        onCloseRequest = { exitProcess(0) },
        title = "Secure Chat Application",
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
                AppNavigation()
            }
        }
    }
}