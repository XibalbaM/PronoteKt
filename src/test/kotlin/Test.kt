import fr.xibalba.pronoteKt.base64Encode
import org.junit.jupiter.api.Test

class Test {

    @Test
    fun test() {
        println(base64Encode("\"ú`F\u0082];^\u001Ebâ?0t¬\u009C\u007F\"".toByteArray()))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun test2() {
        val t = byteArrayOf(84, -7, 11, 87, 104, 50, -82, 12, -16, 87, -127, -110, 123, -6, 78, 63)
        println(base64Encode(t))
    }
}