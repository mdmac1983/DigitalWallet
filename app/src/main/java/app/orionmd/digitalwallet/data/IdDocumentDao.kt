package app.orionmd.digitalwallet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IdDocumentDao {
    @Query("SELECT * FROM id_documents ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<IdDocumentEntity>>

    @Query("SELECT * FROM id_documents WHERE id = :id")
    suspend fun getById(id: Long): IdDocumentEntity?

    @Query("SELECT MAX(sortOrder) FROM id_documents")
    suspend fun maxSortOrder(): Int?

    @Query("UPDATE id_documents SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Insert
    suspend fun insert(entity: IdDocumentEntity): Long

    @Update
    suspend fun update(entity: IdDocumentEntity)

    @Delete
    suspend fun delete(entity: IdDocumentEntity)
}
