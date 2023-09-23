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
    @JsonAdapter(VListLPairDeserializer::class)
    private val subjectAndTeacher: Pair<String, String>
) {
    val date: LocalDate
        get() = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
    val subject: String
        get() = subjectAndTeacher.first
    val teacher: String
        get() = subjectAndTeacher.second
}