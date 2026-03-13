package com.lhzkml.codestudio

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CodeStudioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AgentRunner.init(this)
    }
}
