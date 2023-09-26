package kr.young.firertc.repo

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kr.young.common.UtilLog.Companion.e
import kr.young.common.UtilLog.Companion.i
import kr.young.firertc.model.Game
import kr.young.firertc.util.DateUtil
import okhttp3.internal.notify
import java.lang.Integer.min
import java.lang.System.currentTimeMillis
import java.util.*

class GameRepository {
    companion object {
        fun post(game: Game) {
            Firebase.firestore.collection(COLLECTION)
                .document(game.id)
                .set(game.toMap())
                .addOnSuccessListener {
                    i(TAG, "game ${game.id} post success")
                }.addOnFailureListener {
                    e(TAG, "game ${game.id} post error $it")
                }
        }

        fun getGames(
            endDate: Date = Date(currentTimeMillis()),
            startDate: Date = DateUtil.getDate(DateUtil.getYear(endDate), 1, 1)
        ) {
            Firebase.firestore.collection(COLLECTION)
                .whereGreaterThan("gameDate", startDate)
                .whereLessThan("gameDate", endDate)
                .get()
                .addOnSuccessListener { query ->
                    i(TAG, "size - ${query.size()}")
                    val map = TeamStanding.getTeamMap()
                    for (document in query) {
                        val game = document.toObject<Game>()
                        val home = map[game.home]!!
                        val away = map[game.away]!!
                        when (game.result) {
                            Game.Result.Draw -> {
                                home.draw += 1
                                away.draw += 1
                            }
                            Game.Result.Home -> {
                                home.win += 1
                                away.lose += 1
                            }
                            Game.Result.Away -> {
                                home.lose += 1
                                away.win += 1
                            }
                            else -> {}
                        }
                        map[game.home]!!.rate = home.win.toDouble() / (home.win + home.lose).toDouble()
                        map[game.away]!!.rate = away.win.toDouble() / (away.win + away.lose).toDouble()
                    }

                    for ((i, team) in map.toList().sortedByDescending { it.second.rate }.withIndex()) {
                        i(TAG, "${i+1} $team")
                    }
                }.addOnFailureListener {
                    e(TAG, "getGames Failure", it)
                }
        }

        private const val TAG = "GameRepository"
        private const val COLLECTION = "games"
    }

    class TeamStanding(private val team: Game.Team) {
        var win: Int = 0
        var draw: Int = 0
        var lose: Int = 0
        var rate: Double = 0.0

        override fun toString(): String {
            return "$team $win $draw $lose ${rate.toString().substring(0, min(5, rate.toString().length))} ${win-lose}"
        }

        companion object {
            fun getTeamMap(): Map<Game.Team, TeamStanding> {
                return mapOf(
                    Game.Team.LGTwins to TeamStanding(Game.Team.LGTwins),
                    Game.Team.SSGLanders to TeamStanding(Game.Team.SSGLanders),
                    Game.Team.NCDinos to TeamStanding(Game.Team.NCDinos),
                    Game.Team.KTWiz to TeamStanding(Game.Team.KTWiz),
                    Game.Team.DoosanBears to TeamStanding(Game.Team.DoosanBears),
                    Game.Team.KiaTigers to TeamStanding(Game.Team.KiaTigers),
                    Game.Team.LotteGiants to TeamStanding(Game.Team.LotteGiants),
                    Game.Team.HanwhaEagles to TeamStanding(Game.Team.HanwhaEagles),
                    Game.Team.KiwoomHeroes to TeamStanding(Game.Team.KiwoomHeroes),
                    Game.Team.SamsungLions to TeamStanding(Game.Team.SamsungLions),
                )
            }
        }
    }
}