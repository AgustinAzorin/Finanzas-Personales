package com.agustinazorin.finanzas.feature.household.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.core.ui.components.EmptyState
import com.agustinazorin.finanzas.core.ui.components.LabeledDropdown
import com.agustinazorin.finanzas.core.ui.format.label
import com.agustinazorin.finanzas.engine.model.MemberType
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMember

@Composable
fun HouseholdMembersScreen(viewModel: HouseholdMembersViewModel = hiltViewModel()) {
    val members by viewModel.members.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.household_members_add_title))
            }
        },
    ) { padding ->
        if (members.isEmpty()) {
            EmptyState(stringResource(R.string.household_members_empty), modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(members, key = { it.id }) { member ->
                    MemberRow(member, onToggleActive = { viewModel.setMemberActive(member.id, it) })
                }
            }
        }

        if (showAddDialog) {
            AddMemberDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, type ->
                    viewModel.addMember(name, type)
                    showAddDialog = false
                },
            )
        }
    }
}

@Composable
private fun MemberRow(member: HouseholdMember, onToggleActive: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(member.name, style = MaterialTheme.typography.titleMedium)
                Text(member.type.label(), style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = { onToggleActive(!member.isActive) }) {
                Text(stringResource(if (member.isActive) R.string.household_members_deactivate else R.string.household_members_activate))
            }
        }
    }
}

@Composable
private fun AddMemberDialog(onDismiss: () -> Unit, onConfirm: (name: String, type: MemberType) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MemberType.MEMBER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.household_members_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.household_members_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                LabeledDropdown(
                    label = stringResource(R.string.common_type),
                    options = MemberType.entries,
                    selected = type,
                    optionLabel = { it.label() },
                    onSelected = { type = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, type) }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
