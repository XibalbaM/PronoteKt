package fr.xibalba.pronoteKt

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.time.format.DateTimeFormatter

val gson by lazy {
    val gsonBuilder = GsonBuilder()
    gsonBuilder.registerTypeAdapter(Float::class.java, CustomFloatDeserializer())
    gsonBuilder.create()
}

suspend fun PronoteKt.getTimetable(week: Int = getWeekNumber()): Timetable = requireLogin {

    val ressources = getUserData().get("ressource")?.asJsonObject ?: throw IllegalStateException("Error while loading user data")

    val timetableResponse = doRequest(
        "PageEmploiDuTemps", mapOf(
            "ressource" to ressources,
            "Ressource" to ressources,
            "numeroSemaine" to week,
            "NumeroSemaine" to week,
            "avecAbsencesEleve" to false,
            "avecConseilDeClasse" to true,
            "estEDTPermanence" to false,
            "avecAbsencesRessource" to true,
            "avecDisponibilites" to true,
            "avecInfosPrefsGrille" to true,
        ), 16
    ) ?: throw IllegalStateException("Error while getting timetable")

    class Token : TypeToken<List<Lesson>>()

    val brutTimetable: List<Lesson> = gson.fromJson(timetableResponse.get("ListeCours"), Token().type)
    brutTimetable.reversed().groupBy { it.date.dayOfYear }.map { (_, lessons) ->
        val date = lessons.first().date
        TimetableDay(date.format(DateTimeFormatter.ISO_LOCAL_DATE), date.dayOfWeek, lessons)
    }.sortedBy { it.date }
}

suspend fun PronoteKt.getNotes(period: Period): Notes = requireLogin {

    val periodList = parameters
        ?.getAsJsonObject("General")
        ?.getAsJsonArray("ListePeriodes")
        ?: throw IllegalStateException("Error while getting periods")
    val periodData = periodList.find { it.asJsonObject.get("L").asString == period.dataName }?.asJsonObject?.get("N")?.asString
        ?: throw IllegalStateException("Error while getting period data")
    val timetableResponse = doRequest("DernieresNotes",
        mapOf(
            "Periode" to mapOf(
                "G" to period.g,
                "L" to period.dataName,
                "N" to periodData
            )
        ),
        198
    ) ?: throw IllegalStateException("Error while getting notes")
    class Token : TypeToken<Notes>()
    gson.fromJson(timetableResponse, Token().type)
}