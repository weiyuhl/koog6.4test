package com.lhzkml.jasmine.core.assistant.email

/**
 * 最小化 IMAP 客户端（深度对齐版）
 */
private val imapExistsRegex = Regex("\\* (\\d+) EXISTS")
private val imapTaggedResponseRegex = Regex("^A\\d+ (OK|NO|BAD) .*")
private val mimeBoundaryRegex = Regex("^--([\\w'()+,-./:=? ]+)\\s*$", RegexOption.MULTILINE)

class ImapClient(
    private val host: String,
    private val port: Int = 993,
    private val tls: Boolean = true,
) {
    private var connection: EmailConnection? = null
    private var tagCounter = 0

    private fun nextTag(): String = "A${++tagCounter}"

    suspend fun connect() {
        connection = createEmailConnection(host, port, tls)
        // 读取服务器问候
        readUntilTaggedOrGreeting(null)
    }

    suspend fun login(username: String, password: String): Boolean {
        val tag = nextTag()
        val conn = connection ?: throw IllegalStateException("Not connected")
        conn.writeLine("$tag LOGIN \"${escapeQuoted(username)}\" \"${escapeQuoted(password)}\"")
        val response = readUntilTaggedOrGreeting(tag)
        return response.contains("OK")
    }

    suspend fun selectInbox(): Int {
        val tag = nextTag()
        val conn = connection ?: throw IllegalStateException("Not connected")
        conn.writeLine("$tag SELECT INBOX")
        val response = readUntilTaggedOrGreeting(tag)
        val existsMatch = imapExistsRegex.find(response)
        return existsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    suspend fun searchUnseen(): List<Long> = search("SEARCH UNSEEN")

    private suspend fun search(command: String): List<Long> {
        val tag = nextTag()
        val conn = connection ?: throw IllegalStateException("Not connected")
        conn.writeLine("$tag $command")
        val response = readUntilTaggedOrGreeting(tag)
        val searchLine = response.lines().find { it.startsWith("* SEARCH") } ?: return emptyList()
        return searchLine.removePrefix("* SEARCH").trim().split(" ")
            .filter { it.isNotBlank() }
            .mapNotNull { it.toLongOrNull() }
    }

    suspend fun fetchHeaders(uids: List<Long>, accountId: String): List<EmailMessage> {
        if (uids.isEmpty()) return emptyList()
        val conn = connection ?: throw IllegalStateException("Not connected")
        val messages = mutableListOf<EmailMessage>()

        for (uid in uids.take(50)) {
            val tag = nextTag()
            conn.writeLine("$tag FETCH $uid (BODY.PEEK[HEADER.FIELDS (FROM SUBJECT DATE MESSAGE-ID)] BODY.PEEK[TEXT]<0.200> FLAGS)")
            val response = readUntilTaggedOrGreeting(tag)
            val msg = parseEmailFromFetch(uid, accountId, response)
            if (msg != null) messages.add(msg)
        }
        return messages
    }

    suspend fun fetchBody(uid: Long, accountId: String): EmailMessage? {
        val conn = connection ?: throw IllegalStateException("Not connected")
        val tag = nextTag()
        conn.writeLine("$tag FETCH $uid (BODY.PEEK[HEADER.FIELDS (FROM TO SUBJECT DATE MESSAGE-ID)] BODY[TEXT])")
        val response = readUntilTaggedOrGreeting(tag)
        return parseEmailFromFetch(uid, accountId, response)
    }

    suspend fun markAsRead(uid: Long) {
        val conn = connection ?: throw IllegalStateException("Not connected")
        val tag = nextTag()
        conn.writeLine("$tag STORE $uid +FLAGS (\\Seen)")
        readUntilTaggedOrGreeting(tag)
    }

    suspend fun logout() {
        try {
            val conn = connection ?: return
            val tag = nextTag()
            conn.writeLine("$tag LOGOUT")
            readUntilTaggedOrGreeting(tag)
            conn.close()
        } catch (_: Exception) {
        } finally {
            connection = null
        }
    }

    private suspend fun readUntilTaggedOrGreeting(tag: String?): String {
        val conn = connection ?: throw IllegalStateException("Not connected")
        val result = StringBuilder()
        var lineCount = 0
        val maxLines = 500

        while (lineCount < maxLines) {
            val line = conn.readLine()
            result.appendLine(line)
            lineCount++

            if (tag == null) {
                if (line.startsWith("* OK") || line.startsWith("* NO") || line.startsWith("* BAD")) break
            } else {
                if (line.startsWith("$tag ")) break
            }
        }
        return result.toString()
    }

    private fun parseEmailFromFetch(uid: Long, accountId: String, raw: String): EmailMessage? {
        var from = ""
        var to = ""
        var subject = ""
        var date = ""
        var messageId = ""
        var body = ""
        var isRead = false

        val lines = raw.lines()
        for (line in lines) {
            val trimmed = line.trim()
            val lower = trimmed.lowercase()
            when {
                lower.startsWith("from:") -> from = trimmed.substringAfter(":").trim()
                lower.startsWith("to:") -> to = trimmed.substringAfter(":").trim()
                lower.startsWith("subject:") -> subject = trimmed.substringAfter(":").trim()
                lower.startsWith("date:") -> date = trimmed.substringAfter(":").trim()
                lower.startsWith("message-id:") -> messageId = trimmed.substringAfter(":").trim()
            }
        }

        if (raw.contains("\\Seen")) isRead = true
        body = extractBodyFromResponse(raw)
        val preview = body.take(200).replace("\n", " ").trim()

        return EmailMessage(
            uid = uid,
            accountId = accountId,
            from = from,
            to = to,
            subject = subject,
            date = date,
            preview = preview,
            body = body,
            messageId = messageId,
            isRead = isRead,
        )
    }

    private fun extractBodyFromResponse(raw: String): String {
        val bodyIdx = raw.indexOfAny("BODY[TEXT]", "BODY.PEEK[TEXT]")
        if (bodyIdx == -1) return ""
        val afterMarker = raw.substring(bodyIdx)
        val firstNewline = afterMarker.indexOf('\n')
        if (firstNewline == -1) return ""
        val bodyContent = afterMarker.substring(firstNewline + 1)
        return bodyContent.lines()
            .takeWhile { line -> !line.matches(imapTaggedResponseRegex) && line.trimEnd() != ")" }
            .joinToString("\n").trim()
    }

    private fun String.indexOfAny(vararg strings: String): Int {
        var minIdx = -1
        for (s in strings) {
            val idx = indexOf(s)
            if (idx != -1 && (minIdx == -1 || idx < minIdx)) minIdx = idx
        }
        return minIdx
    }

    private fun escapeQuoted(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")
}
