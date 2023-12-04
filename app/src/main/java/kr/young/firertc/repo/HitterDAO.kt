package kr.young.firertc.repo

import androidx.room.*
import kr.young.firertc.model.Hitter

@Dao
interface HitterDAO {
//    @Query("SELECT * FROM hitters where chatId = :chatId AND sequence > :min AND sequence < :max ORDER BY sequence DESC LIMIT 100")
//    fun getMessages(chatId: String, min: Long = -1L, max: Long = 9_223_372_036_854_775_807): List<Message>
//
//    @Query("SELECT * FROM messages where chatId = :chatId ORDER BY sequence DESC LIMIT 1")
//    fun getLastMessage(chatId: String): Message
//
//    @Query("SELECT * FROM messages where id = :id")
//    fun getMessage(id: String): Message

    @Query("SELECT * FROM hitters WHERE age = :age")
    fun getHitters(age: Int): List<Hitter>

    @Query("SELECT * FROM hitters WHERE name = :name AND age = :age")
    fun getHitter(name: String, age: Int): Hitter

    @Query("SELECT * FROM hitters")
    fun getAll(): List<Hitter>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setHitter(hitter: Hitter)

    @Delete
    fun deleteHitter(hitter: Hitter)

    @Query("DELETE FROM hitters")
    fun deleteAll()
}