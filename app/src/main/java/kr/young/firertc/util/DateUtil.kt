package kr.young.firertc.util

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class DateUtil {
    companion object {
        fun getDate(year: Int, month: Int, day: Int): Date {
            val localDate = LocalDate.of(year, month, day)
//            println(Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
//            println(localDate)
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        }

        fun getDateString(date: Date): String {
            val simpleDateFormat = SimpleDateFormat("yy.MM.dd", Locale.getDefault())
            return simpleDateFormat.format(date)
        }

        fun getDate(src: String, format: String): Date {
            val simpleDateFormat = SimpleDateFormat(format, Locale.getDefault())
            return simpleDateFormat.parse(src)!!
        }

        fun getYear(date: Date): Int {
            val simpleDateFormat = SimpleDateFormat("yyyy", Locale.getDefault())
            return simpleDateFormat.format(date).toInt(10)
        }

        private const val TAG = "DateUtil"
    }
}