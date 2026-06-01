package ru.misterpotz.listly.ui.utils

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.ViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
@Composable
fun <T : Any> rememberStandardDecorators(): List<ViewModelStoreNavEntryDecorator<T>> {
    return listOf(
        rememberSaveableStateHolderNavEntryDecorator<T>(),
        rememberViewModelStoreNavEntryDecorator()
    ) as List<ViewModelStoreNavEntryDecorator<T>>
}
