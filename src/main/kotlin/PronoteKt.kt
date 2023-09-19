package fr.xibalba.pronoteKt

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import java.security.SecureRandom


class PronoteKt(private val pronoteUrl: String, private val sessionType: SessionType) {

    private val ktorClient = HttpClient(CIO) {
        install(UserAgent) {
            agent = "Chrome/117.0.0.0"
        }
        install(ContentNegotiation) {
            gson {
                disableHtmlEscaping()
            }
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("--------------------------------------------------------------------------")
                    println(message)
                }
            }
            level = LogLevel.ALL
            filter { it.method != HttpMethod.Get }
        }
    }
    private var requestCounter = 1
    private var sessionId: Int? = null
    private var uuid: String? = null
    private var rsaModulo: String? = null
    private var rsaExponent: Int? = null
    var identifiantNav: String = ""
    @OptIn(ExperimentalStdlibApi::class)
    var iv = ByteArray(16)

    suspend fun initSession() {
        if (identifiantNav.isNotEmpty()) return
        val response = doRequest("FonctionParametres")
        identifiantNav = response?.get("identifiantNav")?.asString ?: throw Exception("Error while initializing session")
    }

    suspend fun login(username: String, password: String): Boolean {
        initSession()
        val identificationResponse = doRequest(
            "Identification", includeDefaultData = false, data = mapOf(
                "genreConnexion" to 0,
                "genreEspace" to sessionType.id,
                "identifiant" to username,
                "pourENT" to false,
                "enConnexionAuto" to false,
                "demandeConnexionAuto" to false,
                "demandeConnexionAppliMobile" to false,
                "demandeConnexionAppliMobileJeton" to false,
                "uuidAppliMobile" to "",
                "loginTokenSAV" to "",
            )
        )
        println(identificationResponse)
        return identificationResponse != null
    }

    private suspend fun doRequest(name: String, data: Map<String, Any> = emptyMap(), includeDefaultData: Boolean = true): JsonObject? {
        val sessionId = getSessionId()
        val numeroOrdre = getNumeroOrdre()
        val uuid = getUuid()
        val dataToSend = if (includeDefaultData) data.plus(mapOf("Uuid" to uuid, "identifiantNav" to identifiantNav)) else data
        val body = createJsonForRequest(name, sessionId, numeroOrdre, dataToSend)
        val response = ktorClient.post("$pronoteUrl/appelfonction/${sessionType.id}/$sessionId/$numeroOrdre") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val responseBody: String = response.body()
        return JsonParser.parseString(responseBody)?.asJsonObject?.getAsJsonObject("donneesSec")?.getAsJsonObject("donnees")
    }

    suspend fun getSessionId(): Int {
        try {
            if (sessionId != null) return sessionId!!
            val sessionIdResponse = ktorClient.get("$pronoteUrl${sessionType.url}")
            val body: String = sessionIdResponse.body()
            sessionId = Regex("h:'(\\d+)'").find(body)?.groupValues?.get(1)?.toInt()
            return sessionId!!
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getNumeroOrdre(): String {
        println("Request counter: $requestCounter")
        val numeroOrdre = aesEncrypt(requestCounter.toString()).toHexString().uppercase()
        println("Numero ordre: $numeroOrdre")
        requestCounter += 2
        return numeroOrdre
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun getUuid(): String {
        if (uuid != null) return uuid!!
        val random: SecureRandom = SecureRandom.getInstanceStrong()
        iv = ByteArray(16)
        random.nextBytes(iv)
        println("IV: ${iv.toHexString()}")
        uuid = base64Encode(iv)
        return uuid!!
    }

    fun createJsonForRequest(name: String, sessionId: Int, numeroOrdre: String, otherData: Map<String, Any> = emptyMap()): JsonObject {
        val builder = StringBuilder()
        builder.append("{")
        builder.append("\"nom\":\"$name\",")
        builder.append("\"session\":$sessionId,")
        builder.append("\"numeroOrdre\":\"$numeroOrdre\",")
        if (otherData.isNotEmpty()) {
            builder.append("\"donneesSec\": {")
            builder.append("\"donnees\": {")
            otherData.forEach { (key, value) ->
                if (value is String) {
                    builder.append("\"$key\":\"$value\",")
                } else {
                    builder.append("\"$key\":$value,")
                }
            }
            builder.deleteCharAt(builder.length - 1)
            builder.append("}")
            builder.append("}")
        } else {
            builder.deleteCharAt(builder.length - 1)
        }
        builder.append("}")
        return JsonParser.parseString(builder.toString()).asJsonObject
    }

    suspend fun getRsaModulo(): String {
        if (rsaModulo != null) return rsaModulo!!
        getRsaKeys()
        return rsaModulo!!
    }

    suspend fun getRsaExponent(): Int {
        if (rsaExponent != null) return rsaExponent!!
        getRsaKeys()
        return rsaExponent!!
    }

    private suspend fun getRsaKeys() {
        try {
            val sessionIdResponse = ktorClient.get("$pronoteUrl${sessionType.jsUrl}")
            val body: String = sessionIdResponse.body()
            rsaModulo = Regex("const c_rsaPub_modulo_1024='([A-F0-9]+)';").find(body)?.groupValues?.get(1)
            val rsaExponentString = Regex("const c_rsaPub_exposant_1024='([01]+)';").find(body)?.groupValues?.get(1)
            if (rsaExponentString != null) {
                rsaExponent = Integer.decode("0x$rsaExponentString")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

enum class SessionType(val url: String, val jsUrl: String, val id: Int) {
    STUDENT("/eleve.html", "/E_3_C_45ECDEFA6864C78D6AD2329314EC027A1AB045043D8F4F93A09EF7DFE131B197_L_1036/eleve.js", 3)
}