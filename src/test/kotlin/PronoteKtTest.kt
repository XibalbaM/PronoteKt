import fr.xibalba.pronoteKt.PronoteKt
import fr.xibalba.pronoteKt.SessionType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class PronoteKtTest {

    private val pronoteKt = PronoteKt("https://demo.index-education.net/pronote", SessionType.STUDENT)

    @Test
    fun testGetSessionId() = runTest {
        assertNotEquals(-1, pronoteKt.getSessionId())
    }

    @Test
    fun testCreateJsonForRequest() {
        val json = pronoteKt.createJsonForRequest("test.log", 1, "1")
        assertEquals("{\"nom\":\"test.log\",\"session\":1,\"numeroOrdre\":\"1\"}", json.toString())
        val json2 = pronoteKt.createJsonForRequest("test.log", 1, "1", mapOf("test.log" to "test.log"))
        assertEquals("{\"nom\":\"test.log\",\"session\":1,\"numeroOrdre\":\"1\",\"donneesSec\":{\"donnees\":{\"test.log\":\"test.log\"}}}", json2.toString())
    }

    @Test
    fun testRsaKeys() = runTest {
        assertNotNull(pronoteKt.getRsaModulo())
        assertNotNull(pronoteKt.getRsaExponent())
    }

    @Test
    fun testGetUuid() {
        assertNotNull(pronoteKt.getUuid())
        println("UUID: ${pronoteKt.getUuid()}")
        assertEquals("qwwii4iQPq20HEmy53Dp2A==".length, pronoteKt.getUuid().length)
    }
//TODO: Le probleme est que le serveur ne recoit pas le bon IV
    @Test
    fun testLogin() = runTest {
        assert(pronoteKt.login("demonstration", "pronotevs"))
    }
}