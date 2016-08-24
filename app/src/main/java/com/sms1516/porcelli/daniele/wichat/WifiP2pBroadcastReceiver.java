package com.sms1516.porcelli.daniele.wichat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import java.util.List;
import java.util.ArrayList;

/**
 * Nota: Non è una buona soluzione implementarlo in un file
 * a parte. Meglio implementarlo come classe interna del
 * servizio WiChatService.
 *
 * Broadcast Receiver per gli Intent di broadcast
 * provenienti dal framework Wi-Fi P2P.
 *
 * @author Daniele Porcelli
 */
public class WifiP2pBroadcastReceiver extends BroadcastReceiver {

    //Variabili d'istanza
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private ContactsListListener mContactsListListener;
    private List peers = new ArrayList();

    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {

        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            //Elimina tutti i vecchi dispositivi trovati e inserisce quelli nuovi
            peers.clear();
            peers.addAll(peerList.getDeviceList());

            //Manda la lista dei dispositivi appena trovati all'activity interessata
        }
    };

    /**
     * Costruttore del boradcast receiver.
     *
     * @param aManager un'istanza della classe WifiP2pManager per gestire il framework Wi-Fi P2P;
     * @param aChannel l'istanza di WifiP2pManager.Channel ottenuta mediante l'invocazione del metodo initialize();
     * @param aListener un'activity (o altro componente) che implementa l'interfaccia ContactsListListener e a cui verrà mandata la lista dei contatti trovati.
     */
    public WifiP2pBroadcastReceiver(WifiP2pManager aManager, WifiP2pManager.Channel aChannel, ContactsListListener aListener) {
        super();
        mManager = aManager;
        mChannel = aChannel;
        mContactsListListener = aListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)) {

            //Controlla se il Wi-Fi P2P è attivo e supportato dal dispositivo
            int statoWiFi = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (statoWiFi == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {

                //Chiama WiChatService per registrare il servizio di network service discovery
                WiChatService.registerNsdService(context);
            }
        }
        else if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {
            //Non so ancora cosa fare...
        }

        else if (action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {

            //Recupera la nuova lista di contatti disponibili nel range del Wi-Fi

        }


    }
}
