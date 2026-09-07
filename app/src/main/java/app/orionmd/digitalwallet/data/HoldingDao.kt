package app.orionmd.digitalwallet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HoldingDao {
    @Query("SELECT * FROM holdings ORDER BY createdAt DESC")
    fun getAll(): Flow<List<HoldingEntity>>

    @Query("SELECT * FROM holdings WHERE id = :id")
    suspend fun getById(id: Long): HoldingEntity?

    @Insert
    suspend fun insert(entity: HoldingEntity): Long

    @Update
    suspend fun update(entity: HoldingEntity)

    @Delete
    suspend fun delete(entity: HoldingEntity)
}
