package ru.misterpotz.listly

import androidx.navigation3.runtime.NavBackStack

internal fun NavBackStack<*>.toGlobalAppNavKeys(): List<GlobalAppNavKey> {
    return this.mapNotNull { it as? GlobalAppNavKey }
}

internal fun backstackForOpeningAuthFromSettings(
    currentStack: List<GlobalAppNavKey>
): List<GlobalAppNavKey> {
    if (currentStack.lastOrNull() == GlobalAppNavKey.Auth) {
        return currentStack
    }

    return listOf(
        GlobalAppNavKey.Main(BottomBarDestination.Settings),
        GlobalAppNavKey.Auth
    )
}

internal fun backstackForClosingAuth(
    currentStack: List<GlobalAppNavKey>
): List<GlobalAppNavKey> {
    if (currentStack.lastOrNull() == GlobalAppNavKey.Auth && currentStack.size > 1) {
        val previous = currentStack[currentStack.lastIndex - 1]
        if (previous is GlobalAppNavKey.Main &&
            previous.initialDestination == BottomBarDestination.Settings
        ) {
            return currentStack.dropLast(1)
        }
    }

    return listOf(
        GlobalAppNavKey.Main(BottomBarDestination.Settings)
    )
}
