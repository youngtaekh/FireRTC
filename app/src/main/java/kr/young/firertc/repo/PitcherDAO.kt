package kr.young.firertc.repo

import androidx.room.*
import kr.young.firertc.model.Pitcher

@Dao
interface PitcherDAO {

    @Query("SELECT * FROM pitchers WHERE age = :age")
    fun getPitchers(age: Int): List<Pitcher>

    @Query("SELECT * FROM pitchers WHERE name = :name AND age = :age")
    fun getPitcher(name: String, age: Int): Pitcher

    @Query("SELECT * FROM pitchers")
    fun getAll(): List<Pitcher>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setPitcher(pitcher: Pitcher)

    @Delete
    fun deletePitcher(pitcher: Pitcher)

    @Query("DELETE FROM pitchers")
    fun deleteAll()
}