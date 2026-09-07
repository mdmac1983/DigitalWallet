package app.orionmd.digitalwallet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getById(id: Long): ContactEntity?

    @Query("SELECT MAX(sortOrder) FROM contacts")
    suspend fun maxSortOrder(): Int?

    @Query("UPDATE contacts SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Insert
    suspend fun insert(entity: ContactEntity): Long

    @Update
    suspend fun update(entity: ContactEntity)

    @Delete
    suspend fun delete(entity: ContactEntity)
}
