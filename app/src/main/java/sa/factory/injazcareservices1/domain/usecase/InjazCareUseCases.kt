package sa.factory.injazcareservices1.domain.usecase

import sa.factory.injazcareservices1.domain.repository.InjazCareRepository
import sa.factory.injazcareservices1.model.InjazCareItem
import sa.factory.injazcareservices1.model.InjazCareOrder
import sa.factory.injazcareservices1.model.InjazCareTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: InjazCareRepository) {
    operator fun invoke(): Flow<List<InjazCareItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: InjazCareRepository) {
    val cart = repository.activeCart
    fun add(item: InjazCareItem) = repository.addToCart(item)
    fun remove(item: InjazCareItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: InjazCareRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): InjazCareOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: InjazCareRepository) {
    operator fun invoke(): Flow<InjazCareTelemetry> = repository.telemetryFlow
}
