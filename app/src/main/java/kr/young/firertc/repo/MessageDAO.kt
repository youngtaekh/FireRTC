package kr.young.firertc.repo

import androidx.room.*
import kr.young.firertc.model.Message

@Dao
interface MessageDAO {
    @Query("SELECT * FROM messages where chatId = :chatId AND sequence > :min AND sequence < :max ORDER BY sequence DESC LIMIT 100")
    fun getMessages(chatId: String, min: Long = -1L, max: Long = 9_223_372_036_854_775_807): List<Message>

    @Query("SELECT * FROM messages where chatId = :chatId ORDER BY sequence DESC LIMIT 1")
    fun getLastMessage(chatId: String): Message

    @Query("SELECT * FROM messages where id = :id")
    fun getMessage(id: String): Message

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setMessage(message: Message)

    @Delete
    fun deleteMessage(message: Message)
}