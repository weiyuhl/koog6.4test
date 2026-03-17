package com.lhzkml.jasmine.core.assistant.tools

import android.app.AlarmClock
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.CalendarContract
import androidx.core.app.NotificationCompat
import com.lhzkml.jasmine.core.agent.tools.Tool
import com.lhzkml.jasmine.core.prompt.model.ToolDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.TimeZone
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 助手模块原生工具集
 * 直接移植自核心代码，不使用桥接逻辑。
 */

/**
 * 通知工具
 */
class NotificationTool(private val context: Context) : Tool() {
    override val descriptor = ToolDescriptor(
        name = "send_notification",
        description = "Send a push notification to the device",
        requiredParameters = listOf(
            ToolParameterDescriptor("message", "Notification content/body", ToolParameterType.StringType)
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor("title", "Notification title", ToolParameterType.StringType)
        )
    )

    override suspend fun execute(arguments: String): String {
        val args = Json.parseToJsonElement(arguments).jsonObject
        val title = args["title"]?.jsonPrimitive?.content ?: "Assistant"
        val message = args["message"]?.jsonPrimitive?.content ?: return "Error: Message is required"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "assistant_notifications"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Assistant Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        return "Success: Notification sent"
    }
}

/**
 * 闹钟工具
 */
class AlarmTool(private val context: Context) : Tool() {
    override val descriptor = ToolDescriptor(
        name = "set_alarm",
        description = "Set an alarm or countdown timer on the device",
        optionalParameters = listOf(
            ToolParameterDescriptor("hour", "Hour (0-23)", ToolParameterType.IntegerType),
            ToolParameterDescriptor("minutes", "Minutes (0-59)", ToolParameterType.IntegerType),
            ToolParameterDescriptor("label", "Alarm label", ToolParameterType.StringType),
            ToolParameterDescriptor("duration_seconds", "Timer duration", ToolParameterType.IntegerType)
        )
    )

    override suspend fun execute(arguments: String): String {
        val args = Json.parseToJsonElement(arguments).jsonObject
        val duration = args["duration_seconds"]?.jsonPrimitive?.intOrNull
        
        val intent = if (duration != null) {
            Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, duration)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            }
        } else {
            val hour = args["hour"]?.jsonPrimitive?.intOrNull ?: return "Error: Hour required for alarm"
            val minutes = args["minutes"]?.jsonPrimitive?.intOrNull ?: 0
            Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            }
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "Success: Alarm/Timer set"
    }
}

/**
 * 日历工具（“毅力”工具的正确实现）
 */
class CalendarTool(private val context: Context) : Tool() {
    override val descriptor = ToolDescriptor(
        name = "create_calendar_event",
        description = "Create a calendar event on the user's device",
        requiredParameters = listOf(
            ToolParameterDescriptor("title", "Event title", ToolParameterType.StringType),
            ToolParameterDescriptor("start_time", "ISO 8601 format (e.g., 2024-03-15T14:30:00)", ToolParameterType.StringType)
        )
    )

    override suspend fun execute(arguments: String): String {
        val args = Json.parseToJsonElement(arguments).jsonObject
        val title = args["title"]?.jsonPrimitive?.content ?: return "Error: Title required"
        val startTimeRaw = args["start_time"]?.jsonPrimitive?.content ?: return "Error: Start time required"

        return try {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val startDateTime = LocalDateTime.parse(startTimeRaw, formatter)
            val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, startMillis + 3600000) // Default 1 hour
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.CALENDAR_ID, 1) // Default to first calendar
            }

            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            "Success: Event '$title' created at $startTimeRaw"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

/**
 * 本地时间工具（时钟功能）
 */
class LocalTimeTool : Tool() {
    override val descriptor = ToolDescriptor(
        name = "get_local_time",
        description = "Get the current local date and time. Call this first when the user mentions relative dates like 'tomorrow', 'next week', 'in 2 hours', etc.",
        parameters = emptyMap()
    )

    override suspend fun execute(arguments: String): String {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' hh:mm a", Locale.US)
        val displayStr = now.format(formatter)
        val isoStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        
        return """
            {
                "iso_datetime": "$isoStr",
                "display_datetime": "$displayStr",
                "timezone": "${ZoneId.systemDefault().id}",
                "day_of_week": "${now.dayOfWeek.name}"
            }
        """.trimIndent()
    }
}

/**
 * IP 地理位置工具
 */
class IpLocationTool : Tool() {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    override val descriptor = ToolDescriptor(
        name = "get_location_from_ip",
        description = "Get the user's estimated location based on their IP address. Returns city, region, country, coordinates, and timezone.",
        parameters = emptyMap()
    )

    override suspend fun execute(arguments: String): String {
        return try {
            val request = Request.Builder()
                .url("http://ip-api.com/json/")
                .build()
            
            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }
            if (!response.isSuccessful) return "Error: HTTP ${response.code}"
            response.body?.string() ?: "Error: Empty response"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

/**
 * 网页/链接打开工具
 */
class OpenUrlTool(private val context: Context) : Tool() {
    override val descriptor = ToolDescriptor(
        name = "open_url",
        description = "Open a URL or link on the user's device. Use this to open web pages, deep links, local files (file:// URIs), or any URL the user wants to visit.",
        requiredParameters = listOf(
            ToolParameterDescriptor("url", "The URL to open", ToolParameterType.StringType)
        )
    )

    override suspend fun execute(arguments: String): String {
        val args = Json.parseToJsonElement(arguments).jsonObject
        val url = args["url"]?.jsonPrimitive?.content ?: return "Error: URL is required"
        
        return try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Success: URL opened"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
