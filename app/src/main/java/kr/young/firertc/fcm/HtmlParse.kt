package kr.young.firertc.fcm

import kr.young.common.UtilLog.Companion.i
import kr.young.firertc.model.Game
import kr.young.firertc.repo.GameRepository
import kr.young.firertc.util.DateUtil
import java.lang.Integer.max
import java.net.URLDecoder
import java.util.StringTokenizer

class HtmlParse {
    companion object {
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

        fun player(html: String) {
            var parsing = html.substring(html.indexOf("<tr"), html.lastIndexOf("</td"))
            parsing = parsing.replace("\r\n", "")
            parsing = parsing.replace("</tr>", "\r\n")
            parsing = parsing.replace(Regex("<tr[^>]*>"), "")
//            parsing = parsing.replace(Regex("style='[^'>]*"), "")
            parsing = parsing.replace(Regex("</[^>]*>"), "")
            parsing = parsing.replace(Regex("<[^t][^>]*>"), "")
            parsing = parsing.replace(Regex("<th[^>]*>"), "<th>")
            parsing = parsing.replace(Regex("<td[^>]*>"), "<td>")
//            parsing = parsing.replace(Regex("<a[^>]*>"), "")

//            parsing = parsing.replace(Regex("<[^>]*>"), " ")
//            i(TAG, parsing)
            val row = StringTokenizer(parsing, "\r\n")
            val category = mutableListOf<MutableList<String>>()
            val modifiedCategory = mutableListOf<String>()
            val data = mutableListOf<MutableList<String>>()
            while (row.hasMoreTokens()) {
                var line = row.nextToken().trim()
                val list = mutableListOf<String>()
                if (line.startsWith("<th>")) {
                    if (category.size < 2) {
                        while (line.startsWith("<th>")) {
                            line = line.substring(line.indexOf("<th>") + 4)
                            if (line.contains("<th>")) {
                                list.add(line.substring(0, line.indexOf("<th>")))
                                line = line.substring(line.indexOf("<th>"))
                            } else {
                                list.add(line.substring(0, line.length))
                            }
                        }
                        category.add(list)
                    }
                } else if (line.startsWith("<td>")) {
                    while (line.startsWith("<td>")) {
                        line = line.substring(line.indexOf("<td>") + 4)
                        if (line.contains("<td>")) {
                            list.add(line.substring(0, line.indexOf("<td>")))
                            line = line.substring(line.indexOf("<td>"))
                        } else {
                            list.add(line.substring(0, line.length))
                        }
                    }
                    data.add(list)
                } else if (!line.startsWith("<th>") && !line.startsWith("<td>")) {
                    break
                }
            }
            if (category.size == 2) {
                for (cell in category[0]) {
                    when (cell) {
                        "정렬" -> modifiedCategory.add(category[1][0])
                        "비율" -> modifiedCategory.addAll(category[1].subList(1, category[1].size - 1))
                        else -> modifiedCategory.add(cell)
                    }
                }
            }
            val count = 10
            var ten = 0
            while (ten < data.size) {
                for (i in modifiedCategory.indices) {
                    val builder = StringBuilder()
                    builder.append(String.format("%-6s\t", modifiedCategory[i]))
                    for (j in 0 until count) {
                        if (j + ten >= data.size)    break
                        builder.append(String.format("%-6s\t", data[ten+j][i]))
                    }
                    println(builder.toString())
                }
                ten += count
            }
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