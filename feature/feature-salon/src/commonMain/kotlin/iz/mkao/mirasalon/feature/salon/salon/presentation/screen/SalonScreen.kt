package iz.mkao.mirasalon.feature.salon.salon.presentation.screen

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.common.di.AppScope
import iz.mkao.mirasalon.core.common.util.formatRating
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.ShimmerBlock
import iz.mkao.mirasalon.core.designsystem.theme.AvatarSize
import iz.mkao.mirasalon.core.designsystem.theme.ButtonHeight
import iz.mkao.mirasalon.core.designsystem.theme.CardWidthLarge
import iz.mkao.mirasalon.core.designsystem.theme.CategorySize
import iz.mkao.mirasalon.core.designsystem.theme.IconSizeLarge
import iz.mkao.mirasalon.core.designsystem.theme.IconSizeMedium
import iz.mkao.mirasalon.core.designsystem.theme.IconSizeTiny
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.designsystem.theme.OfferCardHeight
import iz.mkao.mirasalon.core.designsystem.theme.OfferCardWidth
import iz.mkao.mirasalon.core.designsystem.theme.RadiusExtraLarge
import iz.mkao.mirasalon.core.designsystem.theme.RadiusFull
import iz.mkao.mirasalon.core.designsystem.theme.RadiusLarge
import iz.mkao.mirasalon.core.designsystem.theme.RadiusMedium
import iz.mkao.mirasalon.core.designsystem.theme.RadiusPromo
import iz.mkao.mirasalon.core.designsystem.theme.SpacingDefault
import iz.mkao.mirasalon.core.designsystem.theme.SpacingExtraLarge
import iz.mkao.mirasalon.core.designsystem.theme.SpacingLarge
import iz.mkao.mirasalon.core.designsystem.theme.SpacingMedium
import iz.mkao.mirasalon.core.designsystem.theme.SpacingSmall
import iz.mkao.mirasalon.core.designsystem.theme.SpacingTiny
import iz.mkao.mirasalon.core.designsystem.theme.StarSizeSmall
import iz.mkao.mirasalon.core.designsystem.theme.Success
import iz.mkao.mirasalon.core.domain.model.PromoType
import iz.mkao.mirasalon.core.domain.model.Promotion
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.core.navigation.ServiceRoute
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.feature.salon.salon.presentation.circuit.SalonEvent
import iz.mkao.mirasalon.feature.salon.salon.presentation.circuit.SalonState
import iz.mkao.mirasalon.feature.salon.salon.presentation.components.ExpertsPromoCard
import iz.mkao.mirasalon.feature.salon.salon.presentation.components.HairColorPromoCard
import iz.mkao.mirasalon.feature.salon.salon.presentation.components.SpecialistMatchPromoCard
import iz.mkao.mirasalon.feature.salon.services.presentation.circuit.ServiceDetailState
import iz.mkao.mirasalon.feature.salon.services.presentation.circuit.ServiceDetailUi
import iz.mkao.mirasalon.feature.salon.services.presentation.circuit.ServicesState
import iz.mkao.mirasalon.feature.salon.services.presentation.circuit.ServicesUi
import iz.mkao.mirasalon.feature.services.presentation.components.CategorySelectorRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.seconds


@CircuitInject(BottomNavKey.Home::class, AppScope::class)
@Composable
fun SalonScreenCircuitContent(
    state: SalonState,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state.promotions.size) {
        Napier.d("SalonScreen: Promotions loaded: ${state.promotions.size}")
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(state.promotionCodeToCopy) {
        val code = state.promotionCodeToCopy ?: return@LaunchedEffect
        clipboardManager.setText(AnnotatedString(code))
        state.eventSink(SalonEvent.PromotionCodeConsumed)
        snackbarHostState.showSnackbar("Promo code copied")
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(message = message, actionLabel = "Retry")
        if (result == SnackbarResult.ActionPerformed) {
            state.eventSink(SalonEvent.Retry)
        }
    }

    SalonScreenWrapper(
        state = state,
        snackbarHostState = snackbarHostState,
        onNotificationClick = { state.eventSink(SalonEvent.NotificationClicked) },
        onFavoriteClick = { state.eventSink(SalonEvent.FavoriteClicked) },
        onCategoryClick = { state.eventSink(SalonEvent.CategorySelected(it)) },
        onViewAllCategories = { state.eventSink(SalonEvent.ViewAllCategories) },
        onSpecialistClick = { state.eventSink(SalonEvent.SpecialistSelected(it)) },
        onViewAllSpecialists = { state.eventSink(SalonEvent.ViewAllSpecialists) },
        onPromotionClick = { state.eventSink(SalonEvent.PromotionClicked(it)) },
        onRetry = { state.eventSink(SalonEvent.Retry) }
    )
}

class SalonManualUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is BottomNavKey.Home -> ui<SalonState> { state, _ -> SalonScreenCircuitContent(state) }
            is ServiceRoute.Services -> ui<ServicesState> { state, modifier -> ServicesUi(state, modifier) }
            is ServiceRoute.ServiceDetail -> ui<ServiceDetailState> { state, modifier -> ServiceDetailUi(state, modifier) }
            else -> null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalonScreenWrapper(
    state: SalonState,
    snackbarHostState: SnackbarHostState,
    onNotificationClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onViewAllCategories: () -> Unit,
    onSpecialistClick: (String) -> Unit,
    onViewAllSpecialists: () -> Unit,
    onPromotionClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MiraTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val resolvedAvatarUrl = ApiEndpoints.resolveImageUrl(state.userAvatarUrl)
                        Box(
                            modifier = Modifier
                                .size(AvatarSize)
                                .clip(RoundedCornerShape(RadiusMedium))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (resolvedAvatarUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalPlatformContext.current)
                                        .data(resolvedAvatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "User Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(IconSizeMedium),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(SpacingDefault))
                        Column {
                            Text(
                                text = state.userName?.let { "Hi, $it" } ?: "Hi,",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(IconSizeTiny),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(SpacingTiny))
                                Text(
                                    text = state.userLocation ?: "Set your location",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNotificationClick) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.NotificationsNone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )

                            if (state.inAppNotificationsEnabled && state.unreadNotificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                ) {
                                    Text(
                                        text = if (state.unreadNotificationCount > 5) "5+" else state.unreadNotificationCount.toString(),
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 10.sp
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
                state.isLoading -> LoadingContent()
        else -> SalonContent(
                    state = state,
                    onCategoryClick = onCategoryClick,
                    onViewAllCategories = onViewAllCategories,
                    onSpecialistClick = onSpecialistClick,
                    onViewAllSpecialists = onViewAllSpecialists,
                    onPromotionClick = onPromotionClick,
                )
            }
        }
    }
}

@Composable
private fun SalonContent(
    state: SalonState,
    onCategoryClick: (String) -> Unit,
    onViewAllCategories: () -> Unit,
    onSpecialistClick: (String) -> Unit,
    onViewAllSpecialists: () -> Unit,
    onPromotionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isCategoriesExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SalonSearchBar(
            query = state.searchQuery,
            onSearchClick = onViewAllCategories,
            modifier = Modifier.padding(horizontal = SpacingLarge, vertical = SpacingMedium)
        )

        SectionHeader(
            title = "Services",
            viewAllText = if (isCategoriesExpanded) "View Less" else "View All",
            showViewAll = state.categories.size > 8,
            onViewAllClicked = { isCategoriesExpanded = !isCategoriesExpanded }
        )

        CategorySelectorRow(
            categories = state.categories,
            selectedCategoryId = state.selectedCategoryId,
            onCategorySelect = onCategoryClick,
            isExpanded = isCategoriesExpanded,
            modifier = Modifier.padding(top = SpacingSmall)
        )

        Spacer(modifier = Modifier.height(SpacingLarge))

        OfferCarousel(
            promotions = state.promotions,
            usedPromotionIds = state.usedPromotionIds,
            onPromotionClick = onPromotionClick
        )

        Spacer(modifier = Modifier.height(SpacingLarge))

        SectionHeader(
            title = "Specialists",
            onViewAllClicked = onViewAllSpecialists
        )

        SpecialistCarousel(
            specialists = state.specialists,
            onSpecialistClicked = onSpecialistClick
        )

        Spacer(modifier = Modifier.height(SpacingExtraLarge))
    }
}

@Composable
private fun SalonSearchBar(
    query: String,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onSearchClick,
        modifier = modifier
            .fillMaxWidth()
            .height(ButtonHeight),
        shape = RoundedCornerShape(RadiusExtraLarge),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (query.isEmpty()) "Search by Salons" else query,
                color = if (query.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Composable
private fun OfferCarousel(
    promotions: List<Promotion>,
    usedPromotionIds: Set<String>,
    onPromotionClick: (String) -> Unit
) {
    if (promotions.isEmpty()) return

    // Use a large number of pages to simulate infinite loop
    val pageCount = if (promotions.size > 1) 10000 else promotions.size
    val pagerState = rememberPagerState(
        initialPage = if (promotions.size > 1) 5000 - (5000 % promotions.size) else 0,
        pageCount = { pageCount }
    )

    val currentPromos by rememberUpdatedState(promotions)
    LaunchedEffect(pagerState) {
        while (true) {
            delay(2.seconds)
            if (currentPromos.size > 1 && !pagerState.isScrollInProgress) {
                try {
                    pagerState.animateScrollToPage(
                        page = pagerState.currentPage + 1,
                        animationSpec = tween(durationMillis = 800)
                    )
                } catch (e: Exception) {
                    yield()
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = SpacingMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = SpacingLarge),
            pageSpacing = SpacingMedium,
            verticalAlignment = Alignment.Top,
            beyondViewportPageCount = 1
        ) { page ->
            val actualIndex = page % promotions.size
            val promo = promotions[actualIndex]
            OfferCard(
                promotion = promo,
                isUsed = promo.id in usedPromotionIds,
                onClick = { onPromotionClick(promo.id ?: "") },
                modifier = Modifier.width(380.dp)
            )
        }

        if (promotions.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(promotions.size) { iteration ->
                    val actualCurrentPage = pagerState.currentPage % promotions.size
                    val isSelected = actualCurrentPage == iteration
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MiraCoral else Color.Gray.copy(alpha = 0.5f))
                            .size(if (isSelected) 10.dp else 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OfferCard(
    promotion: Promotion,
    isUsed: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val resolvedPromoUrl = ApiEndpoints.resolveImageUrl(promotion.imageUrl)
    val ctaText = if (isUsed) "Used" else (promotion.ctaText ?: "Book now")

    when (promotion.type) {
        PromoType.HAIR_COLOR -> {
            HairColorPromoCard(
                title = promotion.title.ifBlank { "-${promotion.discountPercent}%" },
                description = promotion.description.ifBlank { "On Hair Color\nThis Weekend ONLY" },
                ctaText = ctaText,
                imageUrl = resolvedPromoUrl,
                onBookNowClick = onClick,
                isUsed = isUsed,
                modifier = modifier
            )
        }
        PromoType.SPECIALIST_MATCH -> {
            SpecialistMatchPromoCard(
                title = promotion.title.ifBlank { "Choose Your Perfect Match" },
                description = promotion.description.ifBlank { "Select a specialist that fits your style and needs." },
                imageUrl = resolvedPromoUrl,
                isUsed = isUsed,
                modifier = modifier.clickable(onClick = onClick)
            )
        }
        PromoType.EXPERTS -> {
            ExpertsPromoCard(
                title = promotion.title.ifBlank { "Experience Beauty Crafted by Experts" },
                description = promotion.description.ifBlank { "Book with our certified professionals and get the best results." },
                ctaText = ctaText,
                imageUrl = resolvedPromoUrl,
                onBookNowClick = onClick,
                isUsed = isUsed,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    viewAllText: String = "View All",
    showViewAll: Boolean = true,
    onViewAllClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingLarge, vertical = SpacingMedium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (showViewAll) {
            Text(
                text = viewAllText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onViewAllClicked)
            )
        }
    }
}

@Composable
private fun SpecialistCarousel(
    specialists: List<Specialist>,
    onSpecialistClicked: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = SpacingLarge),
        horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
    ) {
        items(specialists) { specialist ->
            SpecialistCard(
                specialist = specialist,
                onClick = { onSpecialistClicked(specialist.id) }
            )
        }
    }
}

@Composable
private fun SpecialistCard(
    specialist: Specialist,
    onClick: () -> Unit
) {
    val isOnline = specialist.isOnline
    val resolvedSpecialistUrl = ApiEndpoints.resolveImageUrl(specialist.imageUrl)
    
    Box(
        modifier = Modifier
            .width(CardWidthLarge)
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(RadiusMedium))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
    ) {
        if (resolvedSpecialistUrl != null) {
            AsyncImage(
                model = resolvedSpecialistUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    null,
                    modifier = Modifier.size(IconSizeLarge),
                    tint = MiraTextSecondary
                )
            }
        }


        if (isOnline) {
            Box(
                modifier = Modifier
                    .padding(SpacingSmall)
                    .size(IconSizeTiny)
                    .clip(CircleShape)
                    .background(Success)
                    .align(Alignment.TopEnd)
            )
        }


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.4f to MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            1.0f to MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(start = SpacingDefault, end = SpacingDefault, bottom = SpacingTiny + 2.dp, top = SpacingMedium),
        ) {
            Column {
                Text(
                    text = specialist.role ?: "Specialist",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Verified Professional",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(SpacingTiny))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(StarSizeSmall)
                    )
                    Spacer(modifier = Modifier.width(SpacingTiny))
                    Text(
                        text = specialist.rating.formatRating(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        ShimmerBlock(
            modifier = Modifier
                .padding(horizontal = SpacingLarge, vertical = SpacingMedium)
                .fillMaxWidth()
                .height(ButtonHeight),
            cornerRadius = RadiusExtraLarge
        )


        ShimmerBlock(
            modifier = Modifier
                .padding(horizontal = SpacingLarge, vertical = SpacingSmall)
                .width(100.dp)
                .height(20.dp)
        )


        Row(
            modifier = Modifier
                .padding(horizontal = SpacingLarge, vertical = SpacingSmall)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            repeat(4) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ShimmerBlock(
                        modifier = Modifier.size(CategorySize),
                        cornerRadius = RadiusFull
                    )
                    Spacer(Modifier.height(SpacingSmall))
                    ShimmerBlock(
                        modifier = Modifier.width(40.dp).height(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SpacingLarge))


        ShimmerBlock(
            modifier = Modifier
                .padding(horizontal = SpacingLarge)
                .width(OfferCardWidth)
                .height(OfferCardHeight),
            cornerRadius = RadiusPromo
        )

        Spacer(modifier = Modifier.height(SpacingLarge))

        // Specialists Section Title
        ShimmerBlock(
            modifier = Modifier
                .padding(horizontal = SpacingLarge, vertical = SpacingSmall)
                .width(120.dp)
                .height(20.dp)
        )

        // Specialists Shimmer
        Row(
            modifier = Modifier
                .padding(horizontal = SpacingLarge, vertical = SpacingSmall)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            repeat(2) {
                ShimmerBlock(
                    modifier = Modifier
                        .width(CardWidthLarge)
                        .aspectRatio(0.7f),
                    cornerRadius = RadiusLarge
                )
            }
        }
    }
}
