package fr.xibalba.pronoteKt

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

typealias Timetable = List<TimetableDay>

data class TimetableDay(
    val date: String,
    val dayOfWeek: DayOfWeek,
    val lessons: List<Lesson>
)

data class Lesson(
    @SerializedName("place")
    val startingPosition: Int,
    @SerializedName("duree")
    val duration: Int,
    @SerializedName("DateDuCours")
    @JsonAdapter(VDeserializer::class)
    val dateString: String,
    @SerializedName("CouleurFond")
    val color: String,
    @SerializedName("ListeContenus")
    @JsonAdapter(VListLListDeserializer::class)
    val courseInfo: List<String>
) {
    val date: LocalDate
        get() = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
}

data class Notes(
    @SerializedName("listeDevoirs")
    @JsonAdapter(VDeserializer::class)
    val notes: List<Note>,
    @SerializedName("listeServices")
    @JsonAdapter(VDeserializer::class)
    val subjects: List<SubjectAverage>,
    @SerializedName("moyGenerale")
    @JsonAdapter(VDeserializer::class)
    val globalAverage: Float,
    @SerializedName("moyGeneraleClasse")
    @JsonAdapter(VDeserializer::class)
    val globalClassAverage: Float,
)

data class Note(
    @SerializedName("date")
    @JsonAdapter(VDeserializer::class)
    val dateString: String,
    @SerializedName("service")
    @JsonAdapter(VLDeserializer::class)
    val subject: String,
    @SerializedName("note")
    @JsonAdapter(VDeserializer::class)
    val mark: Float,
    @SerializedName("bareme")
    @JsonAdapter(VDeserializer::class)
    val scale: Float,
    @SerializedName("commentaire")
    val title: String,
    @SerializedName("moyenne")
    @JsonAdapter(VDeserializer::class)
    val average: Float,
    @SerializedName("noteMax")
    @JsonAdapter(VDeserializer::class)
    val greatest: Float,
    @SerializedName("noteMin")
    @JsonAdapter(VDeserializer::class)
    val lowest: Float,
    @SerializedName("coefficient")
    val coefficient: Float,
    @SerializedName("estRamenerSur20")
    val isSetOver20: Boolean = false,
    @SerializedName("estFacultatif")
    val isOptional: Boolean = false,
    @SerializedName("estBonus")
    val isBonus: Boolean = false,
    @SerializedName("estEnGroupe")
    val isInTeam: Boolean = false,
) {
    val date: LocalDate
        get() = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}

data class SubjectAverage(
    @SerializedName("L")
    val subject: String,
    @SerializedName("moyEleve")
    @JsonAdapter(VDeserializer::class)
    val average: Float,
    @SerializedName("moyMax")
    @JsonAdapter(VDeserializer::class)
    val greatest: Float,
    @SerializedName("moyMin")
    @JsonAdapter(VDeserializer::class)
    val lowest: Float,
    @SerializedName("moyClasse")
    @JsonAdapter(VDeserializer::class)
    val classAverage: Float
)

enum class Period(val dataName: String, val g: Int) {
    FIRST_TRIMESTER("Trimestre 1", 2),
    SECOND_TRIMESTER("Trimestre 2", 2),
    THIRD_TRIMESTER("Trimestre 3", 2),
    FIRST_SEMESTER("Semestre 1", 2),
    SECOND_SEMESTER("Semestre 2", 2),
    FIRST_SEMESTER_TERM("SEMESTRE 1 Term", 4),
    SECOND_SEMESTER_TERM("SEMESTRE  2 Term", 4),
    OUT_OF_PERIOD("Hors période", 4);
    //TODO : SEMESTERS AND DATES FROM DATA
}