package com.lhzkml.codestudio

import android.app.Application
import com.lhzkml.codestudio.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CodeStudioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CodeStudioApplication)
            modules(appModule)
        }
    }
}
