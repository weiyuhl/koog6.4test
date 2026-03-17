package com.lhzkml.jasmine.core.assistant.email

/**
 * 最小化 SMTP 客户端
 */
class SmtpClient(
    private val host: String,
    private val port: Int = 587,
    private val useStartTls: Boolean = true,
) {
    private var connection: EmailConnection? = null

    suspend fun connect() {
        connection = createEmailConnection(host, port, false)
        readResponse() // 220
    }

    suspend fun ehlo() {
        val conn = connection ?: throw IllegalStateException("Not connected")
        conn.writeLine("EHLO assistant")
        readResponse()
    }

    suspend fun startTls() {
        val conn = connection ?: throw IllegalStateException("Not connected")
        conn.writeLine("STARTTLS")
        readResponse() // 220
        conn.upgradeToTls(host)
        ehlo()
    }

    suspend fun authenticate(username: String, password: String) {
        val conn = connection ?: throw IllegalStateException("Not connected")
        conn.writeLine("AUTH LOGIN")
        readResponse() // 334
        conn.writeLine(encodeBase64(username))
        readResponse() // 334
        conn.writeLine(encodeBase64(password))
        readResponse() // 235
    }

    suspend fun sendReply(from: String, to: String, subject: String, body: String, inReplyTo: String? = null): Boolean {
        val conn = connection ?: throw IllegalStateException("Not connected")
        conn.writeLine("MAIL FROM:<$from>")
        readResponse()
        conn.writeLine("RCPT TO:<$to>")
        readResponse()
        conn.writeLine("DATA")
        readResponse()
        
        val message = buildString {
            appendLine("From: $from")
            appendLine("To: $to")
            appendLine("Subject: $subject")
            if (inReplyTo != null) {
                appendLine("In-Reply-To: $inReplyTo")
                appendLine("References: $inReplyTo")
            }
            appendLine("Content-Type: text/plain; charset=UTF-8")
            appendLine()
            appendLine(body)
            appendLine(".")
        }
        conn.writeLine(message)
        val response = readResponse()
        return response.startsWith("250")
    }

    suspend fun quit() {
        try {
            val conn = connection ?: return
            conn.writeLine("QUIT")
            conn.close()
        } catch (_: Exception) {
        } finally {
            connection = null
        }
    }

    private suspend fun readResponse(): String {
        val conn = connection ?: throw IllegalStateException("Not connected")
        val result = StringBuilder()
        while (true) {
            val line = conn.readLine()
            result.appendLine(line)
            if (line.length >= 4 && line[3] == ' ') break
        }
        return result.toString()
    }

    private fun encodeBase64(s: String): String {
        return java.util.Base64.getEncoder().encodeToString(s.toByteArray())
    }
}
