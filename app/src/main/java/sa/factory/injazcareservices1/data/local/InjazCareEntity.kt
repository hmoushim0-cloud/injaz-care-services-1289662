package sa.factory.injazcareservices1.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import sa.factory.injazcareservices1.model.InjazCareItem

@Entity(tableName = "services_items_cache")
data class InjazCareEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priceSar: Double,
    val badge: String,
    val category: String,
    val status: String
) {
    fun toDomain(): InjazCareItem = InjazCareItem(
        id = id,
        title = title,
        description = description,
        priceSar = priceSar,
        badge = badge,
        category = category,
        status = status
    )
}
