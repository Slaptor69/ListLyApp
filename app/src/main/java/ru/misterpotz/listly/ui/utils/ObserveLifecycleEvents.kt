package ru.misterpotz.listly.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Превращает lifecycle-события Compose-экрана в обычные callback'и.
 *
 * В этом проекте helper нужен, чтобы UI отправлял в Store события `OnResume` и `OnPause`.
 * Для ELM это удобно: lifecycle тоже становится входным Event, а не скрытой магией.
 */
@Composable
fun ObserveLifecycleEvents(
    onResume: () -> Unit,
    onPause: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    onResume()
                }

                Lifecycle.Event.ON_PAUSE -> {
                    onPause()
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        // При уходе composable удаляем observer, чтобы не было утечек.
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
