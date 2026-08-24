package iz.mkao.mirasalon.presentation.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.designsystem.theme.RadiusSmall
import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStatus

@Composable
fun FilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    color: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) color else Color.White
    val finalTextColor = if (selected) Color.White else MiraTextSecondary
    val badgeBg = if (selected) Color.White.copy(alpha = 0.2f) else color.copy(alpha = 0.1f)
    val badgeText = if (selected) Color.White else color

    Row(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (selected) color else MiraBorder,
                shape = RoundedCornerShape(2.dp),
            )
            .background(backgroundColor, RoundedCornerShape(2.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = finalTextColor)
        Box(
            modifier = Modifier
                .background(badgeBg, RoundedCornerShape(2.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(text = count.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeText)
        }
    }
}

@Composable
fun FilterDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier
                .width(180.dp)
                .height(40.dp)
                .border(1.dp, MiraBorder, RoundedCornerShape(2.dp))
                .clickable { expanded = true },
            color = Color.White,
            shape = RoundedCornerShape(2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedOption.ifBlank { label },
                    fontSize = 13.sp,
                    color = if (selectedOption.isBlank()) MiraTextSecondary else MiraTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = MiraTextSecondary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(180.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 13.sp) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CalendarFilterBar(
    selectedService: String,
    onServiceSelected: (String) -> Unit,
    selectedEmployment: String,
    onEmploymentSelected: (String) -> Unit,
    selectedStatus: AdminAppointmentStatus?,
    onStatusSelected: (AdminAppointmentStatus?) -> Unit,
    statusCounts: Map<AdminAppointmentStatus, Int>,
    activeTab: Int,
    onTabChange: (Int) -> Unit
) {
    val totalCount = remember(statusCounts) { statusCounts.values.sum() }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterDropdown(
            label = "Services",
            options = listOf("All Services", "Haircut", "Manicure", "Epilation", "Coloring"),
            selectedOption = selectedService,
            onOptionSelected = onServiceSelected
        )

        FilterDropdown(
            label = "Employment",
            options = listOf("All Staff", "Full-time", "Part-time", "Contractors"),
            selectedOption = selectedEmployment,
            onOptionSelected = onEmploymentSelected
        )

        Spacer(modifier = Modifier.width(8.dp))


        FilterChip(
            label = "All",
            count = totalCount,
            selected = selectedStatus == null,
            color = MiraCoral,
            textColor = MiraCoral,
            onClick = { onStatusSelected(null) }
        )


        AdminAppointmentStatus.entries.forEach { status ->
            val count = statusCounts[status] ?: 0
            val color = when (status) {
                AdminAppointmentStatus.Confirmed -> MaterialTheme.colorScheme.primary
                AdminAppointmentStatus.Completed -> MaterialTheme.colorScheme.tertiary
                AdminAppointmentStatus.Cancelled -> MaterialTheme.colorScheme.error
            }
            

            if (count > 0 || selectedStatus == status) {
                FilterChip(
                    label = status.name,
                    count = count,
                    selected = selectedStatus == status,
                    color = color,
                    textColor = color,
                    onClick = {
                        val newStatus = if (selectedStatus == status) null else status
                        onStatusSelected(newStatus)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedTabSwitch(
            activeTab = activeTab,
            onTabChange = onTabChange
        )
    }
}

@Composable
fun AnimatedTabSwitch(
    activeTab: Int,
    onTabChange: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .width(260.dp)
            .height(44.dp),
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(RadiusSmall)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf("Month", "Week", "Day")




            
            tabs.forEachIndexed { index, label ->
                val isSelected = activeTab == index
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(RadiusSmall))
                        .background(if (isSelected) MiraCoral else Color.Transparent)
                        .clickable { onTabChange(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
