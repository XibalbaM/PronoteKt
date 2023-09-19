import fr.xibalba.pronoteKt.PronoteKt
import fr.xibalba.pronoteKt.SessionType
import kotlinx.coroutines.test.runTest

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNotEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PronoteKtRequestsTest {

    val pronoteKt = PronoteKt("https://demo.index-education.net/pronote", SessionType.STUDENT)

    @BeforeAll
    fun init() = runTest {
        pronoteKt.initSession()
        assertNotEquals("", pronoteKt.identifiantNav)
    }
}