package sa.factory.injazcareservices1

import sa.factory.injazcareservices1.data.repository.InjazCareRepositoryImpl
import sa.factory.injazcareservices1.hardware.HardwareSensorDriver
import sa.factory.injazcareservices1.model.InjazCareItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class InjazCareRegressionTest {

    @Test
    fun testCheckoutCalculationAndTax() = runBlocking {
        val sensorDriver = HardwareSensorDriver()
        val repository = InjazCareRepositoryImpl(sensorDriver)

        val item1 = InjazCareItem("t1", "خدمة تجريبية أ", "الوصف", 100.0, "معتمد", "عام")
        val item2 = InjazCareItem("t2", "خدمة تجريبية ب", "الوصف", 200.0, "معتمد", "عام")

        repository.addToCart(item1)
        repository.addToCart(item2)

        val order = repository.checkout("Mada EMV")

        assertEquals(300.0, order.subtotalSar, 0.01)
        assertEquals(45.0, order.vatSar, 0.01)
        assertEquals(345.0, order.totalSar, 0.01)
        assertEquals(0, repository.activeCart.value.size)
    }
}
