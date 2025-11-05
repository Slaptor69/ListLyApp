package ru.misterpotz.demo.features.news

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.misterpotz.demo.appComponent
import ru.misterpotz.demo.ui.theme.DemoTheme
import ru.misterpotz.demo.ui.utils.ObserveLifecycleEvents
import ru.misterpotz.demo.ui.utils.StandardElmScreen
import ru.misterpotz.demo.utils.log

@Composable
fun NewsScreen() {
    StandardElmScreen(
        storeFactory = {
            "creating new news store".log()
            appComponent.newsStoreFactory.create()
        },
        onEffect = {},
        body = { state, onEvent ->
            NewsScreenContent(state, onEvent)
        },
    )
}

@Composable
fun NewsScreenContent(state: NewState, onEvent: (NewsEvent) -> Unit) {
    ObserveLifecycleEvents(
        onResume = { onEvent(NewsEvent.Ui.OnResume) },
        onPause = { onEvent(NewsEvent.Ui.OnPause) }
    )

    val news = state.news.content ?: run {
        Text("Нет новостей")
        return
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
//        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (news.isEmpty()) {
            Box(Modifier, contentAlignment = Alignment.Center) {
                Text("No news yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier.align(Alignment.Center).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(news) { n ->
                    ElevatedCard {
                        Text(n.text, Modifier.padding(12.dp))
                    }
                }
                item {
                    Spacer(Modifier.height(56.dp))
                }
            }
        }
        FilledTonalButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp)
                .fillMaxWidth(),
            onClick = {}
        ) {
            Text("Обновить новости")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    DemoTheme {
        NewsScreen()
    }
}