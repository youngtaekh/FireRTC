package kr.young.firertc.fcm

import android.annotation.SuppressLint
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.i
import kr.young.firertc.model.Game
import kr.young.firertc.model.Hitter
import kr.young.firertc.model.Pitcher
import kr.young.firertc.repo.GameRepository
import kr.young.firertc.repo.HitterDatabase
import kr.young.firertc.repo.PitcherDatabase
import kr.young.firertc.util.DateUtil
import java.net.URLDecoder
import java.util.*

class HtmlParse {
    companion object {

        val generalList = listOf("순", "이름", "팀", "WAR*", "G", "타석", "타수", "득점", "안타", "2타",
            "3타", "홈런", "루타", "타점", "도루", "도실", "볼넷", "사구", "고4", "삼진", "병살", "희타", "희비",
            "타율", "출루", "장타", "OPS", "wOBA", "wRC+", "WAR*", "WPA")
        private val birthList = mutableListOf<String>()

        fun schedule(html: String) {
            var startIndex = 0
            while (html.indexOf(START_BOXSCORE, startIndex) != -1) {
                startIndex = html.indexOf(START_BOXSCORE, startIndex) + START_BOXSCORE.length
                if (html[startIndex] == '?') {
                    val boxscore = html.substring(startIndex+1, html.indexOf("\'", startIndex))
                    val params = getBoxscoreParam(boxscore)
                    if (params.size != 3) { continue }

                    //Get Team
                    startIndex = html.indexOf("<span", startIndex) + 5
                    val away = html.substring(html.indexOf(">", startIndex) + 1, html.indexOf("<", startIndex))
                    if (away.length == 1) { continue }
                    startIndex = html.indexOf("<span", startIndex) + 5
                    val awayScore = html.substring(html.indexOf(">", startIndex) + 1, html.indexOf("<", startIndex))
                    startIndex = html.indexOf("<span", startIndex) + 5
                    val homeScore = html.substring(html.indexOf(">", startIndex) + 1, html.indexOf("<", startIndex))
                    startIndex = html.indexOf("<span", startIndex) + 5
                    val home = html.substring(html.indexOf(">", startIndex) + 1, html.indexOf("<", startIndex))

                    try {
                        val game = Game(
                            home = Game.toTeam(home),
                            away = Game.toTeam(away),
                            homeScore = homeScore.toInt(10),
                            awayScore = awayScore.toInt(10),
                            stadium = URLDecoder.decode(params["stadium"], "UTF-8"),
                            gameDate = DateUtil.getDate(params["date"]!!, "yyyy-MM-dd"),
                            startTime = params["hour"]!!.toInt(10)
                        )

                        println(game.toString())
                        GameRepository.post(game)
                    } catch (e: java.lang.NumberFormatException) {
                        print("asdf")
                        break
                    }
//                    println("${params["date"]} $away $awayScore : $homeScore $home(${URLDecoder.decode(params["stadium"], "UTF-8")})")
                }
            }
            i(TAG, "Parse End!!!")
        }

        private fun removePlayerTag(html: String): String {
            var parsing = html.substring(html.indexOf("<tr"), html.lastIndexOf("</td"))
            parsing = parsing.replace("\r\n", "")
            parsing = parsing.replace("</tr>", "\r\n")
            parsing = parsing.replace(Regex("<tr[^>]*>"), "")
            parsing = parsing.replace(Regex("</[^>]*>"), "")
            var startIndex = 0
            while (parsing.indexOf("birth", startIndex) > 0) {
                startIndex = parsing.indexOf("birth", startIndex) + 6
                if (parsing.substring(startIndex+11, startIndex+12) == ">") {
                    val birth = parsing.substring(startIndex, startIndex + 10)
                    birthList.add(birth)
                }
            }
            parsing = parsing.replace(Regex("<[^t][^>]*>"), "")
            parsing = parsing.replace(Regex("<th[^>]*>"), "<th>")
            parsing = parsing.replace(Regex("<td[^>]*>"), "<td>")

            return parsing
        }

        private fun tableTokenizer(row: String, delim: String): MutableList<String>? {
            var line = row
            val list = mutableListOf<String>()
            if (line.startsWith(delim)) {
                while (line.startsWith(delim)) {
                    line = line.substring(line.indexOf(delim) + 4)
                    if (line.contains(delim)) {
                        list.add(line.substring(0, line.indexOf(delim)).trim())
                        line = line.substring(line.indexOf(delim))
                    } else {
                        list.add(line.substring(0, line.length).trim())
                    }
                }
                return list
            }
            return null
        }

        @SuppressLint("CheckResult")
        fun pitcher(html: String, age: Int) {
            val parsing = removePlayerTag(html)
            val category = mutableListOf<List<String>>()
            val data = mutableListOf<MutableList<String>>()

            val row = StringTokenizer(parsing, "\r\n")

            while (category.size < 2 && row.hasMoreTokens()) {
                tableTokenizer(row.nextToken().trim(), "<th>")?.let { category.add(it) }
            }

            val modifiedCategory = categoryModified(category)

            while (row.hasMoreTokens()) {
                val line = row.nextToken().trim()
                tableTokenizer(line, "<td>")?.let { data.add(it) }

                if (!line.startsWith("<th>") && !line.startsWith("<td>")) {
                    break
                }
            }

            val pitcherList = mutableListOf<Pitcher>()
            for (i in data.indices) {
                val pitcher = Pitcher()
                pitcher.age = age
                data[i].add(1, birthList[i])

                for (j in modifiedCategory.indices) {
                    pitcher.setValue(modifiedCategory[j], data[i][j])
                }
                pitcher.setID()
                Observable.just(pitcher)
                    .observeOn(Schedulers.io())
                    .subscribe { PitcherDatabase.getInstance()!!.pitcherDao().setPitcher(it) }
                pitcherList.add(pitcher)
            }

            printPlayers(modifiedCategory, data)
        }

        @SuppressLint("CheckResult")
        fun hitter(html: String, age: Int) {
            val parsing = removePlayerTag(html)
            val category = mutableListOf<List<String>>()
            val data = mutableListOf<MutableList<String>>()

            val row = StringTokenizer(parsing, "\r\n")

            while (category.size < 2 && row.hasMoreTokens()) {
                tableTokenizer(row.nextToken().trim(), "<th>")?.let { category.add(it) }
            }

            val modifiedCategory = categoryModified(category)

            while (row.hasMoreTokens()) {
                val line = row.nextToken().trim()
                tableTokenizer(line, "<td>")?.let { data.add(it) }

                if (!line.startsWith("<th>") && !line.startsWith("<td>")) {
                    break
                }
            }

            val hitterList = mutableListOf<Hitter>()
            for (i in data.indices) {
                val hitter = Hitter()
                hitter.age = age
                data[i].add(1, birthList[i])

                for (j in modifiedCategory.indices) {
                    hitter.setValue(modifiedCategory[j], data[i][j])
                }
                hitter.setID()
                Observable.just(hitter)
                    .observeOn(Schedulers.io())
                    .subscribe { HitterDatabase.getInstance()!!.hitterDao().setHitter(hitter) }
                hitterList.add(hitter)
            }

            printPlayers(modifiedCategory, data)

            d(TAG, "dataSize ${data.size} listSize ${hitterList.size}")
        }

        private fun printPlayers(modifiedCategory: List<String>, data: List<List<String>>) {
            val count = 10
            var ten = 0
            while (ten < data.size) {
                for (i in modifiedCategory.indices) {
                    val builder = StringBuilder(String.format("%-5s\t", modifiedCategory[i]))
                    for (j in data.indices) {
                        if (j + ten >= data.size) break
                        builder.append(String.format("%-5s\t", data[ten+j][i]))
                    }
                    println(builder.toString())
                }
                ten += count
            }
        }

        private fun categoryModified(categories: List<List<String>>): List<String> {
            val modifiedCategory = mutableListOf<String>()
            if (categories.size == 2) {
                for (cell in categories[0]) {
                    when (cell) {
                        "정렬" -> modifiedCategory.add(categories[1][0])
                        "비율" -> modifiedCategory.addAll(categories[1].subList(1, categories[1].size))
                        else -> modifiedCategory.add(cell)
                    }
                }
            }
            if (modifiedCategory.size > 0) {
                modifiedCategory.add(1, "연도")
            }
            return modifiedCategory
        }

        private fun getBoxscoreParam(src: String): MutableMap<String, String> {
            val map = mutableMapOf<String, String>()
            val tokenizer = StringTokenizer(src, "&")
            while (tokenizer.hasMoreTokens()) {
                val keyValue = StringTokenizer(tokenizer.nextToken(), "=")
                if (keyValue.countTokens() == 2) {
                    map[keyValue.nextToken()] = keyValue.nextToken()
                }
            }
            return map
        }

        private const val TAG = "HtmlParse"
        private const val START_BOXSCORE = "boxscore.php"
    }
}