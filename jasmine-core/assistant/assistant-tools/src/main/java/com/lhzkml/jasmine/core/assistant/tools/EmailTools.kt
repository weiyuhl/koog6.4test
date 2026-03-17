package com.lhzkml.jasmine.core.assistant.tools

import com.lhzkml.jasmine.core.agent.tools.Tool
import com.lhzkml.jasmine.core.assistant.email.*
import com.lhzkml.jasmine.core.prompt.model.ToolDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterType
import kotlinx.serialization.json.*
import java.util.UUID

/**
 * 邮件管理工具集（签名对齐版）
 * 采用与 jasmine-core 统一的 execute(String) 签名。
 */
class EmailTools(private val emailStore: EmailStore) {

    private suspend fun <T> withImapSession(
        account: EmailAccount,
        block: suspend (ImapClient) -> T,
    ): T {
        val imap = ImapClient(account.imapHost, account.imapPort)
        try {
            val password = emailStore.getPassword(account.id)
            imap.connect()
            imap.login(account.username.ifEmpty { account.email }, password)
            imap.selectInbox()
            return block(imap)
        } finally {
            imap.logout()
        }
    }

    private suspend fun withSmtpSession(
        account: EmailAccount,
        block: suspend (SmtpClient, String) -> String,
    ): String {
        val smtp = SmtpClient(account.smtpHost, account.smtpPort, account.useStartTls)
        val password = emailStore.getPassword(account.id)
        smtp.connect()
        smtp.ehlo()
        if (account.useStartTls) smtp.startTls()
        smtp.authenticate(account.username.ifEmpty { account.email }, password)
        val from = if (account.displayName.isNotEmpty()) {
            "${account.displayName} <${account.email}>"
        } else {
            account.email
        }
        try {
            return block(smtp, from)
        } finally {
            smtp.quit()
        }
    }

    fun getSetupTool() = object : Tool() {
        override val descriptor = ToolDescriptor(
            name = "setup_email",
            description = "Connect an email account. Auto-detects settings for Gmail, Outlook, Yahoo, iCloud.",
            requiredParameters = listOf(
                ToolParameterDescriptor("email", "Email address", ToolParameterType.StringType),
                ToolParameterDescriptor("password", "Password (or App Password)", ToolParameterType.StringType)
            ),
            optionalParameters = listOf(
                ToolParameterDescriptor("display_name", "Optional display name", ToolParameterType.StringType)
            )
        )

        override suspend fun execute(arguments: String): String {
            val args = Json.parseToJsonElement(arguments).jsonObject
            val email = args["email"]?.jsonPrimitive?.content ?: return "Error: Missing email"
            val password = args["password"]?.jsonPrimitive?.content ?: return "Error: Missing password"
            val displayName = args["display_name"]?.jsonPrimitive?.content ?: ""

            val detected = ServerAutoDetect.detect(email) ?: return "Error: Could not auto-detect server settings."
            
            val imap = ImapClient(detected.imapHost, detected.imapPort)
            return try {
                imap.connect()
                val ok = imap.login(email, password)
                imap.logout()
                if (!ok) return "Error: Login failed."

                val id = UUID.randomUUID().toString()
                emailStore.addAccount(
                    EmailAccount(
                        id = id,
                        email = email,
                        displayName = displayName,
                        imapHost = detected.imapHost,
                        imapPort = detected.imapPort,
                        smtpHost = detected.smtpHost,
                        smtpPort = detected.smtpPort,
                        username = email,
                        useStartTls = detected.useStartTls
                    ),
                    password
                )
                "Success: Email $email connected. ID: $id"
            } catch (e: Exception) {
                "Error: Connection failed: ${e.message}"
            }
        }
    }

    fun getCheckTool() = object : Tool() {
        override val descriptor = ToolDescriptor(
            name = "check_email",
            description = "Check for unread emails.",
            parameters = emptyMap()
        )

        override suspend fun execute(arguments: String): String {
            val accounts = emailStore.getAccounts()
            if (accounts.isEmpty()) return "Error: No accounts configured."
            
            val results = StringBuilder()
            for (acc in accounts) {
                try {
                    withImapSession(acc) { imap ->
                        val uids = imap.searchUnseen()
                        val msgs = imap.fetchHeaders(uids.takeLast(10), acc.id)
                        results.append("Account: ${acc.email} (${msgs.size} unread)\n")
                        msgs.forEach { results.append("- ${it.from}: ${it.subject} (UID: ${it.uid})\n") }
                    }
                } catch (e: Exception) {
                    results.append("Account: ${acc.email} Error: ${e.message}\n")
                }
            }
            return results.toString()
        }
    }

    fun getReadTool() = object : Tool() {
        override val descriptor = ToolDescriptor(
            name = "read_email",
            description = "Read full email body.",
            requiredParameters = listOf(
                ToolParameterDescriptor("account_id", "ID of the email account", ToolParameterType.StringType),
                ToolParameterDescriptor("uid", "UID of the email", ToolParameterType.IntegerType)
            )
        )

        override suspend fun execute(arguments: String): String {
            val args = Json.parseToJsonElement(arguments).jsonObject
            val accId = args["account_id"]?.jsonPrimitive?.content ?: return "Error: Missing account_id"
            val uid = args["uid"]?.jsonPrimitive?.longOrNull ?: return "Error: Missing uid"
            val acc = emailStore.getAccount(accId) ?: return "Error: Account not found"

            return try {
                withImapSession(acc) { imap ->
                    val msg = imap.fetchBody(uid, accId)
                    imap.markAsRead(uid)
                    if (msg != null) {
                        "From: ${msg.from}\nSubject: ${msg.subject}\n\n${msg.body}"
                    } else {
                        "Error: Email not found"
                    }
                }
            } catch (e: Exception) {
                "Error: Read failed: ${e.message}"
            }
        }
    }

    fun getReplyTool() = object : Tool() {
        override val descriptor = ToolDescriptor(
            name = "reply_email",
            description = "Reply to an email or send a new one.",
            requiredParameters = listOf(
                ToolParameterDescriptor("account_id", "ID of the account to send from", ToolParameterType.StringType),
                ToolParameterDescriptor("to", "Recipient email address", ToolParameterType.StringType),
                ToolParameterDescriptor("subject", "Subject", ToolParameterType.StringType),
                ToolParameterDescriptor("body", "Content", ToolParameterType.StringType)
            ),
            optionalParameters = listOf(
                ToolParameterDescriptor("in_reply_to", "Message-ID (if replying)", ToolParameterType.StringType)
            )
        )

        override suspend fun execute(arguments: String): String {
            val args = Json.parseToJsonElement(arguments).jsonObject
            val accId = args["account_id"]?.jsonPrimitive?.content ?: return "Error: Missing account_id"
            val acc = emailStore.getAccount(accId) ?: return "Error: Account not found"

            return withSmtpSession(acc) { smtp, from ->
                val ok = smtp.sendReply(
                    from = acc.email,
                    to = args["to"]?.jsonPrimitive?.content ?: "",
                    subject = args["subject"]?.jsonPrimitive?.content ?: "",
                    body = args["body"]?.jsonPrimitive?.content ?: "",
                    inReplyTo = args["in_reply_to"]?.jsonPrimitive?.content
                )
                if (ok) "Success: Reply sent." else "Error: SMTP failed."
            }
        }
    }
}
