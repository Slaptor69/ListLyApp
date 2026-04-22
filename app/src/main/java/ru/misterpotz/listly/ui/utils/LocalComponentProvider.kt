package ru.misterpotz.listly.ui.utils

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import ru.misterpotz.listly.GlobalAppNavKey

/** CompositionLocal для Android context / app-level зависимостей, если понадобится в UI. */
val LocalAppComponentProvider =
    staticCompositionLocalOf<Context> { error("no AppComponent provided") }

/** CompositionLocal для глобального backstack, которым пользуются эффекты экранов. */
val LocalGlobalBackstackProvider =
    staticCompositionLocalOf<NavBackStack<GlobalAppNavKey>> { error("No backstack provider") }
