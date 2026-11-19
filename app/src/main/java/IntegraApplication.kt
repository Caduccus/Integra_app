package com.example.plataformaremota

import android.app.Application
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationLifecycleListener
import com.onesignal.notifications.INotificationWillDisplayEvent

class IntegraApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        OneSignal.initWithContext(this, "d2bb6774-9d05-4241-8e20-b78a7a325119")

        // ⭐ SILENCIADOR: Se a notificação chegar e o usuário estiver no chat, previne a exibição
        OneSignal.Notifications.addForegroundLifecycleListener(
            object : INotificationLifecycleListener {
                override fun onWillDisplay(event: INotificationWillDisplayEvent) {
                    val chatId = event.notification.additionalData?.optString("chat_id")

                    // Se a notificação tem um chatId e ele é o chat que o usuário está vendo agora
                    if (!chatId.isNullOrEmpty() && chatId == ChatAtivoManager.chatAtivo) {
                        event.preventDefault() // Cancela o balão na tela
                    }
                }
            }
        )
    }
}