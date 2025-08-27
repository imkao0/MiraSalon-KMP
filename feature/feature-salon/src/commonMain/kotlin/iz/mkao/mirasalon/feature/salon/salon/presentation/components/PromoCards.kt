package iz.mkao.mirasalon.feature.salon.salon.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import iz.mkao.mirasalon.core.designsystem.Res
import iz.mkao.mirasalon.core.designsystem.banner
import iz.mkao.mirasalon.core.designsystem.facepowder
import iz.mkao.mirasalon.core.designsystem.theme.OfferCardHeight
import iz.mkao.mirasalon.core.designsystem.theme.RadiusPromo
import iz.mkao.mirasalon.core.designsystem.theme.RadiusPromoInner
import iz.mkao.mirasalon.core.designsystem.theme.SpacingIntermediate
import iz.mkao.mirasalon.core.designsystem.theme.SpacingMedium
import iz.mkao.mirasalon.core.designsystem.theme.SpacingSmall
import org.jetbrains.compose.resources.painterResource

@Composable
fun HairColorPromoCard(
    title: String,
    description: String,
    ctaText: String,
    imageUrl: String?,
    onBookNowClick: () -> Unit,
    modifier: Modifier = Modifier,
    isUsed: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(OfferCardHeight)
            .clip(RoundedCornerShape(RadiusPromo))
            .background(
                if (isUsed) SolidColor(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                else Brush.horizontalGradient(
                    listOf(Color(0xFFE8A8AC), Color(0xFFF0C4C7)),
                ),
            ),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxHeight()
                    .padding(SpacingIntermediate),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = if (isUsed) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    color = if (isUsed) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(SpacingMedium))
                Button(
                    onClick = onBookNowClick,
                    enabled = !isUsed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isUsed) Color.DarkGray else Color.White,
                        contentColor = if (isUsed) Color.LightGray else Color(0xFFD85A30),
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text(text = ctaText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = RadiusPromoInner, bottomStart = RadiusPromoInner))
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = if (isUsed) 0.5f else 1f
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.banner),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = if (isUsed) 0.5f else 1f
                    )
                }
            }
        }
    }
}

@Composable
fun SpecialistMatchPromoCard(
    title: String,
    description: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    isUsed: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(OfferCardHeight)
            .clip(RoundedCornerShape(RadiusPromo))
            .background(
                if (isUsed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else Color(0xFFF6ECE2)
            ),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxHeight()
                    .padding(SpacingIntermediate),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$title \u2726",
                    color = if (isUsed) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF2E2418),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(SpacingSmall))
                Text(
                    text = description,
                    color = if (isUsed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) else Color(0xFF7A6C5D),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = RadiusPromoInner, bottomStart = RadiusPromoInner))
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = if (isUsed) 0.5f else 1f
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.banner),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = if (isUsed) 0.5f else 1f
                    )
                }
            }
        }
    }
}

@Composable
fun ExpertsPromoCard(
    title: String,
    description: String,
    ctaText: String,
    imageUrl: String?,
    onBookNowClick: () -> Unit,
    modifier: Modifier = Modifier,
    isUsed: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(OfferCardHeight)
            .clip(RoundedCornerShape(RadiusPromo))
            .background(
                if (isUsed) SolidColor(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                else Brush.horizontalGradient(
                    listOf(Color(0xFFE8A8AC), Color(0xFFF0C4C7)),
                ),
            ),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(0.66f)
                    .fillMaxHeight()
                    .padding(SpacingIntermediate),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = if (isUsed) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    color = if (isUsed) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(SpacingMedium))
                Button(
                    onClick = onBookNowClick,
                    enabled = !isUsed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isUsed) Color.DarkGray else Color.White,
                        contentColor = if (isUsed) Color.LightGray else Color(0xFFD85A30),
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text(text = ctaText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .weight(0.33f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = RadiusPromoInner, bottomStart = RadiusPromoInner))
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = if (isUsed) 0.5f else 1f
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.facepowder),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = if (isUsed) 0.5f else 1f
                    )
                }
            }
        }
    }
}
