package fr.xibalba.pronoteKt

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import java.security.MessageDigest
import java.security.SecureRandom

class PronoteKt(private val pronoteUrl: String, private val sessionType: SessionType, private val ent: Ent? = null, private val debug: Boolean = false) {

    private val ktorClient = HttpClient(CIO) {
        install(UserAgent) {
            agent = "Chrome/117.0.0.0"
        }
        install(ContentNegotiation) {
            gson {
                disableHtmlEscaping()
            }
        }
        install(HttpCookies)
        if (debug) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("--------------------------------------------------------------------------")
                        println(message)
                    }
                }
                level = LogLevel.ALL
            }
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
    var entUsername: String? = null
    var entPassword: String? = null

    suspend fun initSession(): Boolean {
        if (identifiantNav.isNotEmpty()) return true
        val uuid1 = getUuid()
        val response = doRequest("FonctionParametres", mapOf("Uuid" to uuid1.first))
        identifiantNav = response?.get("identifiantNav")?.asString ?: return false.also { println("Error while initializing session") }
        iv = uuid1.second
        return true
    }

    suspend fun initEnt(url: String, username: String, password: String): Boolean {
        if (ktorClient.cookies(pronoteUrl).isNotEmpty()) return true
        val entUrlResponse = ktorClient.get(url)
        val entUrlBody: String = entUrlResponse.body()
        val samlRequest = Regex("name=\"SAMLRequest\" value=\"([^\"]+)\"").find(entUrlBody)?.groupValues?.get(1)
        val relayState = Regex("name=\"RelayState\" value=\"([^\"]+)\"").find(entUrlBody)?.groupValues?.get(1)
        val loginUrl = Regex("action=\"([^\"]+)\"").find(entUrlBody)?.groupValues?.get(1)?.replace("&#x3a;", ":")?.replace("&#x2f;", "/")
        val response2 = ktorClient.post(loginUrl!!) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                FormDataContent(Parameters.build {
                    append("SAMLRequest", samlRequest!!)
                    append("RelayState", relayState!!)
                })
            )
        }
        val response3 = ktorClient.post(response2.headers["Location"]!!) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(FormDataContent(Parameters.build {
                append("j_username", username)
                append("j_password", password)
                append("_eventId_proceed", "")
            }))
        }
        val body3: String = response3.body()
        val samlResponse = Regex("name=\"SAMLResponse\" value=\"([^\"]+)\"").find(body3)?.groupValues?.get(1)
        val relayStateResponse = Regex("name=\"RelayState\" value=\"([^\"]+)\"").find(body3)?.groupValues?.get(1)
        val url4 = Regex("action=\"([^\"]+)\"").find(body3)?.groupValues?.get(1)?.replace("&#x3a;", ":")?.replace("&#x2f;", "/")
        ktorClient.post(url4!!) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(FormDataContent(Parameters.build {
                append("SAMLResponse", samlResponse!!)
                append("RelayState", relayStateResponse!!)
            }))
        }.body<String>()
        return true
    }

    suspend fun login(username: String, password: String): Boolean {
        if (isLogged()) return true
        return if (ent?.complexMethod != null) {
            ent.complexMethod.invoke(this)
        } else {
            normalLogin(username, password)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun normalLogin(username: String, password: String): Boolean {
        //ENT
        if (ent?.params?.url != null) initEnt(ent.params.url, username, password)
        //INIT
        if (!initSession()) return false
        //STEP 1
        val identificationResponse = doRequest(
            "Identification", data = mapOf(
                "genreConnexion" to 0,
                "genreEspace" to sessionType.id,
                "identifiant" to if (ent != null) entUsername!! else username,
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
        val usernameToUse = if (ent != null) entUsername!! else if (identificationResponse.get("modeCompLog")?.asInt == 1) username.lowercase() else username
        val passwordToUse = if (ent != null) entPassword!! else if (identificationResponse.get("modeCompMdp")?.asInt == 1) password.lowercase() else password

        //STEP 2
        val mtp = MessageDigest.getInstance("SHA-256").digest((randomString + passwordToUse).toByteArray()).toHexString().uppercase()
        val key = if (ent != null) MessageDigest.getInstance("SHA-256").digest(passwordToUse.toByteArray()).toHexString().uppercase()
            .toByteArray() else (usernameToUse + mtp).toByteArray()
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
            .map { it.toInt().toByte() }
            .toByteArray()
        return authResponse.get("libelleUtil")?.asString != null
    }

    fun isLogged(): Boolean {
        return key.isNotEmpty()
    }

    suspend fun doRequest(name: String, data: Map<String, Any> = emptyMap(), page: Int = -1): JsonObject? {
        val sessionId = getSessionId()
        val numeroOrdre = getNumeroOrdre()
        val body = createJsonForRequest(name, sessionId, numeroOrdre, data, page)
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
            if (ent != null) {
                entUsername = Regex("e:'([^']+)'").find(body)?.groupValues?.get(1)
                entPassword = Regex("f:'([^']+)'").find(body)?.groupValues?.get(1)
            }
            return sessionId!!
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getNumeroOrdre(): String {
        val numeroOrdre = aesEncrypt(requestCounter.toString().toByteArray()).toHexString()
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

    fun createJsonForRequest(name: String, sessionId: Int, numeroOrdre: String, otherData: Map<String, Any> = emptyMap(), page: Int = -1): JsonObject {
        val builder = StringBuilder()
        builder.append("{")
        builder.append("\"nom\":\"$name\",")
        builder.append("\"session\":$sessionId,")
        builder.append("\"numeroOrdre\":\"$numeroOrdre\",")
        builder.append("\"donneesSec\": {")
        if (otherData.isNotEmpty()) {
            builder.append("\"donnees\": {")
            otherData.forEach { (key, value) ->
                if (value is String) {
                    builder.append("\"$key\":\"$value\",")
                } else {
                    builder.append("\"$key\":$value,")
                }
            }
            builder.deleteCharAt(builder.length - 1)
            builder.append("},")
        }
        if (page != -1) {
            builder.append("\"_Signature_\":{")
            builder.append("\"onglet\":$page")
            builder.append("}")
        } else if (otherData.isNotEmpty()) {
            builder.deleteCharAt(builder.length - 1)
        }
        builder.append("}")
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

    suspend fun getUserData(): JsonObject? = requireLogin {
        if (userData != null) userData
        val response = doRequest("ParametresUtilisateur")
        userData = response
        userData
    }

    suspend fun <T> requireLogin(function: suspend PronoteKt.() -> T): T {
        if (!isLogged()) throw IllegalStateException("Must be logged")
        return function()
    }
}

enum class SessionType(val url: String, val jsUrl: String, val id: Int) {
    STUDENT("/eleve.html", "/E_3_C_45ECDEFA6864C78D6AD2329314EC027A1AB045043D8F4F93A09EF7DFE131B197_L_1036/eleve.js", 3)
}

enum class Ent(val params: NormalEntParams? = null, val complexMethod: (PronoteKt.() -> Boolean)? = null) {
    AUVERGNE_RHONE_ALPES(NormalEntParams("https://cas.ent.auvergnerhonealpes.fr/login?selection=EDU&service=https://example.com/", NormalEntType.CAS_EDU))
}

data class NormalEntParams(val url: String, val type: NormalEntType, val redirectForm: Boolean = true)

enum class NormalEntType {
    CAS_EDU
}