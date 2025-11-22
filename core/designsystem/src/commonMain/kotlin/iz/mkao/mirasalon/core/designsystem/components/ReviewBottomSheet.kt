package iz.mkao.mirasalon.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.designsystem.theme.MiraSalonTheme
import iz.mkao.mirasalon.core.designsystem.theme.RadiusLarge
import iz.mkao.mirasalon.core.designsystem.theme.RadiusMedium
import iz.mkao.mirasalon.core.designsystem.theme.SpacingIntermediate
import iz.mkao.mirasalon.core.designsystem.theme.SpacingLarge
import iz.mkao.mirasalon.core.designsystem.theme.SpacingMedium
import iz.mkao.mirasalon.core.designsystem.theme.SpacingSection
import iz.mkao.mirasalon.core.designsystem.theme.SpacingSmall
import iz.mkao.mirasalon.core.designsystem.theme.StarSize
import iz.mkao.mirasalon.core.designsystem.theme.TextFieldHeightLarge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

sealed interface SubmissionState {
    object Idle : SubmissionState

    object Submitting : SubmissionState

    object Success : SubmissionState

    data class Error(
        val message: String,
    ) : SubmissionState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewBottomSheet(
    onDismiss: () -> Unit,
    onReviewSubmit: suspend (Int, String) -> Result<Unit>,
) {
    var rating by rememberSaveable { mutableStateOf(0) }
    var reviewText by rememberSaveable { mutableStateOf("") }
    var submissionState by remember { mutableStateOf<SubmissionState>(SubmissionState.Idle) }
    val scope = rememberCoroutineScope()

    val isSubmitEnabled by remember {
        derivedStateOf { rating > 0 && submissionState !is SubmissionState.Submitting }
    }

    LaunchedEffect(submissionState) {
        if (submissionState is SubmissionState.Success) {
            delay(1500L.milliseconds)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (submissionState !is SubmissionState.Submitting) onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingLarge)
                    .padding(bottom = SpacingSection),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val state = submissionState) {
                is SubmissionState.Idle, is SubmissionState.Submitting, is SubmissionState.Error -> {
                    Row(
                        modifier = Modifier.padding(vertical = SpacingMedium),
                        horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
                    ) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < rating) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = if (index < rating) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                                modifier =
                                    Modifier
                                        .size(StarSize)
                                        .clickable(
                                            enabled = state !is SubmissionState.Submitting,
                                        ) { rating = index + 1 },
                            )
                        }
                    }

                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(TextFieldHeightLarge),
                        placeholder = { Text("Write your experience...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        shape = RoundedCornerShape(RadiusLarge),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        enabled = state !is SubmissionState.Submitting,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                    )

                    Spacer(modifier = Modifier.height(SpacingLarge))

                    Button(
                        onClick = {
                            scope.launch {
                                submissionState = SubmissionState.Submitting
                                val result = onReviewSubmit(rating, reviewText)
                                submissionState =
                                    if (result.isSuccess) {
                                        SubmissionState.Success
                                    } else {
                                        SubmissionState.Error(result.exceptionOrNull()?.message ?: "Something went wrong")
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isSubmitEnabled,
                        shape = RoundedCornerShape(RadiusMedium),
                    ) {
                        if (state is SubmissionState.Submitting) {
                            ShimmerLoading(
                                modifier = Modifier.size(SpacingIntermediate),
                            )
                            Spacer(Modifier.width(SpacingSmall))
                        }
                        Text(if (state is SubmissionState.Submitting) "Submitting..." else "Leave a review")
                    }

                    if (state is SubmissionState.Error) {
                        Spacer(Modifier.height(SpacingSmall))
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                is SubmissionState.Success -> {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RadiusLarge))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(SpacingMedium),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(SpacingSmall))
                            Text(
                                "Your review is saved. Thanks!",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewBottomSheetPreview() {
    MiraSalonTheme {
        ReviewBottomSheet(
            onDismiss = {},
            onReviewSubmit = { _, _ -> Result.success(Unit) },
        )
    }
}
