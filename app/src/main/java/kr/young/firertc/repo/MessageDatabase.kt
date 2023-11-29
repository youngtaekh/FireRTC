package kr.young.firertc.repo

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized
import kr.young.common.ApplicationUtil
import kr.young.firertc.model.Message
import kr.young.firertc.util.Converter

@Database(entities = [Message::class], version = 1)
@TypeConverters(Converter::class)
abstract class MessageDatabase: RoomDatabase() {
    abstract fun messageDao(): MessageDAO

    companion object {
        private var instance: MessageDatabase? = null

        @OptIn(InternalCoroutinesApi::class)
        @Synchronized
        fun getInstance(): MessageDatabase? {
            if (instance == null) {
                synchronized(MessageDatabase::class) {
                    instance = Room.databaseBuilder(ApplicationUtil.getContext()!!.applicationContext, MessageDatabase::class.java, "message.db").build()
                }
            }
            return instance
        }

        fun destroyInstance() {
            instance = null
        }
    }
}