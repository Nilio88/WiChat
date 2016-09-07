package com.sms1516.porcelli.daniele.wichat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.content.Intent;
import android.app.ActivityOptions;
import java.util.HashMap;
import java.util.List;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;


/** Questa classe rappresenta l'activity principale.
 * Nella schermata di questa activity saranno mostrati
 * i dispositivi (visualizzati in maniera user-friendly, che indicheremo come "contatti")
 * che rientrano nel range coperto dal segnale Wi-Fi dell'utente e con i
 * quali si può connettere e comunicare.
 * Questi ultimi possono essere utenti individuali o gruppi.
 * Inoltre questa activity ha anche la responsabilità di
 * attivare il Wi-Fi P2P nel caso in cui quest'ultimo fosse disattivato.
 *
 * @author Daniele Porcelli
 */
public class MainActivity extends AppCompatActivity implements ContactSelectedListener, ContactsListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    @Override
    public void onContactSelected(String contact) {
        Intent intent = new Intent(this, ConversationActivity.class);

        //Aggiunge l'indirizzo MAC del dispositivo con il quale si vuole comunicare
        intent.putExtra(CostantKeys.INTENT_DEVICE_EXTRA, contact);

        //Avvia l'activity di conversazione in base alla versione di Android
        //in esecuzione: se è maggiore o uguale alla versione API Level 21,
        //allora avvia l'activity con l'animazione; altrimenti no.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            //Mi serve il riferimento alla View selezionata nel fragment
            //da parte dell'utente. Bisogna implementarlo in qualche modo...
            View commonView = null;
            //commonView = fragment.getView(contact) ad esempio

            Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(this, commonView, "contact").toBundle();

            //Chiama l'activity con la transizione animata.
            startActivity(intent, bundle);
        }

        else {
            //Chiama semplicemente l'activity passandogli l'indirizzo MAC del dispositivo
            startActivity(intent);
        }

    }
}
