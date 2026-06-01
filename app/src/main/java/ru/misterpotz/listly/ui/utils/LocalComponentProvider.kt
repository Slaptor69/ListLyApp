package ru.misterpotz.listly.ui.utils

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import ru.misterpotz.listly.GlobalAppNavKey
val LocalAppComponentProvider =
    staticCompositionLocalOf<Context> { error("no AppComponent provided") }
val LocalGlobalBackstackProvider =
    staticCompositionLocalOf<NavBackStack<GlobalAppNavKey>> { error("No backstack provider") }
