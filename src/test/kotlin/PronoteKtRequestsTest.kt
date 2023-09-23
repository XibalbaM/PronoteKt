import fr.xibalba.pronoteKt.PronoteKt
import fr.xibalba.pronoteKt.SessionType
import fr.xibalba.pronoteKt.getTimetable
import kotlinx.coroutines.test.runTest

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNotEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PronoteKtRequestsTest {

    private val pronoteKt = PronoteKt("https://demo.index-education.net/pronote", SessionType.STUDENT)

    @BeforeAll
    fun init() = runTest {
        assert(pronoteKt.login("demonstration", "pronotevs"))
    }

    @Test
    fun testGetTimetable() = runTest {
        val timetable = pronoteKt.getTimetable()
        assertNotEquals(timetable.size, 0)
    }
}