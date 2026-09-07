package app.orionmd.digitalwallet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords WHERE id = :id")
    suspend fun getById(id: Long): PasswordEntity?

    @Query("SELECT MAX(sortOrder) FROM passwords")
    suspend fun maxSortOrder(): Int?

    @Query("UPDATE passwords SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Insert
    suspend fun insert(entity: PasswordEntity): Long

    @Update
    suspend fun update(entity: PasswordEntity)

    @Delete
    suspend fun delete(entity: PasswordEntity)
}
