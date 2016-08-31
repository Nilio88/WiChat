package com.sms1516.porcelli.daniele.wichat;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pInfo;
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

import java.io.EOFException;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

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
    private Thread mNsdService;
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

    //Costante per il Log
    private static final String LOG_TAG = WiChatService.class.getName();

    //Costanti per le azioni e i parametri degli intent
    private static final String ACTION_START_NSD_SERVICE = "com.sms1516.porcelli.daniele.wichat.action.START_NSD_SERVICE";

    private static final String ACTION_CONNECT_TO_CLIENT = "com.sms1516.porcelli.daniele.wichat.action.CONNECT_TO_CLIENT";
    private static final String ACTION_CONNECT_TO_CLIENT_EXTRA = "com.sms1516.porcelli.daniele.wichat.extra.DEVICE";

    private static final String ACTION_DISCOVER_SERVICES = "com.sms1516.porcelli.daniele.wichat.action.DISCOVER_SERVICES";

    private static final String ACTION_REGISTER_CONTACTS_LISTENER = "com.sms1516.porcelli.daniele.wichat.action.REGISTER_CONTACTS_LISTENER";
    private static final String ACTION_REGISTER_CONTACTS_LISTENER_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.REGISTER_CONTACTS_LISTENER_EXTRA";

    private static final String ACTION_UNREGISTER_CONTACTS_LISTENER = "com.sms1516.porcelli.daniele.wichat.action.UNREGISTER_CONTACTS_LISTENER";

    private static final String ACTION_REGISTER_MESSAGES_LISTENER = "com.sms1516.porcelli.daniele.wichat.action.REGISTER_MESSAGES_LISTENER";
    private static final String ACTION_REGISTER_MESSAGES_LISTENER_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.REGISTER_MESSAGES_LISTENER_EXTRA";

    private static final String ACTION_UNREGISTER_MESSAGES_LISTENER = "com.sms1516.porcelli.daniele.wichat.action.UNREGISTER_MESSAGES_LISTENER";

    private static final String ACTION_SEND_MESSAGE = "com.sms1516.porcelli.daniele.wichat.action.ACTION_SEND_MESSAGE";
    private static final String ACTION_SEND_MESSAGE_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.ACTION_SEND_MESSAGE_EXTRA";
    private static final String ACTION_SEND_MESSAGE_RECIPIENT_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.ACTION_SEND_MESSAGE_EXTRA";


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
            mReceiver = new WifiP2pBroadcastReceiver();
            registerReceiver(mReceiver, mIntentFilter);
        }

        else if(intent.getAction().equals(ACTION_START_NSD_SERVICE)) {

            //Avvia il servizio di network service discovery locale
            mNsdService = new NsdProviderThread();
            mNsdService.start();
        }

        else if (intent.getAction().equals(ACTION_CONNECT_TO_CLIENT)) {

            //Recupera l'indirizzo MAC del dispositivo a cui connettersi
            String device = intent.getStringExtra(ACTION_CONNECT_TO_CLIENT_EXTRA);

            //Si connette con il dispositivo tramite Wi-Fi direct
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device;
            config.wps.setup = WpsInfo.PBC;

            //Imposta il dispositivo da connettersi nel BroadcastReceiver
            ((WifiP2pBroadcastReceiver)mReceiver).setDeviceToConnect(device);

            mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    //Tutto ok, nulla da fare.
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(LOG_TAG, "Impossibile collegarsi con il client tramite Wi-Fi P2P.");
                }
            });
        }

        else if (intent.getAction().equals(ACTION_DISCOVER_SERVICES)) {

            //Registra la richiesta
            WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

            //Per sicurezza, rimuovi ogni richiesta dal WifiP2pManager
            mManager.clearServiceRequests(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    //Tutto ok. Nulla da fare
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(LOG_TAG, "Impossibile rimuovere le richieste di servizio dal manager: clearServiceRequest failed.");
                }
            });

            mManager.addServiceRequest(mChannel, serviceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    //Tutto ok. Nulla da fare.
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(LOG_TAG, "Impossibile ottenere le informazioni di connessione: AddServiceRequest failed");
                }
            });

            //Avvia la ricerca di dispositivi nelle vicinanze con lo stesso servizio WiChat
            mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    //Tutto bene. Nulla da fare.
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
                            errore = "sistema troppo occupato per elaborare la richiesta.";
                            break;
                        default:
                            errore = "si è verificato un errore durante la registrazione del servizio WiChat.";
                            break;
                    }

                    Log.e(LOG_TAG, "Impossibile iniziare la ricerca dei peers: " + errore);
                }
            });
        }

        else if (intent.getAction().equals(ACTION_REGISTER_CONTACTS_LISTENER)) {

            //Registra il ContactsListener
            mContactsListener = (ContactsListener) intent.getSerializableExtra(ACTION_REGISTER_CONTACTS_LISTENER_EXTRA);
        }

        else if (intent.getAction().equals(ACTION_UNREGISTER_CONTACTS_LISTENER)) {

            //Rimuovi il ContactsListener
            mContactsListener = null;
        }

        else if (intent.getAction().equals(ACTION_REGISTER_MESSAGES_LISTENER)) {

            //Registra il MessagesListener
            mMessagesListener = (MessagesListener) intent.getSerializableExtra(ACTION_REGISTER_MESSAGES_LISTENER_EXTRA);
        }

        else if (intent.getAction().equals(ACTION_UNREGISTER_MESSAGES_LISTENER)) {

            //Rimuovi il MessagesListener
            mMessagesListener = null;
        }

        else if (intent.getAction().equals(ACTION_SEND_MESSAGE)) {

            //Recupera il messaggio da inviare
            Message message = (Message) intent.getSerializableExtra(ACTION_SEND_MESSAGE_EXTRA);

            //Recupera il destinatario
            String recipient = intent.getStringExtra(ACTION_SEND_MESSAGE_RECIPIENT_EXTRA);

            boolean messageSent = false;

            //Cerca la connessione con il dato destinatario
            for (ChatConnection conn: connections) {
                if (conn.getMacAddress().equals(recipient)) {

                    //Invia il messaggio a questa connessione
                    conn.sendMessage(message);
                    messageSent = true;
                }
            }

            if (!messageSent)
                Log.e(LOG_TAG, "Messaggio non inviato: non esiste alcuna connessione con " + recipient);
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
     *
     * @param context L'oggetto di tipo Context che rappresenta il contesto dell'applicazione.
     */
    public static void registerNsdService(Context context) {
        Intent startNsdIntent = new Intent(context, WiChatService.class);
        startNsdIntent.setAction(ACTION_START_NSD_SERVICE);
        context.startService(startNsdIntent);
    }

    /**
     * Metodo statico invocato dall'activity principale per
     * connettersi e avviare una coversazione con il dispositivo
     * selezionato dall'utente.
     *
     * @param context L'oggetto di tipo Context che rappresenta il contesto dell'applicazione.
     * @param device Indirizzo MAC del dispositivo a cui connettersi rappresentato in forma testuale.
     */
    public static void connectToClient(Context context, String device) {
        Intent connectToClientIntent = new Intent(context, WiChatService.class);
        connectToClientIntent.setAction(ACTION_CONNECT_TO_CLIENT);
        connectToClientIntent.putExtra(ACTION_CONNECT_TO_CLIENT_EXTRA, device);
        context.startService(connectToClientIntent);
    }

    /**
     * Metodo statico invocato dall'activity principale per
     * avviare la ricerca dei dispositivi nel raggio del segnale
     * Wi-Fi e popolare la lista.
     *
     * @param context L'oggetto di tipo Context che rappresenta il contesto dell'applicazione.
     */
    public static void discoverServices(Context context) {
        Intent discoverServicesIntent = new Intent(context, WiChatService.class);
        discoverServicesIntent.setAction(ACTION_DISCOVER_SERVICES);
        context.startService(discoverServicesIntent);
    }

    /**
     * Registra in WiChatService l'activity interessata ai contatti.
     *
     * @param context L'oggetto di tipo Context che rappresenta il contesto dell'applicazione.
     * @param contactsListener Una activity che implementa l'interfaccia ContactsListener.
     */
    public static void registerContactsListener(Context context, ContactsListener contactsListener){
        Intent registerContactsListenerIntent = new Intent(context, WiChatService.class);
        registerContactsListenerIntent.setAction(ACTION_REGISTER_CONTACTS_LISTENER);
        registerContactsListenerIntent.putExtra(ACTION_REGISTER_CONTACTS_LISTENER_EXTRA, contactsListener);
        context.startService(registerContactsListenerIntent);
    }

    /**
     * Rimuove da WiChatService l'activity interessata ai contatti.
     *
     * @param context L'oggetto di tipo Context che rappresenta il contesto dell'applicazione.
     */
    public static void unRegisterContactsListener(Context context) {
        Intent unRegisterContactsListenerIntent = new Intent(context, WiChatService.class);
        unRegisterContactsListenerIntent.setAction(ACTION_UNREGISTER_CONTACTS_LISTENER);
        context.startService(unRegisterContactsListenerIntent);
    }

    /**
     * Registra in WiChatService l'activity interessata ai messaggi.
     *
     * @param context L'oggetto di tipo Context che rappresenta il contesto dell'applicazione.
     * @param messagesListener Una activity che implementa l'interfaccia MessagesListener.
     */
    public static void registerMessagesListener(Context context, MessagesListener messagesListener) {
        Intent registerMessagesListenerIntent = new Intent(context, WiChatService.class);
        registerMessagesListenerIntent.setAction(ACTION_REGISTER_MESSAGES_LISTENER);
        registerMessagesListenerIntent.putExtra(ACTION_REGISTER_MESSAGES_LISTENER_EXTRA, messagesListener);
        context.startService(registerMessagesListenerIntent);
    }

    /**
     * Rimuove da WiChatService l'activity interessata ai messaggi.
     *
     * @param context L'oggetto di tipo Context che rappresenta il contesto dell'applicazione.
     */
    public static void unRegisterMessagesListener(Context context) {
        Intent unRegisterMessagesListenerIntent = new Intent(context, WiChatService.class);
        unRegisterMessagesListenerIntent.setAction(ACTION_UNREGISTER_MESSAGES_LISTENER);
        context.startService(unRegisterMessagesListenerIntent);
    }

    public static void sendMessage(Context context, Message message, String recipient) {
        Intent sendMessageIntent = new Intent(context, WiChatService.class);
        sendMessageIntent.setAction(ACTION_SEND_MESSAGE);
        sendMessageIntent.putExtra(ACTION_SEND_MESSAGE_EXTRA, message);
        sendMessageIntent.putExtra(ACTION_SEND_MESSAGE_RECIPIENT_EXTRA, recipient);
        context.startService(sendMessageIntent);
    }

    /**
     * Broadcast Receiver per gli Intent di broadcast
     * provenienti dal framework Wi-Fi P2P.
     *
     * @author Daniele Porcelli
     */
    private class WifiP2pBroadcastReceiver extends BroadcastReceiver {

        //Variabile per tener traccia del dispositivo a cui si sta connettendo.
        //L'ho inserito per poter ottenere l'indirizzo IP del dispositivo quando
        //viene chiamato requestConnectionInfo() dopo aver ricevuto l'intent di
        //broadcast WIFI_P2P_CONNECTION_CHANGED_ACTION
        private String device;

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

        //Implementazione del ConnectionInfoListener per recuperare l'indirizzo IP
        //del dispositivo a cui si è appena connessi
        private WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {

            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {

                //Ottieni l'indirizzo IP
                InetAddress deviceIP = info.groupOwnerAddress;

                //Ottieni la porta del servizio remoto
                int port = servicesConnectionInfo.get(device);

                //Crea e avvia la connessione
                try {
                    ChatConnection chatConnection = new ChatConnection(deviceIP, port, device);
                    connections.add(chatConnection);
                }
                catch (IOException ex) {
                    Log.e(LOG_TAG, "Non è stato possibile connettersi con " + device + ": " + ex.toString());
                    mContactsListener.onContactDisconnected(device);
                }
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

                //Questo dispositivo si è appena connesso con un altro tramite Wi-Fi direct.
                //Recuperiamo le informazioni di connessione di conseguenza.
                if (mManager == null) {
                    return;
                }

                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {

                    //Si è appena connesso al dispositivo remoto, otteniamo le informazioni della connessione
                    mManager.requestConnectionInfo(mChannel, connectionInfoListener);
                }
            }

            else if (action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {

                //Recupera la nuova lista di contatti disponibili nel range del Wi-Fi
                //Nota: probabilmente neanche questo ci servirà, ma lo teniamo per sicurezza.
                //mManager.requestPeers(mChannel, peerListListener);
            }


        }

        /**
         * Metodo invocato quando si vuole impostare il dispositivo di cui
         * si vuole ricavare l'indirizzo IP per il socket.
         *
         * @param device L'indirizzo MAC del dispositivo a cui ci si sta connettendo.
         */
        public void setDeviceToConnect(String device) {
            this.device = device;
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
                    if (mContactsListener != null) {
                        synchronized (mContactsListener) {
                            mContactsListener.onContactFound(srcDevice);
                        }
                    }
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
                Log.e(LOG_TAG, "Impossibile avviare il server: " + ex.toString());
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
                Socket clientSocket = null;
                try {
                    clientSocket = server.accept();
                }
                catch  (IOException ex) {
                    Log.e(LOG_TAG, "Impossibile avviare il server: " + ex.toString());
                    break;
                }
                try {
                    ChatConnection chatConn = new ChatConnection(clientSocket);
                    synchronized (connections) {
                        connections.add(chatConn);
                    }
                }
                catch (IOException ex) {
                    //Errore durante la connessione con il client
                    Log.e(LOG_TAG, "Errore durante la connessione con il client: " + ex.toString());

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
             *
             * @throws IOException se non riesce ad inizializzare lo stream di input.
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
                                if (mMessagesListener != null) {
                                    synchronized (mMessagesListener) {
                                        if (mMessagesListener.getRecipient().equals(macAddress))
                                            mMessagesListener.onMessageReceived(message);
                                    }
                                }
                                else if (mContactsListener != null) {

                                    //Manda il messaggio all'activity principale che notificherà
                                    //l'arrivo di un nuovo messaggio
                                    synchronized (mContactsListener) {
                                        mContactsListener.onMessageReceived(message);
                                    }

                                    //Salva il messaggio in memoria cosicché l'activity interessata
                                    //potrà recuperarlo e mostrarlo all'utente
                                    mMessagesStore.saveMessage(message);
                                }

                                else {

                                    //Salva il messaggio nella memoria interna e nient'altro
                                    mMessagesStore.saveMessage(message);
                                }
                            }

                        } catch (ClassNotFoundException ex) {

                            //In caso di errore, interrompi il ciclo
                            Log.e(LOG_TAG, ex.toString());
                            break;
                        }
                        catch (EOFException ex) {

                            //Questa eccezione indica che il dispositivo remoto ha chiuso lo stream
                            //di output. Quindi chiudi la connessione.
                            Log.e(LOG_TAG, "Il client si è disconnesso: " + ex.toString());
                            break;
                        }
                    }
                    objectInputStream.close();
                    closeConnection();
                }
                catch(IOException ex) {
                    //Non è riuscito a chiudere lo stream
                    Log.e(LOG_TAG, "Impossibile chiudere lo stream di input dei messaggi: " + ex.toString());
                }
            }
        }

        /**
         * Thread che si occupa dell'invio dei messaggi al
         * dispositivo remoto a cui si è connessi.
         *
         * @author Daniele Porcelli
         */
        private class SendingThread extends Thread {

            //Variabili d'istanza
            private BlockingQueue<Message> messagesQueue;
            ObjectOutputStream oos;

            //Costanti statiche
            private static final int QUEUE_CAPACITY = 10;

            /**
             * Costruttore del thread.
             *
             * @Throws IOException se non riesce ad inizializzare lo stream di output.
             */
            public SendingThread() throws IOException {

                //Inizializza la coda dei messaggi da inviare
                messagesQueue = new ArrayBlockingQueue<Message>(QUEUE_CAPACITY);

                //Inizializza lo stream di output
                oos = new ObjectOutputStream(connSocket.getOutputStream());
            }

            @Override
            public void run() {

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        //Rimane in ascolto per eventuali messaggi da inviare
                        Message messageToSend = messagesQueue.take();

                        //Manda il messaggio appena ottenuto dalla coda dei messaggi
                        oos.writeObject(messageToSend);
                    }
                    catch (IOException ex) {
                        //Errore durante l'invio del messaggio prelevato
                        Log.e(LOG_TAG, "Errore durante l'invio del messaggio: " + ex.toString());
                        break;
                    }
                    catch (InterruptedException ex) {
                        //Si è verificata un'interruzione durante l'ottenimento
                        //del messaggio da inviare
                        Log.e(LOG_TAG, "Interruzione durante il prelevamento del messaggio da inviare: " + ex.toString());
                        break;
                    }
                }

                //Chiudi lo stream di output
                try {
                    oos.close();
                }
                catch (IOException ex) {

                    //Segnala l'eccezione, nulla di più
                    Log.e(LOG_TAG, "Errore durante la chiusura dello stream di output: " + ex.toString());
                }
            }

            /**
             * Inserisce nella coda dei messaggi il messaggio scritto dall'utente.
             *
             * @param message Il messaggio scritto dall'utente.
             */
            public void deliverMessage(Message message) {
                messagesQueue.add(message);
            }
        }

        public void closeConnection() {

            //Arresta i thread di ricezione e invio dei messaggi
            if (!sendingThread.isInterrupted())
                sendingThread.interrupt();
            if (!receivingThread.isInterrupted())
                receivingThread.interrupt();

            //Chiude il socket di comunicazione
            try {
                connSocket.close();
            }
            catch (IOException ex) {
                Log.e(LOG_TAG, "Errore durante la chiusura del socket: " + ex.toString());
            }

            //Rimuove questa connessione dalla lista delle connessioni attive
            synchronized (connections) {
                connections.remove(this);
            }

            //Informa le activity della rimozione del dispositivo
            synchronized (mContactsListener) {
                mContactsListener.onContactDisconnected(macAddress);
            }

            if (mMessagesListener != null) {
                synchronized (mMessagesListener) {
                    if (mMessagesListener.getRecipient().equals(macAddress))
                        mMessagesListener.onContactDisconnected();
                }
            }
        }
    }
}
