package fr.xibalba.pronoteKt

import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

const val ALGORITHM = "AES/CBC/PKCS5Padding"

fun aesEncrypt(plainText: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val secretKey = SecretKeySpec(md5(key), "AES")
    val ivParameterSpec = IvParameterSpec(iv)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
    return cipher.doFinal(plainText)
}

fun PronoteKt.aesEncrypt(str: ByteArray, key: ByteArray = this.key) = aesEncrypt(str, key, iv)

fun aesDecrypt(str: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
    val keySpec = SecretKeySpec(md5(key), "AES")
    val ivSpec = IvParameterSpec(iv)

    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

    return cipher.doFinal(str)
}

fun PronoteKt.aesDecrypt(str: ByteArray, key: ByteArray = this.key) = aesDecrypt(str, key, iv)

fun md5(str: ByteArray): ByteArray {
    val md = MessageDigest.getInstance("MD5")
    return md.digest(str)
}

fun base64Encode(str: ByteArray): String {
    return Base64.getEncoder().encodeToString(str)
}