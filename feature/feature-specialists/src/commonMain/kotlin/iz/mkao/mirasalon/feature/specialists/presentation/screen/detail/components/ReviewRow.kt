package iz.mkao.mirasalon.feature.specialists.presentation.screen.detail.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.common.util.DateUtils
import iz.mkao.mirasalon.core.designsystem.components.ReviewItem
import iz.mkao.mirasalon.core.domain.model.SpecialistReview

@Composable
fun ReviewRow(review: SpecialistReview) {
    ReviewItem(
        userName = review.userName,
        userAvatarUrl = review.userAvatarUrl,
        rating = review.rating,
        comment = review.comment,
        date = DateUtils.formatDateFull(review.createdAtEpochSeconds),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
