package kr.young.firertc.db

import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized
import kr.young.common.ApplicationUtil
import kr.young.firertc.model.Call
import kr.young.firertc.model.Chat
import kr.young.firertc.model.Message
import kr.young.firertc.model.User
import kr.young.firertc.util.Converter

@Database(
    entities = [Call::class, Chat::class, Message::class, User::class],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 3, to = 4, spec = AppRoomDatabase.AppRoomAutoMigration::class),
    ]
)
@TypeConverters(Converter::class)
abstract class AppRoomDatabase: RoomDatabase() {
    abstract fun callDao(): CallDAO
    abstract fun chatDao(): ChatDAO
    abstract fun messageDao(): MessageDAO
    abstract fun userDao(): UserDAO

//    @DeleteColumn(tableName = "", columnName = "")
//    @RenameColumn(tableName = "", fromColumnName = "", toColumnName = "")
    @DeleteColumn(tableName = "messages", columnName = "timeFlag")
    class AppRoomAutoMigration: AutoMigrationSpec

    companion object {
        private var instance: AppRoomDatabase? = null

        @OptIn(InternalCoroutinesApi::class)
        @Synchronized
        fun getInstance(): AppRoomDatabase? {
            if (instance == null) {
                synchronized(AppRoomDatabase::class) {
                    instance = Room.databaseBuilder(
                        ApplicationUtil.getContext()!!.applicationContext,
                        AppRoomDatabase::class.java,
                        "fireRTC.db"
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_4_5).build()
                }
            }
            return instance
        }

        fun destroyInstance() {
            instance = null
        }

        private val MIGRATION_4_5 = object: Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE messages ADD COLUMN timeFlag INTEGER NOT NULL default 1")
            }
        }

        private val MIGRATION_2_3 = object: Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chats ADD COLUMN localTitle TEXT NOT NULL default ''")
            }
        }

        private val MIGRATION_1_2 = object: Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                println("migration")
                database.execSQL("CREATE TABLE chats (id TEXT NOT NULL, title TEXT, " +
                        "isGroup INTEGER NOT NULL, lastMessage TEXT NOT NULL, lastSequence INTEGER NOT NULL, " +
                        "modifiedAt INTEGER, createdAt INTEGER, participants TEXT NOT NULL, PRIMARY KEY(id))")
                // CREATE TABLE tableName
                // DROP TABLE tableName
                // ALTER TABLE oldTableName RENAME TO newTableName
                // ALTER TABLE tableName ADD COLUMN columnName TEXT NOT NULL default ''
            }
        }
    }
}