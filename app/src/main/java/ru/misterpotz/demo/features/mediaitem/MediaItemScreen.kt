package ru.misterpotz.demo.features.mediaitem

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import ru.misterpotz.demo.R
import ru.misterpotz.demo.appComponent
import ru.misterpotz.demo.ui.theme.Typography
import ru.misterpotz.demo.ui.utils.StandardElmScreen

@Composable
fun MediaItemScreenEntry(
    mediaItem: Int
) {
    StandardElmScreen(
        storeFactory = { appComponent.mediaItemStoreFactory.create(mediaItem) },
        onEffect = {
            when (it) {
                MediaItemEffect.Close -> backstack.removeLastOrNull()
            }
        },
        body = { state, onEffect -> MediaItemScreenContent(state, onEffect) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaItemScreenContent(
    state: MediaItemState,
    onEvent: (MediaItemEvent) -> Unit,
) {
    val item = state.mediaItem.content ?: run {
        Box(Modifier.fillMaxSize()) {
            Text("Позиция не загружена")
        }
        return
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(MediaItemEvent.Close) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = { onEvent(MediaItemEvent.ToggleTracked) },
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.tracked.isLoading) {
                        CircularProgressIndicator(
                            Modifier.size(24.dp),
                        )
                    } else {
                        Text(if (item.tracked) "Untrack" else "Track")
                    }
                }


                Button(
                    onClick = { onEvent(MediaItemEvent.ToggleReadlist) },
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.readlist.isLoading) {
                        CircularProgressIndicator(
                            Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    } else {
                        Text(if (item.inReadlist) "Remove" else "Add to readlist")
                    }
                }
            }
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(inner)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val ctx = LocalContext.current
            AsyncImage(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(400.dp)
                    .clip(RoundedCornerShape(12.dp)),
                imageLoader = appComponent.imageLoader,
                model = ImageRequest.Builder(ctx)
                    .data(item.imageUrl)
                    .crossfade(true)
                    .listener(
                        onError = { _, result ->
                            Log.e("Coil", "Load failed", result.throwable)
                        },
                        onStart = { Log.d("Coil", "Start") },
                        onSuccess = { _, _ -> Log.d("Coil", "Success") }
                    )
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Fit
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { onEvent(MediaItemEvent.ToggleTracked) },
                    label = { Text(if (item.tracked) "Tracked" else "Not tracked") }
                )
                AssistChip(
                    onClick = { onEvent(MediaItemEvent.ToggleReadlist) },
                    label = { Text(if (item.inReadlist) "In readlist" else "Not in readlist") }
                )
            }

            if (!item.annotation.isNullOrBlank()) {
                Text(
                    text = item.annotation,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "No annotation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

