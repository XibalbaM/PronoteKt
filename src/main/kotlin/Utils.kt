package fr.xibalba.pronoteKt

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.*

fun getWeekNumber(date: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault()): Int {
    return date[WeekFields.of(locale).weekOfYear()]
}

class VDeserializer<T> : JsonDeserializer<T> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): T {
        return gson.fromJson(json?.asJsonObject?.get("V"), typeOfT)
    }
}

class VListLPairDeserializer : JsonDeserializer<Pair<String, String>> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Pair<String, String> {
        val array = json?.asJsonObject?.get("V")?.asJsonArray?.map<JsonElement, String> {
            gson.fromJson(
                it
                    .getAsJsonObject()
                    .get("L"), String::class.java
            )
        }?.toList() ?: emptyList()
        return array[0] to array[1]
    }
}