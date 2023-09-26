package kr.young.firertc.fcm

import kr.young.common.UtilLog.Companion.i
import kr.young.firertc.model.Game
import kr.young.firertc.repo.GameRepository
import kr.young.firertc.util.DateUtil
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