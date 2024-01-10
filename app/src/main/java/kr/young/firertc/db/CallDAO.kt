package kr.young.firertc.db

import androidx.room.*
import kr.young.firertc.model.Call
import java.util.*

@Dao
interface CallDAO {
    @Query("SELECT * FROM calls ORDER BY createdAt DESC LIMIT 50")
    fun getCalls(): List<Call>

    @Query("SELECT * FROM calls WHERE createdAt > :first ORDER BY createdAt DESC")
    fun getCalls(first: Date): List<Call>

    @Query("SELECT * FROM calls WHERE createdAt < :last ORDER BY createdAt DESC LIMIT 1000")
    fun getAdditionCalls(last: Date): List<Call>

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