package com.sms1516.porcelli.daniele.wichat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * BroadcastReceiver che si mette in ascolto dell'Intent
 * di broadcast relativo all'avvio del dispositivo e
 * avvia, di conseguenza, il servizio WiChatService.
 *
 * @author Daniele Porcelli
 */

public class BootReceiver extends BroadcastReceiver {
    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals("android.intent.action.BOOT_COMPLETED")) {

            //Avvia il servizio WiChatService
            context.startService(new Intent(context, WiChatService.class));

            //Inizializza il MessagesStore
            MessagesStore.initialize(context);
        }
    }
}
