package kr.young.firertc.model

import kr.young.firertc.util.DateUtil
import java.lang.System.currentTimeMillis
import java.util.*

data class Game(
    val home: Team? = null,
    val away: Team? = null,
    val homeScore: Int = -1,
    val awayScore: Int = -1,
    var result: Result? = null,
    val stadium: String? = null,
    val gameDate: Date? = null,
    val startTime: Int? = null,
    val id: String = "${DateUtil.getDateString(gameDate ?: Date(currentTimeMillis()))}.$away:$home"
) {
    init {
        if (result == null) {
            result = if (homeScore == -1) {
                Result.Init
            } else if (homeScore == awayScore) {
                Result.Draw
            } else if (homeScore > awayScore) {
                Result.Home
            } else if (homeScore < awayScore) {
                Result.Away
            } else {
                Result.Cancel
            }
        }
    }

    override fun toString(): String {
        return "Game(${DateUtil.getDateString(gameDate!!)}($startTime) $away $awayScore : $homeScore $home($stadium) $result)"
    }

    fun toMap(): Map<String, Any> {
        return mapOf<String, Any> (
            "id" to id,
            "home" to home.toString(),
            "away" to away.toString(),
            "homeScore" to homeScore,
            "awayScore" to awayScore,
            "result" to result!!,
            "stadium" to stadium!!,
            "gameDate" to gameDate!!,
            "startTime" to startTime!!
        )
    }

    companion object {
        fun toTeam(code: String): Team {
            return when (code) {
                "LG" -> Team.LGTwins
                "SSG" -> Team.SSGLanders
                "NC" -> Team.NCDinos
                "KT" -> Team.KTWiz
                "두산" -> Team.DoosanBears
                "KIA" -> Team.KiaTigers
                "롯데" -> Team.LotteGiants
                "한화" -> Team.HanwhaEagles
                "키움" -> Team.KiwoomHeroes
                "삼성" -> Team.SamsungLions
                else -> Team.Error
            }
        }
    }

    enum class Stadium(val value: String) {
        Go("고척돔"),
    }

    enum class Team(val value: String) {
        LGTwins("LGTwins"), SSGLanders("SSGLanders"), NCDinos("NCDinos"),
        KTWiz("KTWiz"), DoosanBears("DoosanBears"), KiaTigers("KiaTigers"),
        LotteGiants("LotteGiants"), HanwhaEagles("HanwhaEagles"),
        KiwoomHeroes("KiwoomHeroes"), SamsungLions("SamsungLions"),
        SKWyverns("SKWyverns"), NexenHeroes("NexenHeroes"), Error("Error")
    }

    enum class Result {
        Init, Home, Away, Draw, Cancel
    }
}
