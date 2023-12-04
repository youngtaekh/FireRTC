package kr.young.firertc.vm

import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.common.UtilLog.Companion.i
import kr.young.firertc.fcm.HtmlParse
import kr.young.firertc.fcm.IndexCode
import kr.young.firertc.fcm.StatizClient
import kr.young.firertc.fcm.Tool
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StatizViewModel {
    companion object {
        fun getPlayerInfo(
            tool: Int = Tool.Batting.idx,
            sort1: String = IndexCode.WAR_ALL_ADJ.title, sort2: String = IndexCode.TPA.title,
            ageStart: Int = 17, ageEnd: Int = 51
        ) {
            StatizClient.getApiService().getPlayer(tool = tool, sort1 = sort1, sort2 = sort2, ageStart = ageStart, ageEnd = ageEnd)
                .enqueue(object: Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            i(TAG, "getInfo Success ${call.request()}")
                            when (tool) {
                                Tool.Batting.idx -> HtmlParse.hitter(response.body()!!.string(), ageStart)
                                Tool.Pitching.idx -> HtmlParse.pitcher(response.body()!!.string(), ageStart)
                                Tool.Fielding.idx -> HtmlParse.hitter(response.body()!!.string(), ageStart)
                            }
                        } else {
                            d(TAG, "getInfo not Success")
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        e(TAG, "getInfo Failure", t)
                    }

                })
        }

        fun getSchedule() {
            StatizClient.getApiService().getSchedule(month = 11, year = 2023)
                .enqueue(object: Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            i(TAG, "getInfo Success ${call.request()}")
                            HtmlParse.schedule(response.body()!!.string())
                        } else {
                            d(TAG, "getInfo not Success")
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        e(TAG, "getInfo Failure", t)
                    }

                })
        }

        private const val TAG = "StatizViewModel"
    }
}