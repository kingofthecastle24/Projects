package nz.co.ridling.healthproof.ui.foodlog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import nz.co.ridling.healthproof.domain.ComplianceStatus
import nz.co.ridling.healthproof.domain.DailyComplianceStatus
import nz.co.ridling.healthproof.domain.MealCategory
import nz.co.ridling.healthproof.domain.MealEntry
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun dateFormatter(): DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLogScreen(
    state: FoodLogUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onAddEntry: (MealCategory) -> Unit,
    onEditEntry: (MealEntry) -> Unit,
    onDeleteEntry: (MealEntry) -> Unit,
    onSaveEntry: (MealEntry) -> Unit,
    onCancelEdit: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Food log") }) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxWidth().padding(padding)) {
            DateNavigationRow(state.selectedDate.format(dateFormatter()), onPreviousDay, onNextDay, onToday)

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { DailyTotalsCard(state) }

                for (category in MealCategory.entries) {
                    item {
                        CategorySection(
                            category = category,
                            entries = state.entries.filter { it.category == category },
                            onAddEntry = { onAddEntry(category) },
                            onEditEntry = onEditEntry,
                            onDeleteEntry = onDeleteEntry,
                        )
                    }
                }
            }
        }
    }

    val editing = state.editingEntry
    if (editing != null) {
        MealEntryEditorDialog(entry = editing, onSave = onSaveEntry, onCancel = onCancelEdit)
    }
}

@Composable
private fun DateNavigationRow(dateLabel: String, onPreviousDay: () -> Unit, onNextDay: () -> Unit, onToday: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPreviousDay) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous day") }
        TextButton(onClick = onToday) { Text(dateLabel, fontWeight = FontWeight.Medium) }
        IconButton(onClick = onNextDay) { Icon(Icons.Filled.ChevronRight, contentDescription = "Next day") }
    }
}

@Composable
private fun DailyTotalsCard(state: FoodLogUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Daily totals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("${state.dailyTotals.calories} kcal", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Protein ${state.dailyTotals.proteinGrams.formatGrams()} · " +
                    "Carbs ${state.dailyTotals.carbsGrams.formatGrams()} · " +
                    "Fat ${state.dailyTotals.fatGrams.formatGrams()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            val (label, color) = when (state.complianceStatus) {
                DailyComplianceStatus.NO_ENTRIES -> state.complianceStatus.displayName to MaterialTheme.colorScheme.onSurfaceVariant
                DailyComplianceStatus.COMPLIANT -> state.complianceStatus.displayName to MaterialTheme.colorScheme.primary
                DailyComplianceStatus.NOT_COMPLIANT -> state.complianceStatus.displayName to MaterialTheme.colorScheme.error
                DailyComplianceStatus.CHEAT_DAY -> state.complianceStatus.displayName to MaterialTheme.colorScheme.tertiary
            }
            SuggestionChip(
                onClick = {},
                enabled = false,
                label = { Text(label) },
                colors = SuggestionChipDefaults.suggestionChipColors(disabledLabelColor = color),
            )
        }
    }
}

@Composable
private fun CategorySection(
    category: MealCategory,
    entries: List<MealEntry>,
    onAddEntry: () -> Unit,
    onEditEntry: (MealEntry) -> Unit,
    onDeleteEntry: (MealEntry) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(category.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onAddEntry) { Icon(Icons.Filled.Add, contentDescription = "Add ${category.displayName} entry") }
            }
            if (entries.isEmpty()) {
                Text("No entries yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                for (entry in entries) {
                    MealEntryRow(entry, onEdit = { onEditEntry(entry) }, onDelete = { onDeleteEntry(entry) })
                }
            }
        }
    }
}

@Composable
private fun MealEntryRow(entry: MealEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name.ifBlank { "(unnamed)" }, fontWeight = FontWeight.Medium)
            Text(
                "${entry.calories} kcal · P ${entry.proteinGrams.formatGrams()} · " +
                    "C ${entry.carbsGrams.formatGrams()} · F ${entry.fatGrams.formatGrams()} · ${entry.compliance.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
    }
}

private fun Double.formatGrams(): String = String.format(Locale.getDefault(), "%.0fg", this)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealEntryEditorDialog(entry: MealEntry, onSave: (MealEntry) -> Unit, onCancel: () -> Unit) {
    var name by remember(entry.id) { mutableStateOf(entry.name) }
    var category by remember(entry.id) { mutableStateOf(entry.category) }
    var calories by remember(entry.id) { mutableStateOf(if (entry.calories == 0) "" else entry.calories.toString()) }
    var protein by remember(entry.id) { mutableStateOf(if (entry.proteinGrams == 0.0) "" else entry.proteinGrams.toString()) }
    var carbs by remember(entry.id) { mutableStateOf(if (entry.carbsGrams == 0.0) "" else entry.carbsGrams.toString()) }
    var fat by remember(entry.id) { mutableStateOf(if (entry.fatGrams == 0.0) "" else entry.fatGrams.toString()) }
    var compliance by remember(entry.id) { mutableStateOf(entry.compliance) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var complianceMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (entry.id == 0L) "Add entry" else "Edit entry") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(expanded = categoryMenuExpanded, onExpandedChange = { categoryMenuExpanded = it }) {
                    OutlinedTextField(
                        value = category.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                    )
                    ExposedDropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                        for (option in MealCategory.entries) {
                            DropdownMenuItem(text = { Text(option.displayName) }, onClick = { category = option; categoryMenuExpanded = false })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { c -> c.isDigit() } },
                    label = { Text("Calories") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text("Fat (g)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(expanded = complianceMenuExpanded, onExpandedChange = { complianceMenuExpanded = it }) {
                    OutlinedTextField(
                        value = compliance.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Slow-carb compliance") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = complianceMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                    )
                    ExposedDropdownMenu(expanded = complianceMenuExpanded, onDismissRequest = { complianceMenuExpanded = false }) {
                        for (option in ComplianceStatus.entries) {
                            DropdownMenuItem(text = { Text(option.displayName) }, onClick = { compliance = option; complianceMenuExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    entry.copy(
                        name = name,
                        category = category,
                        calories = calories.toIntOrNull() ?: 0,
                        proteinGrams = protein.toDoubleOrNull() ?: 0.0,
                        carbsGrams = carbs.toDoubleOrNull() ?: 0.0,
                        fatGrams = fat.toDoubleOrNull() ?: 0.0,
                        compliance = compliance,
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
    )
}
