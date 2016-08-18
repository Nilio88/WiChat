package com.sms1516.porcelli.daniele.wichat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import java.lang.Thread;
import android.util.Log;

public class SplashScreen extends AppCompatActivity {

    //Costante per il tag di log
    private static final String TAG_LOG = SplashScreen.class.getName();

    //Costante per memorizzare il tempo di pausa dello splash screen
    private static final long PAUSA = 3000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        //Rimuove i pulsanti di navigazione dalla schermata
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);

    }

    @Override
    protected void onStart() {
        super.onStart();

        //Mette in pausa lo splash screen
        try {
            Thread.sleep(PAUSA);
        }
        catch (InterruptedException ex) {
            //In caso di interruzione, chiude l'applicazione
            Log.e(TAG_LOG, ex.toString());
            finish();
        }
    }
}
