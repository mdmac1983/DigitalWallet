package app.orionmd.digitalwallet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM finances ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<FinanceEntity>>

    @Query("SELECT * FROM finances WHERE id = :id")
    suspend fun getById(id: Long): FinanceEntity?

    @Insert
    suspend fun insert(entity: FinanceEntity): Long

    @Update
    suspend fun update(entity: FinanceEntity)

    @Delete
    suspend fun delete(entity: FinanceEntity)
}
