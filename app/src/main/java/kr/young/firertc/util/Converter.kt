package kr.young.firertc.util

import androidx.room.TypeConverter
import com.google.gson.JsonArray
import org.json.JSONArray
import java.util.*

class Converter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromList(value: List<String>): String {
        val jsonArray = JsonArray()
        value.map { jsonArray.add(it) }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toList(value: String): List<String> {
        val jsonArray = JSONArray(value)
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }
}