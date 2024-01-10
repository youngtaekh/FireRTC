package kr.young.firertc.db

import androidx.room.*
import kr.young.firertc.model.Chat
import java.util.*

@Dao
interface ChatDAO {
    @Query("SELECT * FROM chats ORDER BY modifiedAt DESC LIMIT 9999")
    fun getChats(): List<Chat>

    @Query("SELECT * FROM chats where id = :id")
    fun getChat(id: String): Chat

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setChats(vararg chat: Chat)

    @Update
    fun updateChats(vararg chat: Chat)

    @Query("UPDATE chats SET lastMessage = :lastMessage, lastSequence = :lastSequence WHERE id = :id")
    fun updateLast(id: String, lastMessage: String, lastSequence: Long)

    @Delete
    fun deleteChat(chat: Chat)

    @Query("DELETE FROM chats")
    fun deleteAll()
}