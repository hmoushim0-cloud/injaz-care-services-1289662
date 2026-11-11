package sa.factory.injazcareservices1.data.remote

import sa.factory.injazcareservices1.model.InjazCareItem
import sa.factory.injazcareservices1.model.InjazCareOrder

interface InjazCareApiService {
    suspend fun fetchCatalog(): List<InjazCareItem>
    suspend fun submitOrder(order: InjazCareOrder): Boolean
}
