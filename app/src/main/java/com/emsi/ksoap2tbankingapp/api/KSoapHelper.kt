package com.emsi.ksoap2tbankingapp.api

import Compte
import TypeCompte
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ksoap2.SoapEnvelope
import org.ksoap2.SoapFault
import org.ksoap2.serialization.MarshalFloat
import org.ksoap2.serialization.SoapObject
import org.ksoap2.serialization.SoapSerializationEnvelope
import org.ksoap2.transport.HttpTransportSE
import java.lang.reflect.Field
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Vector

class KSoapHelper {
    private val NAMESPACE = "http://ws.spring.rest.ma/"
    private val URL = "http://10.0.2.2:8082/services/comptews?wsdl"
    private val transport = HttpTransportSE(URL)

    private fun logResponseCode() {
        try {
            val connectionField: Field = HttpTransportSE::class.java.getDeclaredField("connection")
            connectionField.isAccessible = true
            val connection = connectionField.get(transport) as? HttpURLConnection

            connection?.let {
                val responseCode = it.responseCode
                val responseMessage = it.responseMessage
                Log.d("HTTP Response", "Code: $responseCode, Message: $responseMessage")
            }
        } catch (e: Exception) {
            Log.e("HTTP Error", "Failed to log response code: ${e.message}", e)
        }
    }

    private fun parseSoapCompte(soapCompte: SoapObject): Compte {
        val id = soapCompte.getProperty("id").toString().toLong()
        val solde = soapCompte.getProperty("solde").toString().toDouble()
        val dateCreationString = soapCompte.getProperty("dateCreation").toString()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
        val dateCreation = dateFormat.parse(dateCreationString) ?: Date()
        val type = TypeCompte.valueOf(soapCompte.getProperty("type").toString())

        return Compte(id, solde, dateCreation, type)
    }

    fun getComptes(): List<Compte>? {
        val method = "getComptes"
        val soapObject = SoapObject(NAMESPACE, method)
        val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11)
        envelope.setOutputSoapObject(soapObject)

        return try {
            transport.call("", envelope)
            logResponseCode()
            val result = envelope.response
            val comptes = mutableListOf<Compte>()
            Log.d("La liste des comptes", comptes.toString())

            if (result is Vector<*>) {
                for (item in result) {
                    if (item is SoapObject) {
                        comptes.add(parseSoapCompte(item))
                    }
                }
            }

            comptes
        } catch (e: Exception) {
            Log.e("SOAP Error", "Error during SOAP call: ${e.message}", e)
            null
        }
    }

    suspend fun getCompteById(compteId: Long): Compte? = withContext(Dispatchers.IO) {
        val method = "getCompteById"
        val soapObject = SoapObject(NAMESPACE, method).apply {
            addProperty("id", compteId)
        }
        val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11)
        envelope.setOutputSoapObject(soapObject)

        try {
            transport.call("", envelope)
            logResponseCode()

            val result = envelope.response
            Log.d("KSoapHelper", "Raw Response getCompteById: ${result.toString()}")

            if (result is SoapObject) {
                return@withContext parseSoapCompte(result)
            } else {
                Log.e("KSoapHelper", "Unexpected response type in getCompteById: ${result?.javaClass?.name}")
                return@withContext null
            }
        } catch (e: Exception) {
            if (e is SoapFault) {
                Log.e("KSoapHelper", "SoapFault in getCompteById: ${e.faultstring}", e)
            } else {
                Log.e("KSoapHelper", "Error in getCompteById: ${e.message}", e)
            }
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun createCompte(solde: Double, type: TypeCompte): Compte? =
        withContext(Dispatchers.IO) {
            val method = "createCompte"
            val soapAction = ""
            val soapObject = SoapObject(NAMESPACE, method).apply {
                addProperty("solde", solde)
                addProperty("type", type.name)
            }
            val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11)
            envelope.setOutputSoapObject(soapObject)
            val marshal = MarshalFloat()
            marshal.register(envelope)

            try {
                transport.call(soapAction, envelope)
                logResponseCode()

                val result = envelope.response
                Log.d("KSoapHelper", "Raw Response: ${result.toString()}")

                if (result is SoapObject) {
                    if (result.hasProperty("return")) {
                        val returnProperty = result.getProperty("return")
                        if (returnProperty is SoapObject) {

                            return@withContext parseSoapCompte(returnProperty)
                        } else if (returnProperty != null) {
                            Log.d(
                                "KSoapHelper",
                                "Return Property Type: ${returnProperty.javaClass.name}"
                            )

                        } else {
                            Log.e("KSoapHelper", "Return property is null")
                        }
                    } else {
                        Log.e("KSoapHelper", "Missing 'return' property in response")
                    }
                } else {
                    Log.e("KSoapHelper", "Unexpected response type: ${result?.javaClass?.name}")
                }

            } catch (e: Exception) {
                Log.e("KSoapHelper", "Error creating account: ${e.message}", e)
            }

            return@withContext null
        }

    fun deleteCompte(compteId: Long): Boolean {
        val method = "deleteCompte"
        val soapAction = ""
        val soapObject = SoapObject(NAMESPACE, method).apply {
            addProperty("id", compteId)
        }
        val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11)
        envelope.setOutputSoapObject(soapObject)
        val transport = HttpTransportSE(URL)

        return try {
            transport.call(soapAction, envelope)
            envelope.response.toString().toBoolean()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateCompte(compte: Compte): Boolean {
        val method = "updateCompte"
        val soapAction = ""
        val soapObject = SoapObject(NAMESPACE, method).apply {
            addProperty("id", compte.id)
            addProperty("solde", compte.balance)
            addProperty("type", compte.type.name)
        }
        val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11)
        envelope.setOutputSoapObject(soapObject)
        val marshal = MarshalFloat()
        marshal.register(envelope)

        return try {
            transport.call(soapAction, envelope)
            logResponseCode()

            val result = envelope.response
            Log.d("KSoapHelper", "Raw Response: ${result.toString()}")

            if (result is SoapObject) {
                Log.d("KSoapHelper", "Updated Compte: ${result.toString()}")
                return true
            } else if (result != null) {
                Log.d("KSoapHelper", "Update Result (not SoapObject): ${result.toString()}")

                return result.toString().toBoolean()
            } else {
                Log.e("KSoapHelper", "Null response from updateCompte")
                return false
            }

        } catch (e: Exception) {
            Log.e("KSoapHelper", "Error updating account: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

}
