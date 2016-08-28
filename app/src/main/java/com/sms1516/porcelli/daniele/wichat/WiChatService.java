package com.sms1516.porcelli.daniele.wichat;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
    private MessagesStore mMessagesStore;
    private List peers = new ArrayList();
    private ContactsListener mContactsListener;
    private MessagesListener mMessagesListener;
    private List<ChatConnection> connections = new ArrayList<>();

    //Dizionario che manterrà le connessioni stabilite con i dispositivi nelle vicinanze
    //La chiave di tipo String è l'indirizzo MAC del destinatario in forma testuale
    //private Map<String, ChatConnection> connectionsMap = new HashMap<>();

    //Dizionario che manterrà le informazioni di connessione dei servizi presenti
    //nei dispositivi in vicinanza (Indirizzo MAC e porta)
    private Map<String, Integer> servicesConnectionInfo = new HashMap<>();

    //Costanti
    private static final String LOG_TAG = WiChatService.class.getName();
    private static final String ACTION_START_NSD_SERVICE = "com.sms1516.porcelli.daniele.wichat.action.START_NSD_SERVICE";

    @Override
    public void onCreate() {

        //Inizializza il MessagesStore per memorizzare i messaggi ricevuti
        mMessagesStore = MessagesStore.getInstance();

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
                if (mContactsListener != null)
                    //mContactsListener.onContactsListChanged(peers);
                ;
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
     * del servizio messo a disposizione da questa applicazione e ricevere
     * connessioni da questi ultimi.
     *
     * @author Daniele Porcelli
     */
    private class NsdProviderThread extends Thread {

        //Costanti che fungono da chiavi per il TXT record
        private static final String NICKNAME = "nickname";
        private static final String LISTEN_PORT = "listenport";

        //Costante del nome del servizio
        private static final String SERVICE_NAME = "WiChat";

        //Dizionario che conserva le coppie (indirizzo, nome) per l'associazione di un
        //nome più amichevole al dispositivo individuato
        private final HashMap<String, String> buddies = new HashMap<>();

        //Implementazione del listener dei TXT record
        private final WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {

            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
                if (fullDomainName.contains(SERVICE_NAME)) {
                    buddies.put(srcDevice.deviceAddress, txtRecordMap.get(NICKNAME));
                    servicesConnectionInfo.put(srcDevice.deviceAddress, Integer.parseInt(txtRecordMap.get(LISTEN_PORT)));
                }
            }
        };

        //Implementazione del listener del servizio
        private final WifiP2pManager.DnsSdServiceResponseListener serviceListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
                if (instanceName.contains(SERVICE_NAME)) {

                    //Aggiorna il nome del dispositivo con il nome amichevole fornito dal TXT record
                    //(se ne è arrivato uno)
                    srcDevice.deviceName = buddies.containsKey(srcDevice.deviceAddress) ? buddies.get(srcDevice.deviceAddress) : srcDevice.deviceName;

                    //Avvisa il ContactsListener del nuovo dispositivo trovato
                    if (mContactsListener != null)
                        mContactsListener.onContactFound(srcDevice);
                }
            }
        };

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

            //Crea il TXT record da inviare agli altri dispositivi che hanno installato WiChat
            Map<String, String> txtRecord = new HashMap<>();
            txtRecord.put(LISTEN_PORT, String.valueOf(port));
            txtRecord.put(NICKNAME, StructuredName.DISPLAY_NAME);

            //Istruzione posta per motivi di debug
            Log.d(LOG_TAG, "DISPLAY_NAME: " + StructuredName.DISPLAY_NAME);

            //Crea l'oggetto che conterrà le informazioni riguardo il servizio
            WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(SERVICE_NAME, "_presence._tcp", txtRecord);

            //Registra il servizio appena creato
            mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    //È andato tutto bene. Nulla da fare.
                }

                @Override
                public void onFailure(int reason) {

                    //Si è verificato un errore. Esso verrà registrato nel Log.
                    String errore = null;
                    switch (reason) {
                        case WifiP2pManager.P2P_UNSUPPORTED:
                            errore = "Wi-Fi P2P non supportato da questo dispositivo.";
                            break;
                        case WifiP2pManager.BUSY:
                            errore = "Sistema troppo occupato per elaborare la richiesta.";
                            break;
                        default:
                            errore = "Si è verificato un errore durante la registrazione del servizio WiChat.";
                            break;
                    }
                    Log.e(LOG_TAG, errore);
                }
            });

            //Registra i listener per i TXT record e per i servizi provenienti dai dispositivi in vicinanza
            mManager.setDnsSdResponseListeners(mChannel, serviceListener, txtRecordListener);

            //Avvia l'ascolto di connessioni in entrata
            while(!Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = server.accept();
                    ChatConnection chatConn = new ChatConnection(clientSocket);
                    connections.add(chatConn);
                }
                catch (IOException ex) {
                    //Errore durante la connessione con il client
                    Log.e(LOG_TAG, ex.toString());

                    //Ritorna in ascolto di altri client
                    continue;
                }

            }
        }
    }

    /**
     * Classe che si occupa di mantenere la connessione tra questo
     * dispositivo e quello con cui si è connesso (o che ha ricevuto la
     * connessione).
     *
     * @author Daniele Porcelli
     */
    private class ChatConnection {

        //Variabili d'istanza
        private Socket connSocket;
        private String macAddress;
        private SendingThread sendingThread;
        private ReceivingThread receivingThread;

        /**
         * Costruttore invocato dal server.
         *
         * @param socket Il socket che gestisce la connessione trai due dispositivi
         */
        public ChatConnection(Socket socket) throws IOException {
            connSocket = socket;

            //Crea il thread per la ricezione dei messaggi
            receivingThread = new ReceivingThread();

            //Crea il thread per l'invio dei messaggi
            sendingThread = new SendingThread();

            receivingThread.start();
            sendingThread.start();
        }

        /**
         * Costruttore invocato quando si vuole instaurare una
         * connessione con il server del dispositivo remoto.
         *
         * @param srvAddress L'indirizzo IP del dispositivo che ospita il server.
         * @param srvPort La porta sul quale è in ascolto il server.
         */
        public ChatConnection(InetAddress srvAddress, int srvPort, String macAddress) throws IOException {
            connSocket = new Socket(srvAddress, srvPort);
            this.macAddress = macAddress;

            //Crea il thread per la ricezione dei messaggi
            receivingThread = new ReceivingThread();

            //Crea il thread per l'invio dei messaggi
            sendingThread = new SendingThread();

            receivingThread.start();
            sendingThread.start();
        }


        /**
         * Spedisce il messaggio al thread designato all'invio dei messaggi (SendingThread).
         *
         * @param message Un'istanza di Message che rappresenta il messaggio composto dall'utente.
         */
        public void sendMessage(Message message) {
            sendingThread.deliverMessage(message);
        }

        /**
         * Imposta l'indirizzo MAC del dispositivo remoto con cui si
         * è connessi.
         *
         * @param macAddress L'indirizzo MAC del dispositivo con cui si è connessi in forma di stringa.
         */
        public void setMacAddress(String macAddress) {
            this.macAddress = macAddress;
        }

        /**
         * Restituisce l'indirizzo MAC del dispositivo remoto con il quale si è connessi.
         *
         * @return L'indirizzo MAC del dispositivo remoto in forma di stringa.
         */
        public String getMacAddress() {
            return macAddress;
        }

        /**
         * Classe interna che rappresenta il thread che si mette in ascolto
         * di messaggi provenienti dal dispositivo remoto.
         */
        private class ReceivingThread extends Thread {

            //Variabile d'istanza
            private ObjectInputStream objectInputStream;

            /**
             * Costruttore principale del thread.
             */

            public ReceivingThread() throws IOException {
                objectInputStream = new ObjectInputStream(connSocket.getInputStream());
            }
            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {

                            //Leggi il messaggio che hanno inviato
                            Message message = (Message) objectInputStream.readObject();
                            if (message != null) {
                                if (macAddress == null)
                                    macAddress = message.getSender();

                                //Manda il messaggio all'activity interessata se è registrata
                                if (mMessagesListener != null)
                                    mMessagesListener.onMessageReceived(message);

                                else if (mContactsListener != null) {

                                    //Manda il messaggio all'activity principale che notificherà
                                    //l'arrivo di un nuovo messaggio
                                    mContactsListener.onMessageReceived(message);

                                    //Salva il messaggio in memoria cosicché l'activity interessata
                                    //potrà recuperarlo e mostrarlo all'utente
                                    mMessagesStore.saveMessage(message);
                                }

                                else {

                                    //Salva il messaggio nella memoria interna e nient'altro
                                    mMessagesStore.saveMessage(message);
                                }
                            }

                        } catch (Exception ex) {

                            //In caso di errore, torna in ascolto di messaggi
                            Log.e(LOG_TAG, ex.toString());
                            continue;
                        }
                    }
                    objectInputStream.close();
                }
                catch(IOException ex) {
                    //Non è riuscito a chiudere lo stream
                    Log.e(LOG_TAG, ex.toString());
                }
            }
        }
    }
}
