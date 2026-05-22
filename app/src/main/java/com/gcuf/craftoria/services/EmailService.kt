package com.gcuf.craftoria.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailService {
    private const val GMAIL_USER = "itxzmaheri@gmail.com"
    private const val GMAIL_APP_PASS = "etsj ljwo shbt nhod"

    suspend fun sendOrderConfirmationEmail(
        buyerEmail: String,
        buyerName: String,
        orderId: String,
        totalPrice: String,
        paymentMethod: String,
        deliveryAddress: String
    ) = withContext(Dispatchers.IO) {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.ssl.trust", "smtp.gmail.com")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(GMAIL_USER, GMAIL_APP_PASS)
        })

        val shortOrderId = orderId.take(8).uppercase()
        val htmlBody = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                <h2 style="color: #E91E8C; text-align: center;">Order Confirmed 🎉</h2>
                <p>Hello <b>$buyerName</b>,</p>
                <p>Thank you for your order! Here are your order details:</p>
                <table style="width:100%; border-collapse: collapse; margin: 16px 0;">
                    <tr style="background:#f5f5f5;">
                        <td style="padding: 10px; border: 1px solid #ddd;"><b>Order ID</b></td>
                        <td style="padding: 10px; border: 1px solid #ddd;">#$shortOrderId</td>
                    </tr>
                    <tr>
                        <td style="padding: 10px; border: 1px solid #ddd;"><b>Total Amount</b></td>
                        <td style="padding: 10px; border: 1px solid #ddd;">PKR $totalPrice</td>
                    </tr>
                    <tr style="background:#f5f5f5;">
                        <td style="padding: 10px; border: 1px solid #ddd;"><b>Payment Method</b></td>
                        <td style="padding: 10px; border: 1px solid #ddd;">$paymentMethod</td>
                    </tr>
                    <tr>
                        <td style="padding: 10px; border: 1px solid #ddd;"><b>Delivery Address</b></td>
                        <td style="padding: 10px; border: 1px solid #ddd;">$deliveryAddress</td>
                    </tr>
                </table>
                <hr style="margin: 24px 0; border: none; border-top: 1px solid #eee;" />
                <p style="color: #888; font-size: 12px; text-align: center;">Thank you for shopping on Craftoria ❤️<br/>For help, contact itxzmaheri@gmail.com</p>
            </div>
        """.trimIndent()

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(GMAIL_USER, "Craftoria"))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(buyerEmail))
            subject = "Order Confirmation - #$shortOrderId"
            setContent(htmlBody, "text/html; charset=utf-8")
        }

        Transport.send(message)
    }

    suspend fun sendSellerApprovalEmail(
        sellerEmail: String,
        sellerName: String
    ) = withContext(Dispatchers.IO) {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.ssl.trust", "smtp.gmail.com")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(GMAIL_USER, GMAIL_APP_PASS)
        })

        val htmlBody = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                <h2 style="color: #E91E8C; text-align: center;">🎉 Seller Account Approved!</h2>
                <p>Hello <b>$sellerName</b>,</p>
                <p>Congratulations! Your seller application has been approved by our admin team.</p>
                <div style="background: #E8F5E8; padding: 16px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #4CAF50;">
                    <p style="margin: 0; color: #2E7D2E; font-weight: bold;">✓ Your seller account is now active!</p>
                </div>
                <h3 style="color: #333; margin-top: 24px;">What's Next?</h3>
                <ol style="line-height: 1.8; color: #555;">
                    <li>Open the Craftoria app</li>
                    <li>Complete your identity verification</li>
                    <li>Start adding your products</li>
                    <li>Begin selling to customers!</li>
                </ol>
                <div style="text-align: center; margin: 30px 0;">
                    <p style="color: #888; font-size: 14px;">Open the app now to get started with your seller journey!</p>
                </div>
                <hr style="margin: 24px 0; border: none; border-top: 1px solid #eee;" />
                <p style="color: #888; font-size: 12px; text-align: center;">
                    Welcome to the Craftoria seller community! ❤️<br/>
                    For help, contact itxzmaheri@gmail.com
                </p>
            </div>
        """.trimIndent()

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(GMAIL_USER, "Craftoria"))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(sellerEmail))
            subject = "🎉 Your Seller Account Has Been Approved!"
            setContent(htmlBody, "text/html; charset=utf-8")
        }

        Transport.send(message)
    }

    // ── Password Reset OTP via EmailJS HTTP API ───────────────────────────────
    // Uses a separate EmailJS account/template dedicated to password reset.
    // Replace the three constants below with your 2nd EmailJS account values.
    private const val OTP_EMAILJS_SERVICE_ID  = "service_muotzyb"  
    private const val OTP_EMAILJS_TEMPLATE_ID = "template_k3yupgg"
    private const val OTP_EMAILJS_PUBLIC_KEY  = "wfWfBLv5JKIDKJkj_"

    suspend fun sendPasswordResetOtp(
        toEmail: String,
        toName: String,
        otpCode: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.emailjs.com/api/v1.0/email/send")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("origin", "https://craftoria.app")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("service_id", OTP_EMAILJS_SERVICE_ID)
                put("template_id", OTP_EMAILJS_TEMPLATE_ID)
                put("user_id", OTP_EMAILJS_PUBLIC_KEY)
                put("template_params", JSONObject().apply {
                    put("to_email", toEmail)
                    put("to_name", toName)
                    put("otp_code", otpCode)
                })
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("EmailJS error: HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
