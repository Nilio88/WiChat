package com.sms1516.porcelli.daniele.wichat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.EOFException;

/**
 * Questa Activity ha lo scopo di visualizzare i messaggi
 * scambiati tra gli interlocutori e di permettere all'utente
 * di comporre e inviare i messaggi da lui scritti.
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
    }

    @Override
    protected void onDestroy() {
        //Ritorna alla MainActivity mediante la transizione animata.
        finishAfterTransition();
    }
}
