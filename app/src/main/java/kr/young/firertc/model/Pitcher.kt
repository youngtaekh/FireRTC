package kr.young.firertc.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pitchers")
class Pitcher {
    @PrimaryKey
    lateinit var id: String
    var birthYear: Int = 2023
    var birthMonth: Int = 1
    var birthDay: Int = 1
    lateinit var name: String
    var age: Int = 0
    var year: Int = 0
    lateinit var team: String
    var warStar: Double = 0.0

    // General
    var gameCount: Int = 0
    var completeGame: Int = 0
    var shutout: Int = 0
    var start: Int = 0
    var win: Int = 0
    var lose: Int = 0
    var save: Int = 0
    var hold: Int = 0
    var inning: Double = 0.0
    var run: Int = 0
    var earnedRun: Int = 0
    var hitters: Int = 0
    var hit: Int = 0
    var twoHit: Int = 0
    var threeHit: Int = 0
    var homerun: Int = 0
    var baseOnBalls: Int = 0
    var intentionalWalks: Int = 0
    var hitByPitch: Double = 0.0
    var strikeOut: Double = 0.0
    var balk: Double = 0.0
    var wildPitch: Double = 0.0
    var era: Double = 0.0
    var fip: Double = 0.0
    var whip: Double = 0.0
    var eraPlus: Double = 0.0
    var fipPlus: Double = 0.0
    var wpa: Double = 0.0

    fun setID() {
        id = "$age$name$year"
    }

    fun setValue(key: String, value: String) {
        try {
            when (key) {
                "이름" -> this.name = value
                "WAR*" -> this.warStar = value.toDouble()
                "출장" -> this.gameCount = value.toInt()
                "완투" -> this.completeGame = value.toInt()
                "완봉" -> this.shutout = value.toInt()
                "선발" -> this.start = value.toInt()
                "승" -> this.win = value.toInt()
                "패" -> this.lose = value.toInt()
                "세" -> this.save = value.toInt()
                "홀드" -> this.hold = value.toInt()
                "이닝" -> this.inning = value.toDouble()
                "실점" -> this.run = value.toInt()
                "자책" -> this.earnedRun = value.toInt()
                "타자" -> this.hitters = value.toInt()
                "안타" -> this.hit = value.toInt()
                "2타" -> this.twoHit = value.toInt()
                "3타" -> this.threeHit = value.toInt()
                "홈런" -> this.homerun = value.toInt()
                "볼넷" -> this.baseOnBalls = value.toInt()
                "고4" -> this.intentionalWalks = value.toInt()
                "사구" -> this.hitByPitch = value.toDouble()
                "삼진" -> this.strikeOut = value.toDouble()
                "보크" -> this.balk = value.toDouble()
                "폭투" -> this.wildPitch = value.toDouble()
                "ERA" -> this.era = value.toDouble()
                "FIP" -> this.fip = value.toDouble()
                "WHIP" -> this.whip = value.toDouble()
                "ERA+" -> this.eraPlus = value.toDouble()
                "FIP+" -> this.fipPlus = value.toDouble()
                "WPA" -> this.wpa = value.toDouble()
                "팀" -> {
                    this.year = value.substring(0, 2).toInt()
                    this.team = value.substring(2, 3)
                }
                "연도" -> {
                    this.birthYear = value.substring(0, 4).toInt()
                    this.birthMonth = value.substring(5, 7).toInt()
                    this.birthDay = value.substring(8, 10).toInt()
                }
            }
        } catch (_: NumberFormatException) {}
    }
}