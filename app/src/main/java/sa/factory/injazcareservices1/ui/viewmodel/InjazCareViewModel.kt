package sa.factory.injazcareservices1.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sa.factory.injazcareservices1.domain.usecase.*
import sa.factory.injazcareservices1.model.InjazCareItem
import sa.factory.injazcareservices1.model.InjazCareOrder
import sa.factory.injazcareservices1.model.InjazCareTelemetry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InjazCareUiState(
    val catalog: List<InjazCareItem> = emptyList(),
    val filteredCatalog: List<InjazCareItem> = emptyList(),
    val selectedCategory: String = "الكل",
    val cart: List<InjazCareItem> = emptyList(),
    val telemetry: InjazCareTelemetry? = null,
    val lastOrder: InjazCareOrder? = null,
    val isCheckingOut: Boolean = false,
    val activeTab: String = "home"
)

class InjazCareViewModel(
    private val getCatalogUseCase: GetCatalogUseCase,
    private val manageCartUseCase: ManageCartUseCase,
    private val processCheckoutUseCase: ProcessCheckoutUseCase,
    private val observeHardwareTelemetryUseCase: ObserveHardwareTelemetryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InjazCareUiState())
    val uiState: StateFlow<InjazCareUiState> = _uiState.asStateFlow()

    init {
        // Observe Catalog
        getCatalogUseCase().onEach { list ->
            _uiState.update { it.copy(catalog = list, filteredCatalog = list) }
        }.launchIn(viewModelScope)

        // Observe Cart
        manageCartUseCase.cart.onEach { cartItems ->
            _uiState.update { it.copy(cart = cartItems) }
        }.launchIn(viewModelScope)

        // Observe Telemetry
        observeHardwareTelemetryUseCase().onEach { telem ->
            _uiState.update { it.copy(telemetry = telem) }
        }.launchIn(viewModelScope)
    }

    fun selectCategory(category: String) {
        _uiState.update { state ->
            val filtered = if (category == "الكل") state.catalog else state.catalog.filter { it.category == category }
            state.copy(selectedCategory = category, filteredCatalog = filtered)
        }
    }

    fun switchTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun addToCart(item: InjazCareItem) = manageCartUseCase.add(item)
    fun removeFromCart(item: InjazCareItem) = manageCartUseCase.remove(item)

    fun checkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingOut = true) }
            val order = processCheckoutUseCase()
            _uiState.update { it.copy(isCheckingOut = false, lastOrder = order, activeTab = "checkout") }
        }
    }
}
