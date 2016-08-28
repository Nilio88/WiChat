package com.sms1516.porcelli.daniele.wichat;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Questa interfaccia verrà usata dal servizio WiChatService
 * per astrarre l'attività che si registrerà come listener dei
 * dispositivi trovati tramite il network service discovery
 * e dei messaggi ricevuti.
 *
 * @author Daniele Porcelli
 */
public interface ContactsListener extends MessagesListener {

    /**
     * Metodo di callback invocato quando la lista dei contatti
     * è cambiata.
     *
     * @param contact Il dispositivo trovato nell'area del Wi-Fi.
     */
    public void onContactFound(WifiP2pDevice contact);
}
