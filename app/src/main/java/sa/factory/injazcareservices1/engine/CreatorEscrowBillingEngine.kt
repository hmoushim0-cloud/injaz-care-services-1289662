package sa.factory.injazcareservices1.engine

import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * CreatorEscrowBillingEngine
 * Production domain engine for SAMA-compliant creator campaign escrow contracts,
 * milestone lock/release mechanisms, ZATCA 15% VAT and 5% platform commission accounting,
 * and Mada Contactless EMV Level 2 instant payout settlement.
 *
 * Strict zero-emoji compliance.
 */
class CreatorEscrowBillingEngine {

    companion object {
        const val SAMA_MADA_NO_PIN_LIMIT_SAR = 300.00
        const val MADA_PRIMARY_AID = "A0000002281010"
        const val PLATFORM_FEE_RATE = 0.05
        const val VAT_RATE = 0.15
    }

    data class CampaignMilestone(
        val milestoneId: String,
        val description: String,
        val allocationSar: Double,
        val isDeliverableUploaded: Boolean = false,
        val isSponsorApproved: Boolean = false,
        val isDisbursed: Boolean = false
    )

    data class CampaignContract(
        val contractId: String,
        val sponsorId: String,
        val creatorId: String,
        val totalBudgetSar: Double,
        val milestones: List<CampaignMilestone>,
        val contractStatus: String = "ACTIVE_ESCROW"
    )

    data class FinancialSettlementBreakdown(
        val grossDisbursementSar: Double,
        val platformFeeSar: Double,
        val vatOnPlatformFeeSar: Double,
        val netCreatorPayoutSar: Double,
        val totalDebitFromEscrowSar: Double
    )

    data class DisbursementResult(
        val isSuccess: Boolean,
        val milestoneId: String,
        val financialBreakdown: FinancialSettlementBreakdown,
        val paymentMethod: String,
        val cvmRequired: Boolean,
        val cryptographicReceipt: String,
        val auditNote: String
    )

    /**
     * Calculates financial breakdown for a campaign payout milestone:
     * - Gross amount released from escrow
     * - 5% platform fee deduction
     * - 15% ZATCA VAT on platform fee
     * - Net payout credited to creator
     */
    fun calculateSettlementBreakdown(milestoneAmountSar: Double): FinancialSettlementBreakdown {
        val platformFee = milestoneAmountSar * PLATFORM_FEE_RATE
        val vatOnFee = platformFee * VAT_RATE
        val netPayout = milestoneAmountSar - (platformFee + vatOnFee)
        return FinancialSettlementBreakdown(
            grossDisbursementSar = milestoneAmountSar,
            platformFeeSar = platformFee,
            vatOnPlatformFeeSar = vatOnFee,
            netCreatorPayoutSar = netPayout,
            totalDebitFromEscrowSar = milestoneAmountSar
        )
    }

    /**
     * Evaluates SAMA contactless CVM requirements.
     * Disbursements <= 300 SAR tap-to-pay are no-PIN.
     * Disbursements > 300 SAR require biometric or PIN confirmation.
     */
    fun evaluateCvmRequirement(amountSar: Double): Boolean {
        return amountSar > SAMA_MADA_NO_PIN_LIMIT_SAR
    }

    /**
     * Formats ISO 7816-4 APDU SELECT command for Mada Contactless AID.
     */
    fun buildMadaAidSelectApdu(): ByteArray {
        val aidHex = MADA_PRIMARY_AID
        val aidBytes = hexToBytes(aidHex)
        val apdu = ByteArray(5 + aidBytes.size + 1)
        apdu[0] = 0x00.toByte() // CLA
        apdu[1] = 0xA4.toByte() // INS: SELECT
        apdu[2] = 0x04.toByte() // P1: Select by name
        apdu[3] = 0x00.toByte() // P2: First occurrence
        apdu[4] = aidBytes.size.toByte() // Lc
        System.arraycopy(aidBytes, 0, apdu, 5, aidBytes.size)
        apdu[apdu.size - 1] = 0x00.toByte() // Le
        return apdu
    }

    /**
     * Executes milestone escrow release with HMAC-SHA256 audit proof generation.
     */
    fun releaseMilestoneDisbursement(
        contract: CampaignContract,
        milestoneId: String,
        sponsorSigningKey: String
    ): DisbursementResult {
        val milestone = contract.milestones.firstOrNull { it.milestoneId == milestoneId }
            ?: return DisbursementResult(
                isSuccess = false,
                milestoneId = milestoneId,
                financialBreakdown = calculateSettlementBreakdown(0.0),
                paymentMethod = "NONE",
                cvmRequired = false,
                cryptographicReceipt = "",
                auditNote = "[ERR] Milestone $milestoneId not found in contract ${contract.contractId}"
            )

        if (!milestone.isDeliverableUploaded) {
            return DisbursementResult(
                isSuccess = false,
                milestoneId = milestoneId,
                financialBreakdown = calculateSettlementBreakdown(milestone.allocationSar),
                paymentMethod = "ESCROW_LOCKED",
                cvmRequired = false,
                cryptographicReceipt = "",
                auditNote = "[REJECT] Deliverable not marked uploaded by creator."
            )
        }

        if (!milestone.isSponsorApproved) {
            return DisbursementResult(
                isSuccess = false,
                milestoneId = milestoneId,
                financialBreakdown = calculateSettlementBreakdown(milestone.allocationSar),
                paymentMethod = "ESCROW_LOCKED",
                cvmRequired = false,
                cryptographicReceipt = "",
                auditNote = "[REJECT] Sponsor approval pending for milestone $milestoneId."
            )
        }

        val breakdown = calculateSettlementBreakdown(milestone.allocationSar)
        val cvmRequired = evaluateCvmRequirement(breakdown.netCreatorPayoutSar)

        // Compute HMAC-SHA256 signature
        val rawMessage = "${contract.contractId}:${contract.sponsorId}:${contract.creatorId}:$milestoneId:${breakdown.netCreatorPayoutSar}"
        val signature = computeHmacSha256(rawMessage, sponsorSigningKey)

        val cvmStatus = if (cvmRequired) "CVM_MANDATORY_PIN_OR_BIO" else "NO_PIN_BELOW_300_SAR"
        val auditNote = "[OK] Escrow disbursed for milestone $milestoneId. Net: ${String.format(Locale.US, "%.2f", breakdown.netCreatorPayoutSar)} SAR | Fee: ${String.format(Locale.US, "%.2f", breakdown.platformFeeSar)} SAR | CVM: $cvmStatus"

        return DisbursementResult(
            isSuccess = true,
            milestoneId = milestoneId,
            financialBreakdown = breakdown,
            paymentMethod = "Mada Contactless EMV L2 Payout",
            cvmRequired = cvmRequired,
            cryptographicReceipt = signature,
            auditNote = auditNote
        )
    }

    private fun computeHmacSha256(data: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKey)
        val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02X".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            val index = i * 2
            val v = hex.substring(index, index + 2).toInt(16)
            result[i] = v.toByte()
        }
        return result
    }
}
