package com.example.plataformaremota

import android.app.Application
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationLifecycleListener
import com.onesignal.notifications.INotificationWillDisplayEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IntegraApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        OneSignal.initWithContext(this, "eb63f7b4-c19e-4a5a-8a69-ecf5bc8413db")

        // ⭐ Bloqueia notificação se o usuário já está no chat
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