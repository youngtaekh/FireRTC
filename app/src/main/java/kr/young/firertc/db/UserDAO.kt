package kr.young.firertc.db

import androidx.room.*
import kr.young.firertc.model.User

@Dao
interface UserDAO {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getUsers(): List<User>

    @Query("SELECT * FROM users where id = :id")
    fun getUser(id: String?): User?

    @Query("SELECT * FROM users where id IN (:id)")
    fun getUsers(id: List<String>): List<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setUsers(vararg user: User)

    @Update
    fun updateUsers(vararg user: User)

    @Delete
    fun deleteUser(user: User)
}