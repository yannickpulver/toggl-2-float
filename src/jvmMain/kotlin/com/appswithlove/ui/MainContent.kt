package com.appswithlove.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.appswithlove.floaat.FloatPeopleItem
import com.appswithlove.floaat.rgbColor
import com.appswithlove.floaat.totalHours
import com.appswithlove.ui.setup.SetupForm
import com.appswithlove.ui.theme.FloaterTheme
import com.appswithlove.version
import com.google.accompanist.flowlayout.FlowRow
import com.vanpra.composematerialdialogs.DesktopWindowPosition
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogProperties
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val viewModel = MainViewModel()

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainContent() {

    val state = viewModel.state.collectAsState()

    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    MainContent(
        state = state.value,
        clear = viewModel::clear,
        syncProjects = viewModel::fetchProjects,
        removeProjects = viewModel::removeProjects,
        archiveProjects = viewModel::archiveProjects,
        addTimeEntries = viewModel::addTimeEntries,
        save = viewModel::save,
        syncColors = viewModel::updateColors,
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                hasFocus = it.hasFocus
            }
            .focusable()
            .onKeyEvent {
                if (it.isAltPressed && it.key == Key.S) {
                    viewModel.loadSwicaWeek()
                    true
                } else {
                    false
                }
            }
    )

    if (!hasFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

}

@Composable
fun Version(modifier: Modifier = Modifier) {
    Text(
        "Version $version",
        modifier = modifier,
        style = MaterialTheme.typography.caption
    )
}

@Composable
private fun MainContent(
    state: MainState,
    clear: () -> Unit,
    syncProjects: () -> Unit,
    removeProjects: () -> Unit,
    archiveProjects: () -> Unit,
    addTimeEntries: (LocalDate?) -> Unit,
    save: (String?, String?, FloatPeopleItem?) -> Unit,
    syncColors: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier) {
        Box {
            Row {
                Column(
                    modifier = Modifier.weight(1f).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when {
                        state.isValid -> {
                            Welcome(syncProjects, removeProjects)
                            Divider()
                            AddTime(addTimeEntries = addTimeEntries, state.missingEntryDates)
                            Divider()
                            Logs(state.logs)
                        }

                        state.loading -> {
                            Loading()
                        }

                        else -> {
                            SetupForm(state, save)
                        }
                    }
                    Divider()

                }
                Divider(modifier = Modifier.width(1.dp).fillMaxHeight())
                AnimatedVisibility(state.isValid) {
                    YourWeek(state)
                }
            }
            OutlinedButton(onClick = { clear() }, modifier = Modifier.align(Alignment.BottomCenter)) {
                Text("Reset T2F", style = MaterialTheme.typography.caption)
            }

            Version(modifier = Modifier.align(Alignment.BottomStart))
        }
    }
}

@Composable
private fun YourWeek(state: MainState) {
    Column(
        Modifier.background(MaterialTheme.colors.onSurface.copy(0.05f)).width(300.dp).fillMaxHeight().padding(16.dp)
    ) {
        Text("Your Week", style = MaterialTheme.typography.h4)
        if (state.weeklyOverview.isNotEmpty()) {
            Text("Planned hours: ${state.weeklyOverview.totalHours}h", style = MaterialTheme.typography.caption)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
                //Text("Your Week: (Total hours: ${state.weeklyOverview.totalHours}) ")
                state.weeklyOverview.forEach { (project, items) ->
                    val expanded = remember { mutableStateOf(false) }
                    Box(modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { expanded.value = !expanded.value }
                        .background(MaterialTheme.colors.surface)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                //modifier = Modifier.padding(8.dp)
                            ) {
                                project?.rgbColor?.let {
                                    Box(Modifier.clip(CircleShape).background(it).size(10.dp))
                                }

                                project?.name?.let {
                                    Text(it, modifier = Modifier.weight(1f))
                                }
                                Text("${items.totalHours}h")
                            }

                            AnimatedVisibility(expanded.value, enter = expandVertically(), exit = shrinkVertically()) {
                                Column {
                                    items.forEach {
                                        Divider()
                                        Row {
                                            Column(modifier = Modifier.weight(1f)) {
                                                it.phase?.name?.let {
                                                    Text(
                                                        it,
                                                        style = MaterialTheme.typography.caption,

                                                        )
                                                }
                                                if (it.task.name.isNotEmpty()) {
                                                    Text(it.task.name, style = MaterialTheme.typography.caption)
                                                }
                                            }

                                            if (items.size > 1) {
                                                Text(
                                                    "${it.weekHours}h",
                                                    style = MaterialTheme.typography.caption
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Loading()
        }
    }
}

@Composable
private fun Welcome(syncProjects: () -> Unit, removeProjects: () -> Unit) {
    Text(
        "Happy ${LocalDate.now().dayOfWeek.toString().lowercase().capitalize()}! 🎉",
        style = MaterialTheme.typography.h4
    )
    Text(
        "If you need to add all of your Float projects & tasks to Toggl - Use the button below. You can also run it again to get the latest projects updated.",
        modifier = Modifier.fillMaxWidth(0.8f)
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        Button(onClick = syncProjects) {
            Text("Sync Projects & Tasks")
        }

    }
}

@Composable
private fun Loading() {
    Box(modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(20.dp))
    }
}

@Composable
private fun AddTime(addTimeEntries: (LocalDate?) -> Unit, missingEntryDates: List<LocalDate>) {
    var from by remember { mutableStateOf<LocalDate?>(null) }
    Box {
        val dialogState = rememberMaterialDialogState()
        MaterialDialog(
            dialogState = dialogState,
            buttons = {
                positiveButton("Ok")
                negativeButton("Cancel")
            },
            properties = MaterialDialogProperties(
                size = DpSize(300.dp, 500.dp),
                position = DesktopWindowPosition(Alignment.Center)
            )
        ) {
            datepicker { date ->
                from = LocalDate.of(date.year, date.month, date.dayOfMonth)
            }
        }

        Column {
            Text("Add time Toggl 👉 Float", style = MaterialTheme.typography.h4)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = { dialogState.show() }) {
                    Text(from?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "Select date...")
                }
            }

            if (missingEntryDates.isNotEmpty()) {
                Text("Dates with entries in Toggl but not yet in Float. Click to add:")
                FlowRow(modifier = Modifier.fillMaxWidth(), mainAxisSpacing = 16.dp) {
                    missingEntryDates.forEach {
                        OutlinedButton(onClick = { addTimeEntries(it) }) {
                            Text(it.format(DateTimeFormatter.ofPattern("EEE, dd.MM")))
                        }

                    }
                }
            }

            AnimatedVisibility(from != null) {
                Button(onClick = { addTimeEntries(from) }) {
                    Text("Add time entries 🚀")
                }
            }
        }
    }


}

@Composable
private fun Logs(list: List<Pair<String, LogLevel>>) {
    val logs = remember(list) { list.reversed() }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Logs")
        LazyColumn(
            reverseLayout = true,
            modifier = Modifier.heightIn(max = 400.dp).fillMaxWidth(0.8f)
                .border(1.dp, Color.Black, RoundedCornerShape(4.dp)).padding(8.dp)
        ) {
            itemsIndexed(logs) { index, item ->
                if (index > 0) {
                    Divider()
                }
                Text(
                    item.first,
                    color = if (item.second == LogLevel.Error) Color.Red else LocalContentColor.current,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


@Preview
@Composable
fun EmptyPreview() {
    FloaterTheme {
        MainContent(MainState(), {}, {}, {}, { }, {}, { _, _, _ -> }, {})
    }
}

@Preview
@Composable
fun ValidPreview() {
    FloaterTheme {
        MainContent(
            MainState(floatApiKey = "sdljf", togglApiKey = "sdf", peopleId = 123),
            {},
            {},
            {},
            { },
            { },
            { _, _, _ -> },
            {})
    }
}