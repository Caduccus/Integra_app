package com.example.plataformaremota

import android.app.Application
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

class IntegraApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Ativa logs para debug
        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        // Inicializa o OneSignal com o SEU APP ID (Garanta que é o da nova conta)
        OneSignal.initWithContext(this, "d2bb6774-9d05-4241-8e20-b78a7a325119")

        // A permissão agora é pedida na BaseActivity de forma nativa
    }
}