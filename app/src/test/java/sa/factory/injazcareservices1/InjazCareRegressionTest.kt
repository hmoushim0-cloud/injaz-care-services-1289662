package sa.factory.injazcareservices1

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import sa.factory.injazcareservices1.data.repository.InjazCareRepositoryImpl
import sa.factory.injazcareservices1.engine.CreatorEscrowBillingEngine
import sa.factory.injazcareservices1.engine.CreatorEscrowBillingEngine.CampaignContract
import sa.factory.injazcareservices1.engine.CreatorEscrowBillingEngine.CampaignMilestone
import sa.factory.injazcareservices1.engine.EventDispatchGeofenceEngine
import sa.factory.injazcareservices1.engine.EventDispatchGeofenceEngine.CreatorLocationPing
import sa.factory.injazcareservices1.engine.EventDispatchGeofenceEngine.VenueGeofence
import sa.factory.injazcareservices1.hardware.HardwareSensorDriver
import sa.factory.injazcareservices1.model.InjazCareItem

class InjazCareRegressionTest {

    // 1. Checkout Calculation & 15% VAT
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

    // 2. ISO 7816-4 APDU SELECT AID command generation
    @Test
    fun testMadaAidSelectApduFormatting() {
        val engine = CreatorEscrowBillingEngine()
        val apdu = engine.buildMadaAidSelectApdu()

        assertEquals(0x00.toByte(), apdu[0]) // CLA
        assertEquals(0xA4.toByte(), apdu[1]) // INS: SELECT
        assertEquals(0x04.toByte(), apdu[2]) // P1
        assertEquals(0x00.toByte(), apdu[3]) // P2
        assertEquals(7.toByte(), apdu[4])    // Mada AID length is 7 bytes
        assertEquals(0x00.toByte(), apdu[apdu.size - 1]) // Le
    }

    // 3. Financial settlement breakdown (5% platform fee + 15% VAT on fee)
    @Test
    fun testSettlementBreakdownAccounting() {
        val engine = CreatorEscrowBillingEngine()
        val breakdown = engine.calculateSettlementBreakdown(1000.0)

        assertEquals(1000.0, breakdown.grossDisbursementSar, 0.001)
        assertEquals(50.0, breakdown.platformFeeSar, 0.001)      // 5% of 1000
        assertEquals(7.5, breakdown.vatOnPlatformFeeSar, 0.001)  // 15% of 50
        assertEquals(942.5, breakdown.netCreatorPayoutSar, 0.001) // 1000 - 57.5
    }

    // 4. SAMA Mada Contactless Floor Limit: <= 300 SAR does NOT require CVM
    @Test
    fun testSamaMadaFloorLimitNoPinBelow300() {
        val engine = CreatorEscrowBillingEngine()
        assertFalse(engine.evaluateCvmRequirement(150.0))
        assertFalse(engine.evaluateCvmRequirement(300.00))
    }

    // 5. SAMA Mada Contactless Floor Limit: > 300 SAR REQUIRES CVM
    @Test
    fun testSamaMadaFloorLimitPinRequiredAbove300() {
        val engine = CreatorEscrowBillingEngine()
        assertTrue(engine.evaluateCvmRequirement(300.01))
        assertTrue(engine.evaluateCvmRequirement(1200.0))
    }

    // 6. Milestone disbursement rejected when sponsor approval is missing
    @Test
    fun testMilestoneDisbursementRejectionWhenNotApproved() {
        val engine = CreatorEscrowBillingEngine()
        val contract = CampaignContract(
            contractId = "CTR-8801",
            sponsorId = "SPONSOR-ALMARAI",
            creatorId = "CREATOR-NAJM",
            totalBudgetSar = 5000.0,
            milestones = listOf(
                CampaignMilestone("M1", "Reel Launch", 2500.0, isDeliverableUploaded = true, isSponsorApproved = false)
            )
        )

        val result = engine.releaseMilestoneDisbursement(contract, "M1", "SECRET_KEY_123")
        assertFalse(result.isSuccess)
        assertEquals("ESCROW_LOCKED", result.paymentMethod)
        assertTrue(result.auditNote.contains("[REJECT]"))
    }

    // 7. Successful milestone disbursement with HMAC-SHA256 receipt
    @Test
    fun testMilestoneDisbursementSuccessWithHmacSha256Receipt() {
        val engine = CreatorEscrowBillingEngine()
        val contract = CampaignContract(
            contractId = "CTR-8802",
            sponsorId = "SPONSOR-STC",
            creatorId = "CREATOR-SAUDI_TECH",
            totalBudgetSar = 8000.0,
            milestones = listOf(
                CampaignMilestone("M1", "Coverage Day 1", 4000.0, isDeliverableUploaded = true, isSponsorApproved = true)
            )
        )

        val result = engine.releaseMilestoneDisbursement(contract, "M1", "SECURE_HMAC_KEY_999")
        assertTrue(result.isSuccess)
        assertEquals(3770.0, result.financialBreakdown.netCreatorPayoutSar, 0.01)
        assertTrue(result.cvmRequired) // 3770 SAR > 300 SAR
        assertNotNull(result.cryptographicReceipt)
        assertEquals(64, result.cryptographicReceipt.length) // SHA-256 is 64 hex chars
    }

    // 8. Haversine distance and bearing calculation
    @Test
    fun testHaversineDistanceAndBearingToRiyadhBoulevard() {
        val engine = EventDispatchGeofenceEngine()
        // Coordinates: Riyadh Boulevard City vs Kingdom Centre
        val boulevardLat = 24.7692
        val boulevardLon = 46.6022
        val kingdomLat = 24.7114
        val kingdomLon = 46.6744

        val distanceMeters = engine.calculateHaversineDistanceMeters(kingdomLat, kingdomLon, boulevardLat, boulevardLon)
        val bearing = engine.calculateBearingDegrees(kingdomLat, kingdomLon, boulevardLat, boulevardLon)

        // Distance should be approximately 9.5 - 11.0 km (9500 - 11000m)
        assertTrue(distanceMeters in 9000.0..11500.0)
        // Bearing is North-West (roughly 300 - 335 degrees)
        assertTrue(bearing in 295.0..340.0)
    }

    // 9. Geofence presence detection inside event venue
    @Test
    fun testEventAttendancePresenceValidationInsideGeofence() {
        val engine = EventDispatchGeofenceEngine()
        val venue = VenueGeofence(
            venueId = "VENUE-BLVD-01",
            venueName = "Boulevard Riyadh City Main Arena",
            centerLat = 24.7692,
            centerLon = 46.6022,
            radiusMeters = 600.0
        )

        val pingInside = CreatorLocationPing("CR-01", 24.7695, 46.6020)
        val resultInside = engine.validateEventAttendance(pingInside, venue, System.currentTimeMillis())

        assertTrue(resultInside.isInsideVenue)
        assertTrue(resultInside.distanceToVenueMeters < 600.0)
        assertTrue(resultInside.punctualityStatus.contains("[OK]"))
        assertEquals(64, resultInside.verificationHash.length)
    }

    // 10. Late arrival assessment & telemetry packet formatting
    @Test
    fun testEventAttendancePunctualityAndRealtimeTelemetry() {
        val engine = EventDispatchGeofenceEngine()
        val venue = VenueGeofence(
            venueId = "VENUE-KAFD-01",
            venueName = "KAFD Conference Hall",
            centerLat = 24.7675,
            centerLon = 46.6432,
            radiusMeters = 400.0
        )

        val scheduledTime = System.currentTimeMillis() - (45 * 60 * 1000) // scheduled 45 mins ago
        val pingLate = CreatorLocationPing("CR-02", 24.7676, 46.6431, System.currentTimeMillis())

        val result = engine.validateEventAttendance(pingLate, venue, scheduledTime, gracePeriodMinutes = 15)
        assertTrue(result.isInsideVenue)
        assertTrue(result.punctualityStatus.contains("[ALERT] LATE_ARRIVAL"))

        val telemetryString = engine.buildRealtimeDispatchTelemetry(pingLate, venue)
        assertTrue(telemetryString.contains("NFC EMV L2: READY"))
        assertTrue(telemetryString.contains("KAFD Conference Hall"))
        assertTrue(telemetryString.contains("State: INSIDE"))
    }
}
