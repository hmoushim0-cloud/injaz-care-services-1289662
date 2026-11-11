package sa.factory.injazcareservices1.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InjazCareDao {
    @Query("SELECT * FROM services_items_cache ORDER BY id ASC")
    fun observeAllItems(): Flow<List<InjazCareEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InjazCareEntity>)

    @Query("DELETE FROM services_items_cache")
    suspend fun clearAll()
}
