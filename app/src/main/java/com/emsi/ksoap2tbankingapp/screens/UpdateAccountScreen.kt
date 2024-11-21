package com.emsi.ksoap2tbankingapp.screens

import Compte
import MainViewModel
import TypeCompte
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emsi.ksoap2tbankingapp.state.UiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UpdateAccountScreen(viewModel: MainViewModel, compteId: Long) {
    var balance by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf(TypeCompte.COURANT) }
    var compte by remember { mutableStateOf<Compte?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(compteId) {
        viewModel.fetchCompteById(compteId)
    }

    val compteState by viewModel.compte.collectAsState()

    LaunchedEffect(compteState) {
        val currentState = compteState
        if (currentState is UiState.Success<*>) {
            val successState = currentState
            val fetchedCompte = successState.data as Compte?
            compte = fetchedCompte
            balance = fetchedCompte?.balance?.toString() ?: ""
            accountType = fetchedCompte?.type ?: TypeCompte.COURANT
        } else if (currentState is UiState.Error) {
            showSnackbar = true
            snackbarHostState.showSnackbar(currentState.message)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFEBEE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(Color(0xFFFFEBEE)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Editer le compte", color = Color.Red, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = balance,
                onValueChange = { newBalance ->
                    val filteredBalance = newBalance.filter { it.isDigit() || it == '.' }
                    val doubleValue = filteredBalance.toDoubleOrNull()

                    if (doubleValue != null) {
                        balance = "%.2f".format(doubleValue)
                    } else {
                        balance = filteredBalance
                    }

                },
                label = { Text("Solde") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().wrapContentHeight(Alignment.CenterVertically),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White, focusedIndicatorColor = Color.Red
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (compte != null ) {
                val formattedDate =
                    compte?.dateCreation?.let {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
                            it
                        )
                    }
                Text("Créé en : ${formattedDate}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            UpdateAccountTypeDropdown(accountType) { selectedType -> accountType = selectedType }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val balanceAmount = balance.trim().toDoubleOrNull()
                    if (balanceAmount == null) {
                        scope.launch { snackbarHostState.showSnackbar("Valeur du solde invalide") }
                        return@Button
                    }
                    val updatedCompte = compte?.copy(
                        balance = balanceAmount,
                        dateCreation = compte!!.dateCreation,
                        type = accountType
                    )
                    if (updatedCompte != null) {
                        viewModel.updateCompte(updatedCompte)
                        scope.launch { snackbarHostState.showSnackbar("Compte mis à jour avec succès") }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(8.dp).background(Color(0xFFD32F2F)),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text("Mettre à jour le compte", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            SnackbarHost(hostState = snackbarHostState)

        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAccountTypeDropdown(accountType: TypeCompte, onAccountTypeChange: (TypeCompte) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = accountType.name,
            onValueChange = {},
            label = { Text("Type") },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White, focusedIndicatorColor = Color.Red
            )
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("COURANT") },
                onClick = { onAccountTypeChange(TypeCompte.COURANT); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("EPARGNE") },
                onClick = { onAccountTypeChange(TypeCompte.EPARGNE); expanded = false }
            )
        }
    }
}