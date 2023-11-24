package kr.young.firertc.fcm

import com.google.gson.JsonObject
import kr.young.firertc.BuildConfig
import kr.young.firertc.model.Game
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface NotificationApiService {
    @POST("fcm/send")
    fun sendNotification(
        @Header("Authorization") authorization: String = "key=${BuildConfig.FCM_SERVER_KEY}",
        @Header("Content-Type") contentType: String = "application/json",
        @Body payload: JsonObject?
    ): Call<JsonObject?>?

    @GET("team.php")
    fun getTeam(
        @Query("opt")opt: Int = 0,
        @Query("sopt")sopt: Int = 1,
        @Query("year")year: Int = 2023,
        @Query("team")team: String
    ): Call<ResponseBody>

    @GET("schedule.php")
    fun getSchedule(
        @Query("opt")month: Int,
        @Query("sy")year: Int
    ): Call<ResponseBody>

    @GET("boxscore.php")
    fun getBoxScore(
        @Query("date")date: String,
        @Query("stadium")stadium: String,
        @Query("hour")hour: Int
    ): Call<ResponseBody>

    // tm=&
    // hi=&un=&pl=&
    // tr=&cv=&
    // ml=1&
    // si=&cn=&
    // opt=0&sopt=0&
    // si=999&si_it=1&si_ds=04-01&si_de=4-30&si_wd=&si_tm=&si_ha=&si_te=&si_st=&si_as=&si_or=&si_ty=&si_pl=&si_in=&si_on=&si_um=&si_oc=&si_bs=&si_sc=&si_cnt=&si_aft=&si_li=
    // si=999&si_it=1&si_wd=1&si_tm=&si_ha=&si_te=&si_st=&si_as=&si_or=&si_ty=&si_pl=&si_in=&si_on=&si_um=&si_oc=&si_bs=&si_sc=&si_cnt=&si_aft=&si_li=
    @GET("stat.php")
    fun getPlayer(
        @Query("mid")mid: String = "stat",
        @Query("re")tool: Int = Tool.Batting.idx,
        @Query("ys")yearStart: Int = 1982,
        @Query("ye")yearEnd: Int = 2023,
        @Query("se")season: Int = Season.Regular.idx,
        @Query("te")team: String = Team.All.title,
        @Query("ty")battingLocation: Int = BattingLocation.All.idx,
        @Query("qu")qu: String = "100", //all, p70, 500
        @Query("po")position: Int = FieldPosition.All.idx,
        @Query("da")da: Int = 1,    //Category(기본, 확장...)
        @Query("o1")sort1: String = IndexCode.WAR_ALL_ADJ.toString(),
        @Query("o2")sort2: String = IndexCode.TPA.toString(),
        @Query("de")descending: Int = 1,
        @Query("lr")lr: Int = 0,    //표기
        @Query("ml")ml: Int = 1,
        @Query("sn")count: Int = 100,
        @Query("pa")pa: Int = 0, // 이상 순위부터 출력
        @Query("as")ageStart: Int = 19,
        @Query("ae")ageEnd: Int = 19,
    ): Call<ResponseBody>
}

enum class Team(val title: String) {
    All(""),
    LG("LG"),
    Doosan("두산"),
    Kiwoom("키움"),
    SSG("SSG"),
    KT("kt"),
    Hanwha("한화"),
    Samsung("삼성"),
    KIA("KIA"),
    NC("NC"),
    Lotte("롯데"),
    LG_MBC("LG.MBC"),
    Doosan_OB("두산.OB"),
    Heroes("히어로즈"),
    SSG_SK("SSG.SK"),
    Han_Bing("한화.빙그레"),
    KIA_Haitai("KIA.해태"),
    HyunTea("현대.태평양.청보.삼미"),
    Ssangbang("쌍방울"),
    Haitai("해태"),
    Hyundai("현대"),
    Chungbo("청보"),
    Sammi("삼미"),
    MBC("MBC"),
    OB("OB"),
    Teapyoung("태평양"),
    Binggrea("빙그레"),
    SK("SK")
}

enum class FieldPosition(val idx: Int) {
    All(0),
    Pitcher(1),
    Catcher(2),
    First(3),
    Second(4),
    Third(5),
    SS(6),
    Left(7),
    Center(8),
    Right(9),
    DH(10),
    Infielder(11),
    Outfielder(12)
}

enum class BattingLocation(val idx: Int) {
    All(0),
    Right(1),
    Left(2),
    Both(3),
    RightBoth(4),
    LeftBoth(5)
}

enum class Tool(val idx: Int) {
    Batting(0),
    Pitching(1),
    Fielding(2)
}

enum class Season(val idx: Int) {
    Regular(0),
    PostSeason(1),
    KoreanSeries(2),
    PlayOff(3),
    SemiPlayOff(4),
    AllStar(5),
    Summer(6),
    WildCard(7)
}

enum class IndexCode(val title: String) {
    Year("연도"),
    GP("G"),
    AB("타수"),
    H("안타"),
    H2B("2타"),
    H3B("3타"),
    TB("루타"),
    RBI("타점"),
    SB("도루"),
    CS("도실"),
    BB("볼넷"),
    HB("사구"),
    IBB("고4"),
    SO("삼진"),
    GDP("병살"),
    SH("희타"),
    SF("희비"),
    AVG("타율"),
    OBP("출루율"),
    SLG("장타율"),
    OPS("OPS"),
    HR_P("HR%"),
    BB_P("BB%"),
    SO_P("K%"),
    BBK("BB/K"),
    IsoD("IsoD"),
    SPDSC("Spd"),
    PSN("PSN"),
    WOBA("wOBA"),
    WRC("WRC"),
    WRC27("wRC/27"),
    WRAA("wRAA"),
    WOBA_ADJ("wOBA"),
    WRC_ADJ("wRC"),
    WRC27_ADJ("wRC/27"),
    WRAA_ADJ("wRAA"),
    WRCPLUS("wRC+"),
    RAA_BT("타격"),
    RAA_SB("도루"),
    WAR_ALL_ADJ("WAR*"),
    TPA("타석")
}