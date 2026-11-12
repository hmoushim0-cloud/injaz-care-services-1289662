# منصة نجم — إدارة شاملة لمحتوى المؤثرين (injaz-care-services-1289662)
> إدارة محتوى ذكية مع تكامل NFC و GPS لتعزيز النمو

- **Project ID**: #1289662
- **Commercial Archetype**: services
- **Bound Hardware Peripheral**: Mada Contactless EMV L2 NFC + GPS Dispatch
- **Package Name**: sa.factory.injazcareservices1
- **Zero-Emoji Invariant**: Strictly Enforced (100% Vector Icons and Clean Typography)

---

## Production Domain Engines (Iteration 92)

### 1. Creator Campaign Escrow & Billing Engine (`CreatorEscrowBillingEngine.kt`)
- **SAMA-Compliant Escrow Contracts**:
  - Secure milestone locking and disbursement validation for influencer marketing agreements.
  - Transparent accounting breakdown: 5% platform service fee, 15% ZATCA VAT on platform fee, and net creator payout.
  - SAMA Contactless Floor Limit rule enforcement: payments <= 300.00 SAR processed without PIN; transactions > 300.00 SAR require mandatory Cardholder Verification Method (CVM: On-Device PIN / Biometrics).
- **ISO 7816-4 APDU Integration**:
  - `SELECT AID` command generator for Saudi Mada primary scheme (`A0000002281010`).
- **Cryptographic Audit Proof**:
  - HMAC-SHA256 signature generation binding sponsor, creator, contract ID, milestone index, and net disbursement.

### 2. Event Dispatch & Geofence Verification Engine (`EventDispatchGeofenceEngine.kt`)
- **Geodesic Haversine Navigation**:
  - High-precision distance calculation in meters and compass navigation bearing in degrees (0..359).
- **VIP Venue Geofencing**:
  - Real-time perimeter monitoring for major entertainment & retail venues (e.g. Boulevard Riyadh City, KAFD Conference Center).
- **Attendance & Punctuality SLA Verification**:
  - Categorizes arrival status: early, on-time with 15-minute grace period, or late arrival alert.
  - SHA-256 tamper-evident digital attendance receipt generation.
  - Formatted live dispatch telemetry for mobile operations dashboards.

---

## Verification & Unit Testing

- 10 comprehensive unit tests in `InjazCareRegressionTest.kt`:
  1. `testCheckoutCalculationAndTax`: Cart totals, 15% VAT, and SAR settlement.
  2. `testMadaAidSelectApduFormatting`: ISO 7816-4 Mada AID selection APDU.
  3. `testSettlementBreakdownAccounting`: 5% platform fee and 15% VAT deduction.
  4. `testSamaMadaFloorLimitNoPinBelow300`: Verification of no-PIN threshold under 300 SAR.
  5. `testSamaMadaFloorLimitPinRequiredAbove300`: Mandatory CVM verification above 300 SAR.
  6. `testMilestoneDisbursementRejectionWhenNotApproved`: Escrow safety locking when sponsor approval is missing.
  7. `testMilestoneDisbursementSuccessWithHmacSha256Receipt`: Successful payout and HMAC-SHA256 receipt generation.
  8. `testHaversineDistanceAndBearingToRiyadhBoulevard`: Geodesic coordinate calculation across Riyadh venues.
  9. `testEventAttendancePresenceValidationInsideGeofence`: Presence detection inside event geofence radius.
  10. `testEventAttendancePunctualityAndRealtimeTelemetry`: Punctuality assessment and real-time dispatch telemetry string formatting.
