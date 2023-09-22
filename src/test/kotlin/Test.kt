import fr.xibalba.pronoteKt.aesDecrypt
import fr.xibalba.pronoteKt.aesEncrypt
import fr.xibalba.pronoteKt.md5
import org.junit.jupiter.api.Test
import java.security.MessageDigest

class Test {

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun test() {
        val usernameToUse = "demonstration"
        val passwordToUse = "pronotevs"
        val randomString = "{6AD5E32E-D36B-C1E1-EFF0-930984332A6E}"
        val challenge = "C8847CEE1CDC57C96C88EC4CDBB04001FB823B6927D3083A7692BBFDEFE5794FECDD0FABE21B515FFA38231C8A1394FA"
        val iv = md5("13be021c2bc2bebc78b1b505ca0d9c66".hexToByteArray())
        val mtp = MessageDigest.getInstance("SHA-256").digest((randomString + passwordToUse).toByteArray()).toHexString().uppercase() //WORKS
        println("MTP: $mtp")
        val key = (usernameToUse + mtp).toByteArray() //WORKS
        println("Key: ${key.toHexString()}")
        val decryptedChallenge = aesDecrypt(challenge.hexToByteArray(), key, iv).decodeToString() //WORKS
        println("Decrypted challenge: $decryptedChallenge")
        val modifiedChallenge = decryptedChallenge.filterIndexed { index, _ -> index % 2 == 0 } //WORKS
        println("Modified challenge: $modifiedChallenge")
        val byteChallenge = modifiedChallenge.toByteArray()
        println("Byte challenge: ${byteChallenge.toHexString()}")
        val solvedChallenge = aesEncrypt(byteChallenge, key, iv).toHexString() //WORKS
        println("Solved challenge: $solvedChallenge")
    }
}