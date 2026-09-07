package app.orionmd.digitalwallet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StatementDao {
    @Query("SELECT * FROM statements ORDER BY createdAt DESC")
    fun getAll(): Flow<List<StatementEntity>>

    @Query("SELECT * FROM statements WHERE id = :id")
    suspend fun getById(id: Long): StatementEntity?

    @Insert
    suspend fun insert(entity: StatementEntity): Long

    @Update
    suspend fun update(entity: StatementEntity)

    @Delete
    suspend fun delete(entity: StatementEntity)
}
