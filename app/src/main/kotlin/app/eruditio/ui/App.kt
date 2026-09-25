package app.eruditio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.eruditio.ui.theme.EruditioTheme

/** The Paper shell: pinned navigation and a Resume-first reading desk. */
@Composable
fun App(vm: EruditioViewModel) {
    val palette by vm.palette.collectAsState()
    EruditioTheme(palette) {
        var tabIndex by rememberSaveable { mutableIntStateOf(AppTab.TODAY.ordinal) }
        val tab = AppTab.entries[tabIndex]
        LaunchedEffect(Unit) { vm.refresh() }
        LifecycleResumeEffect(Unit) {
            vm.refresh()
            onPauseOrDispose { }
        }
        // Checkpoint pause (ticket 14): return to Today when the reader sets
        // the shell's pauseToHome flag.
        if (vm.pauseToHome) {
            LaunchedEffect(Unit) {
                tabIndex = AppTab.TODAY.ordinal
                vm.consumePauseToHome()
            }
        }
        val homeState by vm.home.collectAsState()
        val gleanings by vm.gleanings.collectAsState()
        val progressState by vm.progress.collectAsState()

        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    AppTab.TODAY -> EditorialHomeScreen(
                        vm,
                        homeState,
                        Modifier.fillMaxSize(),
                        onOpenLibrary = { tabIndex = AppTab.LIBRARY.ordinal },
                        onOpenGleanings = { tabIndex = AppTab.GLEANINGS.ordinal },
                        gleanings = gleanings,
                    )
                    AppTab.LIBRARY -> LibraryScreen(vm, Modifier.fillMaxSize())
                    AppTab.GLEANINGS -> GleaningsScreen(
                        gleanings = gleanings,
                        modifier = Modifier.fillMaxSize(),
                        titleOf = vm::titleOf,
                    )
                    AppTab.PROGRESS -> ProgressScreen(
                        progressState,
                        Modifier.fillMaxSize(),
                        onOpenLibrary = { tabIndex = AppTab.LIBRARY.ordinal },
                    )
                    AppTab.SETTINGS -> SettingsScreen(vm, Modifier.fillMaxSize())
                }
            }
            BottomNav(tab) { destination ->
                tabIndex = destination.ordinal
                vm.refresh()
            }
        }
    }
}

/** Bottom navigation: always visible, anchored to the bottom edge. */
@Composable
private fun BottomNav(selected: AppTab, onSelect: (AppTab) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTab.entries.forEach { destination ->
                    val color =
                        if (destination == selected) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(
                        modifier = Modifier
                            .clickable { onSelect(destination) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (destination == selected) {
                            Box(
                                Modifier.padding(bottom = 3.dp).size(4.dp).background(
                                    MaterialTheme.colorScheme.tertiary,
                                    CircleShape,
                                ),
                            )
                        }
                        Text(destination.glyph, color = color, style = MaterialTheme.typography.titleMedium)
                        Text(destination.label, color = color, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
