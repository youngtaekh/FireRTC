package kr.young.firertc.repo

import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized
import kr.young.common.ApplicationUtil
import kr.young.firertc.model.Hitter
import kr.young.firertc.model.Message
import kr.young.firertc.model.Pitcher
import kr.young.firertc.util.Converter

@Database(
    entities = [Pitcher::class],
    version = 1,
    exportSchema = true,
)
abstract class PitcherDatabase: RoomDatabase() {
    abstract fun pitcherDao(): PitcherDAO

    companion object {
        private var instance: PitcherDatabase? = null

        @OptIn(InternalCoroutinesApi::class)
        @Synchronized
        fun getInstance(): PitcherDatabase? {
            if (instance == null) {
                synchronized(PitcherDatabase::class) {
                    instance = Room.databaseBuilder(
                        ApplicationUtil.getContext()!!.applicationContext,
                        PitcherDatabase::class.java,
                        "pitcher.db"
                    )
                        .build()
                }
            }
            return instance
        }

        fun destroyInstance() {
            instance = null
        }
    }
}