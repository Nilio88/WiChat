package com.sms1516.porcelli.daniele.wichat;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.ChangeBounds;

/**
 * Questa Activity ha lo scopo di visualizzare i messaggi
 * scambiati tra gli interlocutori e di permettere all'utente
 * di comporre e inviare messaggi.
 * Come prima cosa, però, deve caricare nella UI la cronologia
 * dei messaggi scambiati, che sono stati salvati su un apposito file.
 * Questo file inoltre memorizza i messaggi ricevuti e non ancora
 * letti dall'utente. Infine, prima di chiudersi, deve salvare
 * i messaggi appena scambiati nel suddetto file.
 *
 * @author Daniele Porcelli, Giancosimo Montanaro
 */

public class ConversationActivity extends AppCompatActivity implements MessagesListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        //Controlla se la versione di Android è maggiore
        //o uguale alla API Level 21 (Lollipop) e, in tal caso,
        //registra la transizione animata per questa activity.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setSharedElementEnterTransition(new ChangeBounds());
        }
    }

    @Override
    protected void onDestroy() {

    }
}
