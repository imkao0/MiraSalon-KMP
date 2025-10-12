package iz.mkao.mirasalon.presentation.dashboard.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraPrimaryDeep
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.designsystem.theme.VelvetaOrange
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.presentation.dashboard.components.DashboardCard

@Composable
fun AnalyticsCard(
    modifier: Modifier = Modifier,
    products: List<Product> = emptyList()
) {
    DashboardCard(modifier, title = "Inventory Alerts", subtitle = "Products low on stock") {
        Column {
            if (products.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("All products well stocked", color = MiraTextSecondary)
                }
            } else {
                products.take(4).forEachIndexed { index, product ->
                    val progress = (product.stockQuantity.toFloat() / 20f).coerceIn(0f, 1f)

                    ProductRow(
                        label = product.name,
                        count = product.stockQuantity,
                        progress = progress,
                        color = if (product.stockQuantity < 5) MiraPrimaryDeep else VelvetaOrange
                    )

                    if (index < products.size - 1 && index < 3) {
                        HorizontalDivider(color = MiraBorder, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductRow(label: String, count: Int, progress: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = MiraTextPrimary, fontWeight = FontWeight.Medium)
            Text("$count left", color = MiraTextPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = color,
            trackColor = MiraBorder,
        )
    }
}
