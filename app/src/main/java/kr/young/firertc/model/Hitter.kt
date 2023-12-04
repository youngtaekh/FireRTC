package kr.young.firertc.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hitters")
class Hitter {
    @PrimaryKey
    lateinit var id: String
    var birthYear: Int = 2023
    var birthMonth: Int = 1
    var birthDay: Int = 1
    lateinit var name: String
    var age: Int = 0
    var year: Int = 0
    lateinit var team: String
    lateinit var position: String
    var warStar: Double = 0.0

    // General
    var gameCount: Int = 0
    var plateAppearance: Int = 0
    var atBat: Int = 0
    var run: Int = 0
    var hit: Int = 0
    var twoHit: Int = 0
    var threeHit: Int = 0
    var homerun: Int = 0
    var runsBattedIn: Int = 0
    var stealBase: Int = 0
    var caughtStealing: Int = 0
    var baseOnBalls: Int = 0
    var hitByPitch: Int = 0
    var intentionalWalks: Int = 0
    var strikeOut: Int = 0
    var doublePlay: Int = 0
    var sacrificeBunts: Int = 0
    var sacrificeFly: Int = 0
    var battingAverage: Double = 0.0
    var onBasePercentage: Double = 0.0
    var sluggingAverage: Double = 0.0
    var ops: Double = 0.0
    var weightedOBA: Double = 0.0
    var wrcPlus: Double = 0.0
    var wpa: Double = 0.0

    fun setID() {
        id = "$age$name$year"
    }

    fun setValue(key: String, value: String) {
        try {
            when (key) {
                "이름" -> this.name = value
                "WAR*" -> this.warStar = value.toDouble()
                "G" -> this.gameCount = value.toInt()
                "타석" -> this.plateAppearance = value.toInt()
                "타수" -> this.atBat = value.toInt()
                "득점" -> this.run = value.toInt()
                "안타" -> this.hit = value.toInt()
                "2타" -> this.twoHit = value.toInt()
                "3타" -> this.threeHit = value.toInt()
                "홈런" -> this.homerun = value.toInt()
                "타점" -> this.runsBattedIn = value.toInt()
                "도루" -> this.stealBase = value.toInt()
                "도실" -> this.caughtStealing = value.toInt()
                "볼넷" -> this.baseOnBalls = value.toInt()
                "사구" -> this.hitByPitch = value.toInt()
                "고4" -> this.intentionalWalks = value.toInt()
                "삼진" -> this.strikeOut = value.toInt()
                "병살" -> this.doublePlay = value.toInt()
                "희타" -> this.sacrificeBunts = value.toInt()
                "희비" -> this.sacrificeFly = value.toInt()
                "타율" -> this.battingAverage = value.toDouble()
                "출루" -> this.onBasePercentage = value.toDouble()
                "장타" -> this.sluggingAverage = value.toDouble()
                "OPS" -> this.ops = value.toDouble()
                "wOBA" -> this.weightedOBA = value.toDouble()
                "wRC+" -> this.wrcPlus = value.toDouble()
                "WPA" -> this.wpa = value.toDouble()
                "팀" -> {
                    this.year = value.substring(0, 2).toInt()
                    this.team = value.substring(2, 3)
                    this.position = value.substring(3, value.length)
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