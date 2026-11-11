package sa.factory.injazcareservices1.di

import sa.factory.injazcareservices1.data.repository.InjazCareRepositoryImpl
import sa.factory.injazcareservices1.domain.repository.InjazCareRepository
import sa.factory.injazcareservices1.domain.usecase.*
import sa.factory.injazcareservices1.hardware.HardwareSensorDriver
import sa.factory.injazcareservices1.ui.viewmodel.InjazCareViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: InjazCareRepository = InjazCareRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): InjazCareViewModel = InjazCareViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
