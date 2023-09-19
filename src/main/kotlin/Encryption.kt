package fr.xibalba.pronoteKt

import sun.security.rsa.RSAPublicKeyImpl
import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

const val ALGORITHM = "AES/CBC/PKCS5Padding"
@OptIn(ExperimentalStdlibApi::class)
val keyData = md5("").hexToByteArray()

fun aesEncrypt(str: String, iv: ByteArray = ByteArray(16)): ByteArray {
    val keySpec = SecretKeySpec(keyData, "AES")
    val ivSpec = IvParameterSpec(iv)

    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

    return cipher.doFinal(str.toByteArray())
}
fun PronoteKt.aesEncrypt(str: String) = aesEncrypt(str, iv)

fun md5(str: String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(str.toByteArray())).toString(16).padStart(32, '0')
}

fun base64Encode(str: ByteArray): String {
    return Base64.getEncoder().encodeToString(str)
}

fun rsaEncrypt(str: ByteArray, modulo: String, exponent: Int): ByteArray {
    val key = RSAPublicKeySpec(BigInteger(modulo, 16), BigInteger.valueOf(exponent.toLong()))
    val kf = KeyFactory.getInstance("RSA")
    val publicKey = kf.generatePublic(key)
    val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    cipher.init(Cipher.ENCRYPT_MODE, publicKey)
    return cipher.doFinal(str)
}
suspend fun PronoteKt.rsaEncrypt(str: ByteArray) = rsaEncrypt(str, getRsaModulo(), getRsaExponent())