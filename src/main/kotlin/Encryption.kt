package fr.xibalba.pronoteKt

import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

const val ALGORITHM = "AES/CBC/PKCS5Padding"

fun aesEncrypt(str: String, iv: ByteArray = ByteArray(16), key: String = ""): ByteArray {
    val keySpec = SecretKeySpec(md5(key.toByteArray()), "AES")
    val ivSpec = IvParameterSpec(iv)

    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

    return cipher.doFinal(str.toByteArray())
}
fun PronoteKt.aesEncrypt(str: String) = aesEncrypt(str, iv)

fun aesDecrypt(str: ByteArray, key: String, iv: ByteArray): ByteArray {
    val keySpec = SecretKeySpec(md5(key.toByteArray()), "AES")
    val ivSpec = IvParameterSpec(iv)

    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

    return cipher.doFinal(str)
}

fun PronoteKt.aesDecrypt(str: ByteArray, key: String) = aesDecrypt(str, key, iv)

fun md5(str: String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(str.toByteArray())).toString(16).padStart(32, '0')
}

fun md5(str: ByteArray): ByteArray {
    val md = MessageDigest.getInstance("MD5")
    return md.digest(str)
}

fun sha256(str: ByteArray): ByteArray {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(str)
}

fun base64Encode(str: ByteArray): String {
    return Base64.getEncoder().encodeToString(str)
}