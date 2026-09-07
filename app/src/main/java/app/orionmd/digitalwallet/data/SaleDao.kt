package app.orionmd.digitalwallet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getById(id: Long): SaleEntity?

    @Insert
    suspend fun insert(entity: SaleEntity): Long

    @Update
    suspend fun update(entity: SaleEntity)

    @Delete
    suspend fun delete(entity: SaleEntity)
}
