package com.example.plataformaremota

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationLifecycleListener
import com.onesignal.notifications.INotificationWillDisplayEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IntegraApplication : Application() {

    // ⭐ Aplica fontScale no app INTEIRO
    override fun attachBaseContext(base: Context) {
        val escala = AcessibilidadePrefs.getFontScaleValue(base)
        val config = Configuration(base.resources.configuration)
        config.fontScale = escala
        super.attachBaseContext(base.createConfigurationContext(config))
    }

    override fun onCreate() {
        super.onCreate()

        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        OneSignal.initWithContext(this, "d2bb6774-9d05-4241-8e20-b78a7a325119")

        OneSignal.Notifications.addForegroundLifecycleListener(
            object : INotificationLifecycleListener {
                override fun onWillDisplay(event: INotificationWillDisplayEvent) {
                    val chatId = event.notification.additionalData?.optString("chat_id")
                    if (!chatId.isNullOrEmpty() && chatId == ChatAtivoManager.chatAtivo) {
                        event.preventDefault()
                    }
                }
            }
        )

        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(true)
        }
    }
}