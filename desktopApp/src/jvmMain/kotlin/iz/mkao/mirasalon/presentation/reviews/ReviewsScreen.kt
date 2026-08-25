package iz.mkao.mirasalon.presentation.reviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.*
import iz.mkao.mirasalon.core.domain.model.AdminReview
import iz.mkao.mirasalon.presentation.*
import iz.mkao.mirasalon.presentation.components.DesktopShell
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReviewsScreenUi(
    state: ReviewsUiState,
    modifier: Modifier = Modifier
) {
    val reviews = state.reviews
    val isLoading = state.isLoading
    val error = state.error

    DesktopShell(
        title = "Customer Feedback",
        subtitle = "Manage salon reviews",
        selectedRoute = "Reviews"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.width(400.dp).height(40.dp),
                color = Color.White,
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, MiraBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp), tint = MiraTextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = state.searchQuery,
                        onValueChange = { state.eventSink(ReviewsEvent.Search(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 14.sp, color = MiraTextPrimary),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (state.searchQuery.isEmpty()) {
                                Text("Search reviews...", fontSize = 14.sp, color = MiraTextSecondary)
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when {
            isLoading && reviews.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ShimmerLoading()
                }
            }
            error != null && reviews.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.ErrorOutline, null, tint = MiraCoral, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Failed to load reviews", style = MaterialTheme.typography.titleMedium)
                        Text(error, style = MaterialTheme.typography.bodySmall, color = MiraTextSecondary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { state.eventSink(ReviewsEvent.Refresh) }, colors = ButtonDefaults.buttonColors(containerColor = MiraCoral)) {
                            Text("Retry")
                        }
                    }
                }
            }
            reviews.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Feedback, null, tint = MiraTextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No reviews found", style = MaterialTheme.typography.titleMedium, color = MiraTextSecondary)
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    items(reviews) { review ->
                        ReviewListItem(
                            review = review,
                            onReply = { state.eventSink(ReviewsEvent.Reply(review.id, it)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewListItem(
    review: AdminReview,
    onReply: (String) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    var showReplyDialog by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(2.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MiraBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        review.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MiraTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            dateFormatter.format(Date(review.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MiraTextSecondary
                        )
                        if (review.targetName != null) {
                            Text(" • ", color = MiraTextSecondary)
                            Text(
                                review.targetName!!,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MiraCoral,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < review.rating) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = VelvetaOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    review.comment ?: "No comment provided.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MiraTextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (review.adminReply != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = VelvetaOffWhiteLight,
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "Admin Reply",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MiraCoral
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                review.adminReply ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MiraTextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (review.adminReply == null) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = { showReplyDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MiraCoral),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Reply, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reply to Review")
                }
            }
        }
    }

    if (showReplyDialog) {
        AlertDialog(
            onDismissRequest = { showReplyDialog = false },
            title = { Text("Reply to ${review.customerName}") },
            text = {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Type your response...") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(2.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReply(replyText)
                        showReplyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MiraCoral)
                ) {
                    Text("Post Reply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/** Circuit [Ui.Factory] binding [DesktopScreen.Reviews] to [ReviewsScreenUi]. */
class ReviewsUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
        is DesktopScreen.Reviews -> ui<ReviewsUiState> { state, _ ->
            ReviewsScreenUi(
                state = state
            )
        }
        else -> null
    }
}
