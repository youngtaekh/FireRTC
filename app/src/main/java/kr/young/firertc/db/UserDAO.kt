package kr.young.firertc.db

import androidx.room.*
import kr.young.firertc.model.User

@Dao
interface UserDAO {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getUsers(): List<User>

    @Query("SELECT * FROM users where id = :id")
    fun getUser(id: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setUser(user: User)

    @Delete
    fun deleteUser(user: User)
}