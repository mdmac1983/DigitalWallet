package app.orionmd.digitalwallet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: Long): CardEntity?

    @Query("SELECT MAX(sortOrder) FROM cards")
    suspend fun maxSortOrder(): Int?

    @Query("UPDATE cards SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Insert
    suspend fun insert(entity: CardEntity): Long

    @Update
    suspend fun update(entity: CardEntity)

    @Delete
    suspend fun delete(entity: CardEntity)
}
