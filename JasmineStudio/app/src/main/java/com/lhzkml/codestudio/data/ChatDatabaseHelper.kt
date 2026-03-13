package com.lhzkml.codestudio.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class ChatDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "chat_studio.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_SESSIONS = "chat_sessions"
        const val COLUMN_SESSION_ID = "id"
        const val COLUMN_SESSION_TITLE = "title"
        const val COLUMN_SESSION_CREATED_AT = "created_at"

        const val TABLE_MESSAGES = "chat_messages"
        const val COLUMN_MESSAGE_ID = "id"
        const val COLUMN_MESSAGE_SESSION_ID = "session_id"
        const val COLUMN_MESSAGE_ROLE = "role"
        const val COLUMN_MESSAGE_CONTENT = "content"
        const val COLUMN_MESSAGE_TIMESTAMP = "timestamp"

        private const val CREATE_SESSIONS_TABLE = """
            CREATE TABLE $TABLE_SESSIONS (
                $COLUMN_SESSION_ID TEXT PRIMARY KEY,
                $COLUMN_SESSION_TITLE TEXT,
                $COLUMN_SESSION_CREATED_AT INTEGER
            )
        """

        private const val CREATE_MESSAGES_TABLE = """
            CREATE TABLE $TABLE_MESSAGES (
                $COLUMN_MESSAGE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_MESSAGE_SESSION_ID TEXT,
                $COLUMN_MESSAGE_ROLE TEXT,
                $COLUMN_MESSAGE_CONTENT TEXT,
                $COLUMN_MESSAGE_TIMESTAMP INTEGER,
                FOREIGN KEY($COLUMN_MESSAGE_SESSION_ID) REFERENCES $TABLE_SESSIONS($COLUMN_SESSION_ID) ON DELETE CASCADE
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_SESSIONS_TABLE)
        db.execSQL(CREATE_MESSAGES_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SESSIONS")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }
}
