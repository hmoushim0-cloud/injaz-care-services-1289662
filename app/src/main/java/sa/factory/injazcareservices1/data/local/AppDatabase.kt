package sa.factory.injazcareservices1.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [InjazCareEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun servicesDao(): InjazCareDao
}
