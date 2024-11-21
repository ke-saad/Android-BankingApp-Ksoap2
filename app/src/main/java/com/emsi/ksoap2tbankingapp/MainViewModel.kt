import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emsi.ksoap2tbankingapp.api.KSoapHelper
import com.emsi.ksoap2tbankingapp.state.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    private val _comptes = MutableStateFlow<UiState<List<Compte>>>(UiState.Loading)
    val comptes: StateFlow<UiState<List<Compte>>> = _comptes

    private val _compte = MutableStateFlow<UiState<Compte>>(UiState.Loading)
    val compte: StateFlow<UiState<Compte>> = _compte

    private val kSoapHelper = KSoapHelper()

    fun fetchComptes() {
        viewModelScope.launch(Dispatchers.IO) {
            _comptes.value = UiState.Loading
            try {

                val comptesList = kSoapHelper.getComptes() ?: emptyList()
                Log.d("TAG", "Comptes List: $comptesList")

                _comptes.value = if (comptesList.isEmpty()) {
                    UiState.Error("No accounts found.")
                } else {
                    UiState.Success(comptesList)
                }
            } catch (e: Exception) {
                _comptes.value = UiState.Error("Error: ${e.message}")
            }
        }
    }

    fun fetchCompteById(compteId: Long) {
        viewModelScope.launch {
            _compte.value = UiState.Loading
            try {
                val apiCompte = kSoapHelper.getCompteById(compteId)
                if (apiCompte != null) {
                    val mappedCompte = Compte(
                        id = apiCompte.id,
                        balance = apiCompte.balance,
                        dateCreation = apiCompte.dateCreation,
                        type = apiCompte.type
                    )
                    _compte.value = UiState.Success(mappedCompte)
                } else {
                    _compte.value = UiState.Error("Account not found.")
                }
            } catch (e: Exception) {
                _compte.value = UiState.Error("Error: ${e.message}")
            }
        }
    }


    fun deleteCompte(id: Long) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    kSoapHelper.deleteCompte(id)
                }
                if (response) {
                    _comptes.value = when (val currentState = _comptes.value) {
                        is UiState.Success -> UiState.Success(
                            currentState.data.filter { it.id != id }
                        )

                        else -> currentState
                    }
                } else {
                    _comptes.value = UiState.Error("Failed to delete account.")
                }
            } catch (e: Exception) {
                _comptes.value = UiState.Error("Error: ${e.message}")
            }
        }
    }

    fun createCompte(newCompte: Compte) {
        viewModelScope.launch {
            _comptes.value = UiState.Loading
            try {
                val createdCompte = kSoapHelper.createCompte(newCompte.balance, newCompte.type)

                if (createdCompte != null) {
                    val updatedList = when (val currentState = _comptes.value) {
                        is UiState.Success -> currentState.data + createdCompte
                        else -> listOf(createdCompte)
                    }
                    _comptes.value = UiState.Success(updatedList)
                    fetchComptes()

                } else {
                    _comptes.value =
                        UiState.Error("Failed to create account. Check logs for details.")
                }
            } catch (e: Exception) {
                _comptes.value = UiState.Error("Error: ${e.message}")
                Log.e("MainViewModel", "Error in createCompte", e)
            }
        }
    }

    fun updateCompte(updatedCompte: Compte) {
        viewModelScope.launch {
            _comptes.value = UiState.Loading
            try {
                val response =
                    withContext(Dispatchers.IO) { kSoapHelper.updateCompte(updatedCompte) }
                if (response) {
                    _comptes.value = when (val currentState = _comptes.value) {
                        is UiState.Success -> UiState.Success(
                            currentState.data.map {
                                if (it.id == updatedCompte.id) updatedCompte else it
                            }
                        )

                        else -> currentState
                    }
                } else {
                    _comptes.value = UiState.Error("Failed to update account.")
                }
            } catch (e: Exception) {
                _comptes.value = UiState.Error("Error: ${e.message}")
            }
        }
    }
}
