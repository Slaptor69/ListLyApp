package ru.misterpotz.demo.ui.utils

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
import ru.misterpotz.demo.GlobalAppNavKey

@Composable
fun <Event : Any, Effect : Any, State : Any, Command : Any> StandardElmScreen(
    storeFactory: () -> ElmStore<Event, State, Effect, Command>,
    onEffect: EffectHandlerScope.(effect: Effect) -> Unit = { },
    body: @Composable (state: State, onEvent: (Event) -> Unit) -> Unit
) {
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

interface EffectHandlerScope {
    val backstack: NavBackStack<GlobalAppNavKey>
    val context: Context
}