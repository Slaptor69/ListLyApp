package ru.misterpotz.listly.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.misterpotz.listly.GlobalAppNavKey
import ru.misterpotz.listly.appComponent
import ru.misterpotz.listly.backstackForOpeningAuthFromSettings
import ru.misterpotz.listly.toGlobalAppNavKeys
import ru.misterpotz.listly.ui.utils.StandardElmScreen

@Composable
fun SettingsScreen() {
    StandardElmScreen(
        storeFactory = { appComponent.settingsStoreFactory.create() },
        onEffect = { effect ->
            when (effect) {
                SettingsEffect.OpenAuth -> {
                    val currentStack = backstack.toGlobalAppNavKeys()
                    val targetStack = backstackForOpeningAuthFromSettings(currentStack)
                    if (targetStack != currentStack) {
                        while (backstack.isNotEmpty()) {
                            backstack.removeLastOrNull()
                        }
                        targetStack.forEach { backstack.add(it) }
                    }
                }
            }
        },
        body = { state, onEvent ->
            SettingsContent(state, onEvent)
        }
    )
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onEvent(SettingsEvent.Ui.ThemeClicked) }
        ) {
            Text(
                text = "Тема: " + if (state.currentThemeMode == ThemeMode.DARK) "Тёмная" else "Светлая",
                modifier = Modifier.padding(16.dp)
            )
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onEvent(SettingsEvent.Ui.AuthClicked) }
        ) {
            Text(
                text = "Авторизация",
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    if (state.isThemeDialogVisible) {
        ThemeDialog(state, onEvent)
    }
}

@Composable
private fun ThemeDialog(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onEvent(SettingsEvent.Ui.ThemeDialogDismissed) },
        title = {
            Text("Выбор темы")
        },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEvent(SettingsEvent.Ui.ThemeSelected(mode)) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (mode == ThemeMode.DARK) "Тёмная" else "Светлая",
                            modifier = Modifier.weight(1f)
                        )
                        if (state.currentThemeMode == mode) {
                            Text("✓")
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
