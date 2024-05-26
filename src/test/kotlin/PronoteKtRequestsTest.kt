import fr.xibalba.pronoteKt.Period
import fr.xibalba.pronoteKt.PronoteKt
import fr.xibalba.pronoteKt.SessionType
import fr.xibalba.pronoteKt.getNotes
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
    fun init(): Unit = runTest {
        assert(pronoteKt.login("demonstration", "pronotevs"))
    }

    @Test
    fun testGetTimetable(): Unit = runTest {
        val timetable = pronoteKt.getTimetable()
        assertNotEquals(timetable.size, 0)
    }

    @Test
    fun testGetNotes(): Unit = runTest {
        val notes = pronoteKt.getNotes(Period.FIRST_TRIMESTER)
        assertNotEquals(notes.notes.size, 0)
    }
}