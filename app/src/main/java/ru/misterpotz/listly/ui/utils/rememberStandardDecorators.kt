package ru.misterpotz.listly.ui.utils

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.ViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

/**
 * Возвращает стандартный набор decorators для Navigation 3.
 *
 * Роль в структуре: сохраняет ViewModel и saveable-state между переходами по backstack.
 * Это особенно полезно для экранов со Store, чтобы их состояние не терялось без необходимости.
 */
@Composable
fun <T : Any> rememberStandardDecorators(): List<ViewModelStoreNavEntryDecorator<T>> {
    return listOf(
        rememberSaveableStateHolderNavEntryDecorator<T>(),
        rememberViewModelStoreNavEntryDecorator()
    ) as List<ViewModelStoreNavEntryDecorator<T>>
}
