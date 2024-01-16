package kr.young.firertc.db

import androidx.room.*
import kr.young.firertc.model.Call
import kr.young.firertc.util.Config.Companion.HISTORY_PAGE_SIZE
import java.util.*

@Dao
interface CallDAO {
    @Query("SELECT * FROM calls ORDER BY createdAt DESC LIMIT :limit")
    fun getCalls(limit: Long = HISTORY_PAGE_SIZE): List<Call>

    @Query("SELECT * FROM calls WHERE createdAt > :first ORDER BY createdAt DESC LIMIT :limit")
    fun getCalls(first: Date, limit: Long = HISTORY_PAGE_SIZE): List<Call>

    @Query("SELECT * FROM calls WHERE createdAt < :last ORDER BY createdAt DESC LIMIT :limit")
    fun getAdditionCalls(last: Date, limit: Long = HISTORY_PAGE_SIZE): List<Call>

    @Query("SELECT * FROM calls where id = :id")
    fun getCall(id: String): Call

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setCalls(vararg call: Call)

    @Update
    fun updateCalls(vararg call: Call)

    @Delete
    fun deleteCall(call: Call)

    @Query("DELETE FROM calls")
    fun deleteAll()
}