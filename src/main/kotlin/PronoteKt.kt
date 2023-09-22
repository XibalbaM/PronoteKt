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
import java.security.MessageDigest
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
    private var userData: JsonObject? = null
    var identifiantNav: String = ""
    var key = "".toByteArray()
    var iv = ByteArray(16)

    suspend fun initSession(): Boolean {
        if (identifiantNav.isNotEmpty()) return true
        val uuid1 = getUuid()
        val response = doRequest("FonctionParametres", mapOf("Uuid" to uuid1.first))
        identifiantNav = response?.get("identifiantNav")?.asString ?: return false.also { println("Error while initializing session") }
        iv = uuid1.second
        return true
    }

    @OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
    suspend fun login(username: String, password: String): Boolean {
        if (isLogged()) return true
        //INIT
        if (!initSession()) return false

        //STEP 1
        val identificationResponse = doRequest(
            "Identification", data = mapOf(
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
        ) ?: return false.also { println("An error occurred during first login request") }
        val challenge = identificationResponse.get("challenge")?.asString ?: return false.also { println("An error occurred during first login request") }
        val randomString = identificationResponse.get("alea")?.asString ?: ""
        val usernameToUse = if (identificationResponse.get("modeCompLog")?.asInt == 1) username.lowercase() else username
        val passwordToUse = if (identificationResponse.get("modeCompMdp")?.asInt == 1) password.lowercase() else password

        //STEP 2
        val mtp = MessageDigest.getInstance("SHA-256").digest((randomString + passwordToUse).toByteArray()).toHexString().uppercase()
        val key = (usernameToUse + mtp).toByteArray()
        val decryptedChallenge = aesDecrypt(challenge.hexToByteArray(), key).decodeToString()
        val modifiedChallenge = decryptedChallenge.filterIndexed { index, _ -> index % 2 == 0 }
        val byteChallenge = modifiedChallenge.toByteArray()
        val solvedChallenge = aesEncrypt(byteChallenge, key).toHexString()

        //STEP 3
        val authResponse = doRequest(
            "Authentification", data = mapOf(
                "connexion" to 0,
                "challenge" to solvedChallenge,
                "espace" to sessionType.id
            )
        ) ?: return false.also { println("An error occurred during second login request") }
        val newKeyEncrypted = authResponse.get("cle")?.asString ?: return false.also { println("An error occurred during second login request") }
        this.key = aesDecrypt(newKeyEncrypted.hexToByteArray(), key)
            .decodeToString()
            .split(",")
            .map { it.toUByte() }
            .toUByteArray().toByteArray() //TODO: Not working
        return authResponse.get("libelleUtil")?.asString != null
    }

    fun isLogged(): Boolean {
        return key.isNotEmpty()
    }

    private suspend fun doRequest(name: String, data: Map<String, Any> = emptyMap()): JsonObject? {
        val sessionId = getSessionId()
        val numeroOrdre = getNumeroOrdre()
        val body = createJsonForRequest(name, sessionId, numeroOrdre, data)
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
        println("IV: ${iv.toHexString()}")
        val numeroOrdre = aesEncrypt(requestCounter.toString().toByteArray()).toHexString()
        println("Numero ordre: $numeroOrdre")
        requestCounter += 2
        return numeroOrdre
    }

    fun getUuid(): Pair<String, ByteArray> {
        if (uuid != null) return uuid!! to iv
        val random: SecureRandom = SecureRandom.getInstanceStrong()
        val newIv = ByteArray(16)
        random.nextBytes(newIv)
        uuid = base64Encode(newIv)
        return uuid!! to md5(newIv)
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

    suspend fun getUserData(): JsonObject? {
        if (!isLogged()) return null.also { println("Must be logged") }
        if (userData != null) return userData
        val response = doRequest("ParametresUtilisateur")
        userData = response
        println(userData)
        return userData
    }
}

enum class SessionType(val url: String, val jsUrl: String, val id: Int) {
    STUDENT("/eleve.html", "/E_3_C_45ECDEFA6864C78D6AD2329314EC027A1AB045043D8F4F93A09EF7DFE131B197_L_1036/eleve.js", 3)
}