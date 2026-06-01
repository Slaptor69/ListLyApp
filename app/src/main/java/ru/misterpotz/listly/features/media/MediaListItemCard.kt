package ru.misterpotz.listly.features.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import ru.misterpotz.listly.appComponent
import ru.misterpotz.listly.domain.models.CollectionStatus
import ru.misterpotz.listly.features.favourite.FavouriteButton
import ru.misterpotz.listly.features.liststate.UserMediaBadgesRow
import ru.misterpotz.listly.features.liststate.UserMediaListActionButton
import ru.misterpotz.listly.features.poster.MediaPosterPlaceholder

@Composable
internal fun MediaListItemCard(
    title: String,
    typeTitle: String,
    imageUrl: String?,
    inUserList: Boolean,
    status: CollectionStatus?,
    folderNames: List<String>,
    isFavourite: Boolean,
    userRating: Int?,
    userNote: String?,
    isLoading: Boolean,
    actionTitle: String?,
    onClick: () -> Unit,
    onToggleFavourite: (() -> Unit)?,
    onActionClick: (() -> Unit)?,
) {
    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MediaListPoster(
                title = title,
                imageUrl = imageUrl
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MediaListPosterHeight),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            typeTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (inUserList && onToggleFavourite != null) {
                        FavouriteButton(
                            isFavourite = isFavourite,
                            isLoading = isLoading,
                            onToggle = onToggleFavourite
                        )
                    }
                }

                if (inUserList) {
                    UserMediaBadgesRow(
                        status = status,
                        folderNames = folderNames
                    )
                }

                userNote?.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (inUserList) {
                        Text(
                            text = userRatingText(userRating),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (actionTitle != null && onActionClick != null) {
                        UserMediaListActionButton(
                            title = actionTitle,
                            isLoading = isLoading,
                            onClick = onActionClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaListPoster(
    title: String,
    imageUrl: String?,
) {
    val context = LocalContext.current
    val posterModifier = Modifier
        .size(width = MediaListPosterWidth, height = MediaListPosterHeight)
        .clip(RoundedCornerShape(8.dp))

    if (imageUrl.isNullOrBlank()) {
        MediaPosterPlaceholder(modifier = posterModifier)
        return
    }

    AsyncImage(
        modifier = posterModifier,
        imageLoader = appComponent.imageLoader,
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = title,
        contentScale = ContentScale.Crop
    )
}

private fun userRatingText(userRating: Int?): String {
    return "Оценка: ${userRating?.let { "$it/10" } ?: "нет"}"
}

private val MediaListPosterWidth = 72.dp
private val MediaListPosterHeight = 108.dp
