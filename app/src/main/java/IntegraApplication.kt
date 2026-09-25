package com.example.plataformaremota

import android.app.Application
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IntegraApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Ativa logs detalhados
        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        // Inicializa o OneSignal
        OneSignal.initWithContext(this, "eb63f7b4-c19e-4a5a-8a69-ecf5bc8413db")

        // Pede permissão (dentro de uma coroutine)
        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(true)
        }
    }
}