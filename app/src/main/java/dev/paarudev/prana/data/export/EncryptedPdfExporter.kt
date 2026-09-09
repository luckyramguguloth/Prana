package dev.paarudev.prana.data.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import dev.paarudev.prana.data.db.WellnessEntry
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * On-Device Encrypted PDF Exporter.
 *
 * Synthesizes a structured clinical/wellness report locally and encrypts it
 * with a user-specified passcode using AES-256-GCM + PBKDF2 key derivation.
 * Zero network dependencies; strictly on-device.
 */
class EncryptedPdfExporter(private val context: Context) {

    data class ExportResult(
        val file: File,
        val contentUri: android.net.Uri,
        val originalPdfBytesSize: Int,
        val encryptedBytesSize: Int,
        val isEncrypted: Boolean
    )

    fun generateAndEncryptReport(
        entries: List<WellnessEntry>,
        passcode: String
    ): ExportResult {
        val pdfBytes = createPdfReport(entries)

        val reportDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportFile = File(reportDir, "Prana_Wellness_Report_$timeStamp.prana.enc")

        val encryptedBytes = encryptBytes(pdfBytes, passcode)

        FileOutputStream(exportFile).use { it.write(encryptedBytes) }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile
        )

        return ExportResult(
            file = exportFile,
            contentUri = uri,
            originalPdfBytesSize = pdfBytes.size,
            encryptedBytesSize = encryptedBytes.size,
            isEncrypted = true
        )
    }

    private fun createPdfReport(entries: List<WellnessEntry>): ByteArray {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 portrait
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Header Background
        paint.color = Color.rgb(0x3E, 0x5C, 0x50) // Sage Primary
        canvas.drawRect(0f, 0f, 595f, 100f, paint)

        // Title
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText("Prana — On-Device Wellness Report", 40f, 50f, paint)

        paint.textSize = 11f
        paint.isFakeBoldText = false
        val dateStr = SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated locally: $dateStr · DPDP Act 2023 Compliant", 40f, 75f, paint)

        // Non-Diagnostic Notice
        paint.color = Color.rgb(0xC1, 0x55, 0x3D) // Muted brick
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("NOTICE: Wellness awareness data only. Not a medical diagnosis or prescription.", 40f, 125f, paint)

        paint.color = Color.rgb(0x1C, 0x1B, 0x19)
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("7-Day Longitudinal Telemetry Summary", 40f, 160f, paint)

        // Table Header
        paint.textSize = 10f
        paint.isFakeBoldText = true
        paint.color = Color.rgb(0x5A, 0x58, 0x53)
        var y = 190f
        canvas.drawText("Date & Time", 40f, y, paint)
        canvas.drawText("Heart Rate", 160f, y, paint)
        canvas.drawText("HRV (RMSSD)", 250f, y, paint)
        canvas.drawText("Voice Stress", 350f, y, paint)
        canvas.drawText("Wellness Score", 450f, y, paint)

        paint.color = Color.rgb(0xDD, 0xD8, 0xCE)
        canvas.drawLine(40f, y + 6f, 555f, y + 6f, paint)

        y += 24f
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

        paint.isFakeBoldText = false
        paint.color = Color.rgb(0x1C, 0x1B, 0x19)

        for (e in entries.take(12)) {
            val dateFormatted = sdf.format(Date(e.timestampMs))
            canvas.drawText(dateFormatted, 40f, y, paint)
            canvas.drawText("${e.heartRateBpm.toInt()} bpm", 160f, y, paint)
            canvas.drawText("${e.hrvRmssdMs.toInt()} ms", 250f, y, paint)
            canvas.drawText("${e.voiceStressScore} / 10", 350f, y, paint)
            canvas.drawText("${e.wellnessScore} (${e.wellnessState})", 450f, y, paint)
            y += 20f
        }

        // Security & Cryptography Verification Footer
        y += 20f
        paint.color = Color.rgb(0x3E, 0x5C, 0x50)
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("Security & Privacy Guarantees:", 40f, y, paint)

        paint.color = Color.rgb(0x5A, 0x58, 0x53)
        paint.textSize = 9f
        paint.isFakeBoldText = false
        y += 16f
        canvas.drawText("• End-to-End On-Device Processing: Zero cloud dependencies, zero external network calls.", 40f, y, paint)
        y += 14f
        canvas.drawText("• Encryption at rest: SQLCipher database with Android Keystore-backed AES-256 key.", 40f, y, paint)
        y += 14f
        canvas.drawText("• Encryption in transit: User-passcode protected AES-256-GCM envelope.", 40f, y, paint)

        doc.finishPage(page)

        val outputStream = ByteArrayOutputStream()
        doc.writeTo(outputStream)
        doc.close()
        return outputStream.toByteArray()
    }

    private fun encryptBytes(data: ByteArray, passcode: String): ByteArray {
        val salt = ByteArray(16)
        val iv = ByteArray(12)
        val random = SecureRandom()
        random.nextBytes(salt)
        random.nextBytes(iv)

        // PBKDF2WithHmacSHA256 key derivation (10,000 iterations)
        val keySpec = PBEKeySpec(passcode.toCharArray(), salt, 10000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKeyBytes = factory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(secretKeyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val cipherText = cipher.doFinal(data)

        // Format: [16-byte Salt] + [12-byte IV] + [Ciphertext + Tag]
        val combined = ByteArray(salt.size + iv.size + cipherText.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(cipherText, 0, combined, salt.size + iv.size, cipherText.size)
        return combined
    }
}
