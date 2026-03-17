package com.lhzkml.jasmine.core.assistant.email

/**
 * 邮件服务器配置自动检测逻辑
 */
@Serializable
data class DetectedServer(
    val imapHost: String,
    val imapPort: Int = 993,
    val smtpHost: String,
    val smtpPort: Int = 587,
    val useStartTls: Boolean = true,
    val note: String? = null
)

object ServerAutoDetect {
    fun detect(email: String): DetectedServer? {
        val domain = email.substringAfter("@").lowercase()
        return when {
            domain.contains("gmail") -> DetectedServer(
                imapHost = "imap.gmail.com",
                smtpHost = "smtp.gmail.com",
                note = "Requires 'App Password' if 2FA is enabled."
            )
            domain.contains("outlook") || domain.contains("hotmail") || domain.contains("live.com") -> DetectedServer(
                imapHost = "outlook.office365.com",
                smtpHost = "smtp.office365.com"
            )
            domain.contains("yahoo") -> DetectedServer(
                imapHost = "imap.mail.yahoo.com",
                smtpHost = "smtp.mail.yahoo.com"
            )
            domain.contains("icloud") -> DetectedServer(
                imapHost = "imap.mail.me.com",
                smtpHost = "smtp.mail.me.com"
            )
            else -> null
        }
    }
}
