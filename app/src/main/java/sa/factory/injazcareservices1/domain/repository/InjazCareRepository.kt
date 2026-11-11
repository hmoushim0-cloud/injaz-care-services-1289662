package sa.factory.injazcareservices1.domain.repository

import sa.factory.injazcareservices1.model.InjazCareItem
import sa.factory.injazcareservices1.model.InjazCareOrder
import sa.factory.injazcareservices1.model.InjazCareTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface InjazCareRepository {
    val catalogFlow: Flow<List<InjazCareItem>>
    val activeCart: StateFlow<List<InjazCareItem>>
    val telemetryFlow: Flow<InjazCareTelemetry>
    
    fun addToCart(item: InjazCareItem)
    fun removeFromCart(item: InjazCareItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): InjazCareOrder
    suspend fun refreshCatalog()
}
