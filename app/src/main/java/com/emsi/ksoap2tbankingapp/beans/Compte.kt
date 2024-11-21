import java.util.Date

data class Compte(
    val id: Long,

    val balance: Double,

    val dateCreation: Date,

    val type: TypeCompte
)