package com.sms1516.porcelli.daniele.wichat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.app.IntentService;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.content.BroadcastReceiver;
import java.util.HashMap;
import java.util.List;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;


/** Questa classe rappresenta l'activity principale.
 * Nella schermata di questa activity saranno mostrati
 * i dispositivi (visualizzati in maniera user-friendly) che rientrano
 * nel range coperto dal segnale Wi-Fi dell'utente e con i
 * quali si può connettere e comunicare.
 * Questi ultimi possono essere utenti individuali
 * o gruppi.
 * Inoltre questa activity ha anche la responsabilità di
 * attivare il Wi-Fi P2P nel caso in cui quest'ultimo fosse
 * disattivato.
 *
 * @author Daniele Porcelli
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
}
