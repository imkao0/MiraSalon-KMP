package iz.mkao.mirasalon.feature.salon.services.presentation.circuit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.slack.circuit.codegen.annotations.CircuitInject
import iz.mkao.mirasalon.core.common.di.AppScope
import iz.mkao.mirasalon.core.common.util.DateUtils
import iz.mkao.mirasalon.core.common.util.formatRating
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.ReviewBottomSheet
import iz.mkao.mirasalon.core.designsystem.components.ReviewItem
import iz.mkao.mirasalon.core.designsystem.components.ShimmerBlock
import iz.mkao.mirasalon.core.designsystem.theme.SpacingTiny
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.navigation.ServiceRoute
import iz.mkao.mirasalon.core.network.util.toPriceString

@CircuitInject(ServiceRoute.ServiceDetail::class, AppScope::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailUi(state: ServiceDetailState, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MiraTopAppBar(
                title = "",
                onBackClick = { state.eventSink(ServiceDetailEvent.BackClicked) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { state.eventSink(ServiceDetailEvent.NotificationClicked) }) {
                        Box {
                            Icon(
                                Icons.Outlined.NotificationsNone,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurface
                            )

                            if (state.unreadNotificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = SpacingTiny, y = -SpacingTiny)
                                ) {
                                    Text(
                                        text = if (state.unreadNotificationCount > 9) "9+" else state.unreadNotificationCount.toString(),
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = { state.eventSink(ServiceDetailEvent.ToggleFavorite) }) {
                        Icon(
                            imageVector = if (state.isFavorited) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (state.service != null) {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .height(56.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { state.eventSink(ServiceDetailEvent.SaveClicked) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text(
                                text = "SAVE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { state.eventSink(ServiceDetailEvent.BookClicked) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = "BOOK",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    ServiceDetailShimmer()
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { state.eventSink(ServiceDetailEvent.Retry) }) {
                            Text("Retry")
                        }
                    }
                }
                state.service != null -> {
                    var showReviewSheet by remember { mutableStateOf(false) }

                    if (showReviewSheet && state.onReviewSubmit != null) {
                        ReviewBottomSheet(
                            onDismiss = { showReviewSheet = false },
                            onReviewSubmit = state.onReviewSubmit
                        )
                    }

                    ServiceDetailContent(
                        service = state.service,
                        categoryName = state.categoryName,
                        categoryIconKey = state.categoryIconKey,
                        specialists = state.specialists,
                        relatedServices = state.relatedServices,
                        onLeaveReviewClick = { showReviewSheet = true },
                        onSpecialistClick = { state.eventSink(ServiceDetailEvent.SpecialistClicked(it)) },
                        onRelatedServiceClick = { state.eventSink(ServiceDetailEvent.RelatedServiceClicked(it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceDetailContent(
    service: iz.mkao.mirasalon.core.domain.model.Service,
    categoryName: String?,
    categoryIconKey: String?,
    specialists: List<Specialist>,
    relatedServices: List<iz.mkao.mirasalon.core.domain.model.Service>,
    onLeaveReviewClick: () -> Unit,
    onSpecialistClick: (String) -> Unit,
    onRelatedServiceClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large Image or Icon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            if (service.imageUrl != null) {
                AsyncImage(
                    model = service.imageUrl,
                    contentDescription = service.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                val iconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                Icon(
                    imageVector = Icons.Outlined.Spa,
                    contentDescription = null,
                    modifier = Modifier
                        .size(220.dp)
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(iconColor, Color.Transparent)
                                ),
                                blendMode = BlendMode.SrcIn
                            )
                        },
                    tint = iconColor
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Rating Circle at top
        if (service.rating > 0) {
            RatingCircle(rating = service.rating, reviewCount = service.reviews.size)
            Spacer(modifier = Modifier.height(24.dp))
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = service.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "SERVICE ID: #${service.id.takeLast(6).uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Rating chip below service ID
            if (service.rating > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RatingChip(
                        content = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${((service.rating * 10).toInt() / 10.0)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (service.discountPercent > 0) {
                    Text(
                        text = service.discountedPrice.toPriceString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = service.price.toPriceString(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "-${service.discountPercent}%",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = service.price.toPriceString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(label = "Duration", value = "${service.durationMinutes} MIN")
                InfoChip(label = "Category", value = categoryName ?: "General")
                if (service.subCategory != null) {
                    InfoChip(label = "Sub-Category", value = service.subCategory ?: "")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Description",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = service.description,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            DashedDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = "Leave a review",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onLeaveReviewClick() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reviews Section
            if (service.reviews.isNotEmpty()) {
                Text(
                    text = "Reviews (${service.reviews.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                service.reviews.forEach { review ->
                    ReviewItem(
                        userName = review.userName,
                        userAvatarUrl = review.userAvatarUrl,
                        rating = review.rating,
                        comment = review.comment,
                        date = DateUtils.formatDateFull(review.createdAtEpochSeconds)
                    )
                    DashedDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            } else {
                Text(
                    text = "No reviews yet. Be the first!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Specialists Section
            if (specialists.isNotEmpty()) {
                Text(
                    text = "Specialists",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(specialists) { specialist ->
                        SpecialistChip(
                            specialist = specialist,
                            onClick = { onSpecialistClick(specialist.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Related Services Section
            if (relatedServices.isNotEmpty()) {
                Text(
                    text = "Related Services",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(relatedServices) { relatedService ->
                        RelatedServiceChip(
                            service = relatedService,
                            onClick = { onRelatedServiceClick(relatedService.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ServiceDetailShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShimmerBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            cornerRadius = 12.dp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            ShimmerBlock(
                modifier = Modifier
                    .width(200.dp)
                    .height(28.dp),
                cornerRadius = 4.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            ShimmerBlock(
                modifier = Modifier
                    .width(120.dp)
                    .height(14.dp),
                cornerRadius = 4.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            ShimmerBlock(
                modifier = Modifier
                    .width(100.dp)
                    .height(32.dp),
                cornerRadius = 4.dp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ShimmerBlock(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    cornerRadius = 8.dp
                )
                ShimmerBlock(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    cornerRadius = 8.dp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            ShimmerBlock(
                modifier = Modifier
                    .width(80.dp)
                    .height(14.dp),
                cornerRadius = 4.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            repeat(4) {
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .padding(vertical = 2.dp),
                    cornerRadius = 4.dp
                )
            }
        }
    }
}

@Composable
private fun RatingChip(text: String? = null, content: @Composable (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        if (content != null) {
            content()
        } else if (text != null) {
            Text(text = text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashedDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    thickness: Dp = 1.dp,
    dashLength: Dp = 4.dp,
    gapLength: Dp = 4.dp
) {
    Canvas(modifier.fillMaxWidth().height(thickness)) {
        val dashLengthPx = dashLength.toPx()
        val gapLengthPx = gapLength.toPx()
        val thicknessPx = thickness.toPx()
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLengthPx, gapLengthPx), 0f)
        drawLine(
            color = color,
            start = Offset(0f, thicknessPx / 2),
            end = Offset(size.width, thicknessPx / 2),
            strokeWidth = thicknessPx,
            pathEffect = pathEffect
        )
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SpecialistChip(specialist: Specialist, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
            .width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (specialist.imageUrl != null) {
                AsyncImage(
                    model = specialist.imageUrl,
                    contentDescription = specialist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = specialist.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        if (specialist.rating > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = specialist.rating.formatRating(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RelatedServiceChip(service: iz.mkao.mirasalon.core.domain.model.Service, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
            .width(120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val iconColor = MaterialTheme.colorScheme.primary
            Icon(
                imageVector = Icons.Outlined.Spa,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(iconColor, Color.Transparent)
                            ),
                            blendMode = BlendMode.SrcIn
                        )
                    },
                tint = iconColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = service.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = service.price.toPriceString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RatingCircle(rating: Double, reviewCount: Int) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .background(Color(0xFFFFC107), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = rating.formatRating(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "$reviewCount reviews",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black.copy(alpha = 0.7f)
            )
        }
    }
}

