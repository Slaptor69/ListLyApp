package ru.misterpotz.listly.ui.utils

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import money.vivid.elmslie.android.RetainedElmStore
import money.vivid.elmslie.android.RetainedElmStoreFactory
import money.vivid.elmslie.core.store.ElmStore
import ru.misterpotz.listly.GlobalAppNavKey

/**
 * Универсальная Compose-обвязка над Elmslie Store.
 *
 * Это один из самых полезных файлов для понимания проекта:
 * здесь UI подключается к ELM-циклу.
 *
 * Упрощённая схема такая:
 * 1. Экран передаёт storeFactory.
 * 2. StandardElmScreen создаёт или восстанавливает Store.
 * 3. UI читает State из store.states.
 * 4. UI отправляет Event через store.accept(event).
 * 5. Effect обрабатывается отдельно в onEffect, чтобы не смешивать его со State.
 */
@Composable
fun <Event : Any, Effect : Any, State : Any, Command : Any> StandardElmScreen(
    storeFactory: () -> ElmStore<Event, State, Effect, Command>,
    onEffect: EffectHandlerScope.(effect: Effect) -> Unit = { },
    body: @Composable (state: State, onEvent: (Event) -> Unit) -> Unit
) {
    // RetainedElmStore сохраняет Store при конфигурационных изменениях
    // и не пересоздаёт бизнес-логику на каждый recomposition.
    val viewModel = viewModel<RetainedElmStore<Event, Effect, State>>(
        factory = RetainedElmStoreFactory(
            LocalSavedStateRegistryOwner.current,
            Bundle(),
            storeFactory = {
                storeFactory()
            },
            saveState = {}
        )
    )
    val currentCatalogState = viewModel.store.states.collectAsState().value
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val globalBackstack = LocalGlobalBackstackProvider.current
    val context = LocalContext.current
    val onEffect by rememberUpdatedState(onEffect)

    // Эффекты собираем отдельно от State.
    // Это важно в ELM: State описывает, "что рисовать",
    // а Effect описывает одноразовое действие вроде навигации.
    LaunchedEffect(context, globalBackstack) {
        val effectHandlerScope: EffectHandlerScope = object : EffectHandlerScope {
            override val backstack: NavBackStack<GlobalAppNavKey> = globalBackstack
            override val context: Context = context
        }
        withContext(Dispatchers.Main.immediate) {
            viewModel.store.effects
                .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
                .collect {
                    onEffect.invoke(effectHandlerScope, it)
                }
        }
    }

    body(currentCatalogState, { viewModel.store.accept(it) })
}

/**
 * Контекст для обработки Effect.
 *
 * Сюда мы складываем вещи, которые чаще всего нужны одноразовым эффектам:
 * навигацию и Android context.
 */
interface EffectHandlerScope {
    val backstack: NavBackStack<GlobalAppNavKey>
    val context: Context
}
