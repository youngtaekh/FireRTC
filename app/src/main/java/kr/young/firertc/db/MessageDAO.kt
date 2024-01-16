package kr.young.firertc.db

import androidx.room.*
import kr.young.firertc.model.Message
import kr.young.firertc.util.Config.Companion.MAX_LONG
import kr.young.firertc.util.Config.Companion.MESSAGE_PAGE_SIZE
import kr.young.firertc.util.Config.Companion.MIN_LONG

@Dao
interface MessageDAO {
    @Query("SELECT * FROM messages where chatId = :chatId AND " +
            "sequence > :min AND sequence < :max " +
            "ORDER BY sequence DESC LIMIT :limit")
    fun getMessages(
        chatId: String,
        min: Long = MIN_LONG,
        max: Long = MAX_LONG,
        limit: Long = MESSAGE_PAGE_SIZE
    ): List<Message>

    @Query("SELECT * FROM messages where chatId = :chatId ORDER BY sequence DESC LIMIT 1")
    fun getLastMessage(chatId: String): Message?

    @Query("SELECT * FROM messages where id = :id")
    fun getMessage(id: String): Message

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setMessages(vararg message: Message)

    @Update
    fun updateMessages(vararg message: Message)

    @Query("DELETE FROM messages where sequence >= :from AND sequence <= :to")
    fun deleteMessages(from: Long, to: Long)

    @Delete
    fun delete(message: Message)
}