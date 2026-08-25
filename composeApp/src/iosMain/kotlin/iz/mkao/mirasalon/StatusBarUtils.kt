package iz.mkao.mirasalon

import androidx.compose.runtime.Composable
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.setStatusBarStyle

@Composable
actual fun StatusBarEffect(isDarkTheme: Boolean) {
    val style = if (isDarkTheme) UIStatusBarStyleLightContent else UIStatusBarStyleDarkContent
    UIApplication.sharedApplication.setStatusBarStyle(style, true)
}
