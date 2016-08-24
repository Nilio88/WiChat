package com.sms1516.porcelli.daniele.wichat;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.IBinder;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.content.BroadcastReceiver;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Questo servizio rappresenta il cuore dell'applicazione.
 * Esso ha la responsabilità di attivare il network service discovery
 * per la comunicazione tra due singoli interlocutori e, opzionalmente,
 * attivare un network service discovery per un gruppo.
 * Questo servizio inoltre si mette in ascolto di servizi forniti
 * da altri dispositivi limitrofi e, se tali servizi sono uguali a
 * quelli richiesti dall'applicazione WiChat, i rispettivi dispositivi
 * verranno aggiunti nella lista dei contatti della MainActivity.
 * L'invio e la ricezione dei messaggi avverranno, anch'essi, in
 * questo servizio.
 *
 * @author Daniele Porcelli
 */

public class WiChatService extends Service {

    //Variabili d'istanza
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;
    private List peers = new ArrayList();
    private ContactsListListener mContactsListListener;

    //Costanti
    private static final String LOG_TAG = WiChatService.class.getName();
    private static final String ACTION_START_NSD_SERVICE = "com.sms1516.porcelli.daniele.wichat.action.START_NSD_SERVICE";

    @Override
    public void onCreate() {

        //Registra WiChatService al framework Wi-Fi P2P
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        //Crea l'intent filter per WifiP2pBroadCastReceiver
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {

            //Registra il WifiP2pBroadcastReceiver
            registerReceiver(mReceiver, mIntentFilter);
        }

        else if(intent.getAction().equals(ACTION_START_NSD_SERVICE)) {

            //Avvia il servizio di network service discovery locale

        }
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {

        //Interrompi il broadcast receiver per gli Intent del framework Wi-Fi P2P
        if (mReceiver != null)
            unregisterReceiver(mReceiver);
    }

    /**
     * Metodo statico chiamato da WifiP2pBroadcastReceiver per
     * registrare il network service discovery.
     */
    public static void registerNsdService(Context context) {
        Intent startNsdIntent = new Intent(context, WiChatService.class);
        startNsdIntent.setAction(ACTION_START_NSD_SERVICE);

    }


    /**
     * Broadcast Receiver per gli Intent di broadcast
     * provenienti dal framework Wi-Fi P2P.
     *
     * @author Daniele Porcelli
     */
    private class WifiP2pBroadcastReceiver extends BroadcastReceiver {

        //Implementazione del PeerListListener
        //Nota: probabilmente non ci servirà, ma lo lascio per sicurezza.
        private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {

            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {

                //Elimina tutti i vecchi dispositivi trovati e inserisce quelli nuovi
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                //Manda la lista dei dispositivi appena trovati all'activity interessata
                if (mContactsListListener != null)
                    mContactsListListener.onContactsListChanged(peers);
            }
        };

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
                //Nota: probabilmente neanche questo ci servirà, ma lo teniamo per sicurezza.
                mManager.requestPeers(mChannel, peerListListener);
            }


        }
    }

    /**
     * Classe interna che rappresenta il thread da eseguire per attivare
     * il network service discovery per informare i dispositivi limitrofi
     * del servizio messo a disposizione da questa applicazione.
     *
     * @author Daniele Porcelli
     */
    private class NsdProviderThread extends Thread {

        @Override
        public void run() {

            //Ottiene il numero della prima porta disponibile
            ServerSocket server;
            try {
                server = new ServerSocket(0);
            }
            catch(IOException ex) {
                Log.e(LOG_TAG, ex.toString());
                return;
            }

            int port = server.getLocalPort();

        }
    }
}
