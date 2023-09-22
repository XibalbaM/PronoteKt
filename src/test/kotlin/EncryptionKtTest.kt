import fr.xibalba.pronoteKt.aesEncrypt
import fr.xibalba.pronoteKt.base64Encode
import fr.xibalba.pronoteKt.md5
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EncryptionKtTest {

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testAesEncrypt() {
        assertEquals("3fa959b13967e0ef176069e01e23c8d7", aesEncrypt("1".toByteArray(), "".toByteArray(), ByteArray(16)).toHexString())
    }

    @Test
    fun testMd5() {
        assertEquals("098f6bcd4621d373cade4e832627b4f6", md5("test".toByteArray()))
    }

    @Test
    fun testBase64Encode() {
        assertEquals("dGVzdA==", base64Encode("test".toByteArray()))
    }
}