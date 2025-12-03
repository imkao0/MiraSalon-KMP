package iz.mkao.mirasalon.feature.notifications.data.repository

import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon

actual fun createDesktopNotifier(): DesktopNotifier? {
    return if (!java.awt.GraphicsEnvironment.isHeadless() && SystemTray.isSupported()) {
        JvmDesktopNotifier()
    } else {
        null
    }
}

class JvmDesktopNotifier : DesktopNotifier {
    override fun showNotification(title: String, message: String) {
        try {
            val systemTray = SystemTray.getSystemTray()
            val image = Toolkit.getDefaultToolkit().createImage("")
            val trayIcon = TrayIcon(image, "MiraSalon")
            trayIcon.isImageAutoSize = true
            trayIcon.toolTip = "MiraSalon"
            
            if (systemTray.trayIcons.none { it.toolTip == trayIcon.toolTip }) {
                systemTray.add(trayIcon)
            }
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO)
        } catch (e: Exception) {
            println("Failed to show desktop notification: ${e.message}")
        }
    }
}
