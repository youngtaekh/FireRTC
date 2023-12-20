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
    fun setChat(chat: Chat)

    @Delete
    fun deleteChat(chat: Chat)

    @Query("DELETE FROM chats")
    fun deleteAll()
}