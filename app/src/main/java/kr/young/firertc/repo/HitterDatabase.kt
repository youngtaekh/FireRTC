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
import kr.young.firertc.util.Converter

@Database(
    entities = [Hitter::class],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 3, to = 4, spec = HitterDatabase.HitterAutoMigration::class),
        AutoMigration(from = 4, to = 5, spec = HitterDatabase.HitterAutoMigration::class),
    ]
)
abstract class HitterDatabase: RoomDatabase() {
    abstract fun hitterDao(): HitterDAO

    @DeleteColumn(tableName = "hitters", columnName = "birth")
    @RenameColumn(tableName = "hitters", fromColumnName = "intentionalBaseOnBalls", toColumnName = "intentionalWalks")
    @RenameColumn(tableName = "hitters", fromColumnName = "hitByPitched", toColumnName = "hitByPitch")
    class HitterAutoMigration: AutoMigrationSpec

    companion object {
        private var instance: HitterDatabase? = null

        @OptIn(InternalCoroutinesApi::class)
        @Synchronized
        fun getInstance(): HitterDatabase? {
            if (instance == null) {
                synchronized(HitterDatabase::class) {
                    println("setInstance")
                    instance = Room.databaseBuilder(
                        ApplicationUtil.getContext()!!.applicationContext,
                        HitterDatabase::class.java,
                        "hitter.db"
                    )
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                        .build()
                }
            }
            return instance
        }

        fun destroyInstance() {
            instance = null
        }

        private val MIGRATION_1_2 = object: Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                println("migration")
                database.execSQL("ALTER TABLE hitters ADD COLUMN birth TEXT NOT NULL default ''")
                // CREATE TABLE tableName
                // DROP TABLE tableName
                // ALTER TABLE oldTableName RENAME TO newTableName
                // ALTER
            }
        }

        private val MIGRATION_2_3 = object: Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE hitters ADD COLUMN birthYear INTEGER NOT NULL default 2023")
                database.execSQL("ALTER TABLE hitters ADD COLUMN birthMonth INTEGER NOT NULL default 1")
                database.execSQL("ALTER TABLE hitters ADD COLUMN birthDay INTEGER NOT NULL default 1")
            }
        }
    }
}