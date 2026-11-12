package sa.factory.injazcareservices1.engine

import java.security.MessageDigest
import java.util.Locale
import kotlin.math.*

/**
 * EventDispatchGeofenceEngine
 * Production domain engine for VIP Influencer Event Dispatch, geodesic arrival verification,
 * venue geofencing (e.g. Riyadh Boulevard, KAFD, Red Sea Mall), and punctuality SLA audits.
 *
 * Strict zero-emoji compliance.
 */
class EventDispatchGeofenceEngine {

    companion object {
        const val EARTH_RADIUS_METERS = 6371000.0
    }

    data class VenueGeofence(
        val venueId: String,
        val venueName: String,
        val centerLat: Double,
        val centerLon: Double,
        val radiusMeters: Double
    )

    data class CreatorLocationPing(
        val creatorId: String,
        val lat: Double,
        val lon: Double,
        val timestampMs: Long = System.currentTimeMillis(),
        val batteryPct: Int = 98,
        val signalDbm: Int = -56
    )

    data class AttendanceValidationResult(
        val isInsideVenue: Boolean,
        val distanceToVenueMeters: Double,
        val bearingDegrees: Double,
        val punctualityStatus: String,
        val verificationHash: String,
        val auditNote: String
    )

    /**
     * Calculates surface distance between two coordinates in meters using the Haversine formula.
     */
    fun calculateHaversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Calculates initial forward bearing from point 1 to point 2 in degrees (0..359).
     */
    fun calculateBearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLonRad = Math.toRadians(lon2 - lon1)

        val y = sin(dLonRad) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLonRad)
        val bearingRad = atan2(y, x)
        return (Math.toDegrees(bearingRad) + 360.0) % 360.0
    }

    /**
     * Validates creator presence within event geofence and assesses arrival punctuality.
     */
    fun validateEventAttendance(
        ping: CreatorLocationPing,
        venue: VenueGeofence,
        scheduledStartTimeMs: Long,
        gracePeriodMinutes: Int = 15
    ): AttendanceValidationResult {
        val distanceMeters = calculateHaversineDistanceMeters(ping.lat, ping.lon, venue.centerLat, venue.centerLon)
        val isInside = distanceMeters <= venue.radiusMeters
        val bearing = calculateBearingDegrees(ping.lat, ping.lon, venue.centerLat, venue.centerLon)

        val diffMinutes = (ping.timestampMs - scheduledStartTimeMs) / (1000 * 60)
        val punctualityStatus = when {
            !isInside -> "[PENDING] OUTSIDE_VENUE_PERIMETER"
            diffMinutes <= 0 -> "[OK] ON_TIME_EARLY"
            diffMinutes <= gracePeriodMinutes -> "[OK] ON_TIME_GRACE_PERIOD"
            else -> "[ALERT] LATE_ARRIVAL (+${diffMinutes}m)"
        }

        // Generate SHA-256 presence receipt
        val rawAudit = "${ping.creatorId}:${venue.venueId}:$distanceMeters:$punctualityStatus:${ping.timestampMs}"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawAudit.toByteArray(Charsets.UTF_8))
        val verificationHash = hashBytes.joinToString("") { "%02X".format(it) }

        val auditNote = if (isInside) {
            "[OK] Creator ${ping.creatorId} validated inside ${venue.venueName} (${String.format(Locale.US, "%.1f", distanceMeters)}m from centroid). Status: $punctualityStatus"
        } else {
            "[INFO] Creator ${ping.creatorId} is ${String.format(Locale.US, "%.1f", distanceMeters)}m away from ${venue.venueName}. Status: $punctualityStatus"
        }

        return AttendanceValidationResult(
            isInsideVenue = isInside,
            distanceToVenueMeters = distanceMeters,
            bearingDegrees = bearing,
            punctualityStatus = punctualityStatus,
            verificationHash = verificationHash,
            auditNote = auditNote
        )
    }

    /**
     * Formats real-time telemetry frame for dispatch monitoring dashboard.
     */
    fun buildRealtimeDispatchTelemetry(
        ping: CreatorLocationPing,
        venue: VenueGeofence
    ): String {
        val distMeters = calculateHaversineDistanceMeters(ping.lat, ping.lon, venue.centerLat, venue.centerLon)
        val insideTag = if (distMeters <= venue.radiusMeters) "INSIDE" else "TRANSIT"
        return "NFC EMV L2: READY | GPS Dispatch: ${String.format(Locale.US, "%.3f", ping.lat)}N ${String.format(Locale.US, "%.3f", ping.lon)}E | Venue: ${venue.venueName} | Distance: ${String.format(Locale.US, "%.0f", distMeters)}m | State: $insideTag"
    }
}
