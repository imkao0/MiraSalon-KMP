package iz.mkao.mirasalon.feature.products.presentation.circuit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import iz.mkao.mirasalon.core.common.util.DateUtils
import iz.mkao.mirasalon.core.common.util.toPriceString
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.ReviewBottomSheet
import iz.mkao.mirasalon.core.designsystem.components.ReviewItem
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.IconSizeSmall
import iz.mkao.mirasalon.core.designsystem.theme.RadiusSmall
import iz.mkao.mirasalon.core.designsystem.theme.RadiusPromo
import iz.mkao.mirasalon.core.designsystem.theme.SpacingDefault
import iz.mkao.mirasalon.core.designsystem.theme.SpacingExtraLarge
import iz.mkao.mirasalon.core.designsystem.theme.SpacingLarge
import iz.mkao.mirasalon.core.designsystem.theme.SpacingMedium
import iz.mkao.mirasalon.core.designsystem.theme.SpacingSmall
import iz.mkao.mirasalon.core.designsystem.theme.SpacingTiny
import iz.mkao.mirasalon.core.designsystem.theme.StepperButtonSize
import iz.mkao.mirasalon.core.network.config.ApiEndpoints

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailContent(
    state: ProductDetailState,
    modifier: Modifier = Modifier,
) {
    var quantity by remember { mutableStateOf(1) }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                MiraTopAppBar(
                    title = state.product?.category ?: "Product",
                    onBackClick = { state.eventSink(ProductDetailEvent.Back) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = Color.White,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = { state.eventSink(ProductDetailEvent.ToggleWishlist) }) {
                            Icon(
                                imageVector = if (state.isWishlisted) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Wishlist",
                                tint = if (state.isWishlisted) Color.Red else Color.White
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            state.product?.let { product ->
                ProductDetailBottomBar(
                    totalAmount = product.discountedPrice * quantity,
                    onOrderNowClick = { state.eventSink(ProductDetailEvent.AddToCart(product.id, quantity)) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    ShimmerLoading(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Text(
                        text = state.error,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                state.product != null -> {
                    val product = state.product
                    var showReviewSheet by remember { mutableStateOf(false) }
                    
                    if (showReviewSheet) {
                        ReviewBottomSheet(
                            onDismiss = { showReviewSheet = false },
                            onReviewSubmit = { rating, comment ->
                                state.eventSink(ProductDetailEvent.SubmitReview(rating, comment))
                                Result.success(Unit)
                            }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        ProductImagePager(
                            images = listOf(product.imageUrl)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(SpacingMedium)
                        ) {
                            ProductPriceRow(
                                productName = product.name,
                                weightLabel = product.category,
                                originalPrice = product.price,
                                discountedPrice = product.discountedPrice,
                                quantity = quantity,
                                stockQuantity = product.stockQuantity,
                                onIncrement = { if (quantity < product.stockQuantity) quantity++ },
                                onDecrement = { if (quantity > 1) quantity-- }
                            )
                            Spacer(Modifier.height(SpacingMedium))
                            Text(
                                text = "Brand: Mira Salon",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(SpacingDefault))
                            RatingChipsRow(
                                ratingsCount = product.reviewCount,
                                reviewsCount = product.reviewCount,
                                ratingScore = product.averageRating
                            )
                            Spacer(Modifier.height(SpacingLarge))
                            Text(
                                text = "Description",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(SpacingSmall))
                            Text(
                                text = product.description,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(SpacingLarge))
                            
                            DashedDivider(modifier = Modifier.padding(vertical = SpacingMedium))
                            
                            Text(
                                text = "Leave a review",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { showReviewSheet = true }
                            )
                            
                            Spacer(Modifier.height(SpacingMedium))
                            
                            if (state.reviews.isNotEmpty()) {
                                Text(
                                    text = "Reviews (${state.reviews.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(SpacingSmall))
                                
                                state.reviews.forEach { review ->
                                    ReviewItem(
                                        userName = review.userName,
                                        userAvatarUrl = review.userAvatarUrl,
                                        rating = review.rating,
                                        comment = review.comment,
                                        date = DateUtils.formatDateFull(review.createdAtEpochSeconds)
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = SpacingTiny),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            } else {
                                Text(
                                    text = "No reviews yet. be the first to review!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Spacer(Modifier.height(SpacingExtraLarge))
                        }
                        
                        Spacer(modifier = Modifier.height(padding.calculateBottomPadding()))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductImagePager(
    images: List<String>
) {
    val pagerState = rememberPagerState(pageCount = { images.size })
    Box(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) { page ->
            val imageUrl = images.getOrNull(page)
            val resolvedUrl = ApiEndpoints.resolveImageUrl(imageUrl)
            AsyncImage(
                model = resolvedUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = SpacingMedium),
            horizontalArrangement = Arrangement.spacedBy(SpacingTiny + 2.dp)
        ) {
            repeat(images.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == pagerState.currentPage) SpacingSmall else SpacingSmall - 2.dp)
                        .background(
                            color = if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun ProductPriceRow(
    productName: String,
    weightLabel: String,
    originalPrice: Double,
    discountedPrice: Double,
    quantity: Int,
    stockQuantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Text(text = weightLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(SpacingTiny))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = productName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(SpacingSmall))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingSmall)
        ) {
            Text(
                text = originalPrice.toPriceString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline,
                textDecoration = TextDecoration.LineThrough
            )
            Text(
                text = discountedPrice.toPriceString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        QuantityStepper(
            quantity = quantity,
            stockQuantity = stockQuantity,
            onIncrement = onIncrement,
            onDecrement = onDecrement
        )
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    stockQuantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    minQuantity: Int = 1
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SpacingMedium)) {
        IconButton(
            onClick = { if (quantity > minQuantity) onDecrement() },
            modifier = Modifier
                .size(StepperButtonSize)
                .background(Color.Transparent, CircleShape)
        ) {
            Icon(Icons.Outlined.Remove, contentDescription = "Decrease quantity", tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            text = "$quantity",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(
            onClick = onIncrement,
            enabled = quantity < stockQuantity,
            modifier = Modifier
                .size(StepperButtonSize)
                .background(
                    if (quantity < stockQuantity) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                )
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = "Increase quantity",
                tint = if (quantity < stockQuantity) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RatingChipsRow(
    ratingsCount: Int,
    reviewsCount: Int,
    ratingScore: Double
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SpacingSmall)) {
        RatingChip(
            content = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Star, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(IconSizeSmall))
                    Spacer(Modifier.width(SpacingTiny))
                    Text(text = "$ratingScore", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
        RatingChip(text = "$ratingsCount Ratings")
        RatingChip(text = "$reviewsCount Reviews")
    }
}

@Composable
private fun RatingChip(text: String? = null, content: @Composable (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(RadiusSmall))
            .padding(horizontal = SpacingDefault, vertical = SpacingTiny + 2.dp)
    ) {
        if (content != null) {
            content()
        } else if (text != null) {
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun ProductDetailBottomBar(
    totalAmount: Double,
    onOrderNowClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(SpacingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Total amount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = totalAmount.toPriceString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(RadiusPromo))
                .clickable(onClick = onOrderNowClick)
                .padding(horizontal = SpacingLarge + 4.dp, vertical = SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingSmall)
        ) {
            Icon(Icons.Outlined.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
            Text(text = "Order Now", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
