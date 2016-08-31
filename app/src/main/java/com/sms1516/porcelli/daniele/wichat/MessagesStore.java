package com.sms1516.porcelli.daniele.wichat;

import android.content.Context;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.List;
import java.util.LinkedList;
import android.util.Log;

/**
 * Questa classe si occupa di salvare i messaggi
 * ricevuti (sia quelli letti che quelli ancora da leggere)
 * nella memoria di massa del dispositivo.
 */
public class MessagesStore {

    //Variabili d'istanza
    private Context context;

    //Variabili statiche
    private static MessagesStore instance;

    //Costante per il Log
    private static final String LOG_TAG = MessagesStore.class.getName();

    /**
     * Costruttore della classe.
     *
     * @param context Oggetto di tipo Context per ottenere le directory dove salvare i messaggi.
     */
    private MessagesStore(Context context) {
        this.context = context;
    }

    /**
     * Inizializza il MessagesStore. Questo metodo deve essere invocato
     * prima di ottenerne l'istanza.
     *
     * @param context Oggetto di tipo Context per ottenere le directory dove salvare i messaggi.
     */
    public static void initialize(Context context) {
        if (instance == null)
            instance = new MessagesStore(context);
    }

    /**
     * Salva in memoria interna il messaggio ricevuto.
     *
     * @param message Il messaggio ricevuto
     * @throws IOException Se si verifica un errore durante l'apertura del file
     * o durante la scrittura del messaggio su di esso.
     */
    public synchronized void saveMessage(Message message) {

        //Apre (e crea se non esistente) il file dove memorizzare il messaggio
        File messagesFile = new File(context.getFilesDir(), message.getSender().replace(":", ""));

        if (!messagesFile.exists()) {
            try {
                messagesFile.createNewFile();
            }
            catch (IOException ex) {
                //Non è stato possibile creare il file
                Log.e(LOG_TAG, "Impossibile creare file " + messagesFile.toString() + ": " + ex.toString());
                return;
            }
        }

        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(messagesFile, true);
            oos = new ObjectOutputStream(fos);
        }
        catch (FileNotFoundException ex) {
            //File non trovato
            Log.e(LOG_TAG, "Impossibile salvare il messaggio: file " + messagesFile.toString() + " non trovato.");
            return;
        }
        catch (IOException ex) {
            Log.e(LOG_TAG, "Impossibile inizializzare lo stream di output per salvare il messaggio: " + ex.toString());
            return;
        }

        try {
            //Scrive il messaggio sul file
            oos.writeObject(message);
        }
        catch (IOException ex) {
            Log.e(LOG_TAG, "Impossibile salvare il messaggio sul file " + messagesFile.toString() + ": " + ex.toString());
            return;
        }

        try {
            oos.flush();
        }
        catch (IOException ex) {
            Log.e(LOG_TAG, "Non è stato possibile fare il flush dello stream di output: " + ex.toString());
        }

        try {
            oos.close();
        }
        catch (IOException ex) {
            Log.e(LOG_TAG, "Non è stato possiblie chiudere il file: " + ex.toString());
        }
    }

    /**
     * Salva una lista di messaggi nella memoria interna.
     *
     * @param messageList La lista di messaggi da salvare.
     * @throws IOException Quando si verifica un errore durante le operazioni di I/O.
     */
    public synchronized void saveMessagesList(List<Message> messageList) {

        //Apre e crea il file dove verrà salvata la lista dei messaggi
        File messagesFile = new File(context.getFilesDir(), messageList.get(0).getSender().replace(":", ""));

        if (!messagesFile.exists()) {
            try {
                messagesFile.createNewFile();
            }
            catch (IOException ex) {
                //Non è stato possibile creare il file
                Log.e(LOG_TAG, "Impossibile creare file " + messagesFile.toString() + ": " + ex.toString());
                return;
            }
        }

        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(messagesFile, true);
            oos = new ObjectOutputStream(fos);
        }
        catch (FileNotFoundException ex) {
            //File non trovato
            Log.e(LOG_TAG, "Impossibile salvare il messaggio: file " + messagesFile.toString() + " non trovato.");
            return;
        }
        catch (IOException ex) {
            Log.e(LOG_TAG, "Impossibile inizializzare lo stream di output per salvare il messaggio: " + ex.toString());
            return;
        }

        //Scrive i messaggi sul file
        for (Message message: messageList) {
            try {
                oos.writeObject(message);
            }
            catch (IOException ex) {
                Log.e(LOG_TAG, "Impossibile salvare il messaggio nel file " + messagesFile.toString() + ": " + ex.toString());
            }
        }

        try {
            oos.flush();
        }
        catch (IOException ex) {
            Log.e(LOG_TAG, "Non è stato possibile fare il flush dello stream di output: " + ex.toString());
        }

        try {
            oos.close();
        }
        catch (IOException ex) {
            Log.e(LOG_TAG, "Non è stato possiblie chiudere il file: " + ex.toString());
        }
    }

    public synchronized List<Message> loadMessagesList(String device) {
        List<Message> messagesList = new LinkedList<>();

        //Apre il file per recuperare i messaggi presenti in esso
        File messagesFile = new File(context.getFilesDir(), device.replace(":", ""));

        FileInputStream fis = null;
        if(messagesFile.exists()) {
            try {
                fis = new FileInputStream(messagesFile);
            }
            catch (FileNotFoundException ex) {
                Log.e(LOG_TAG, "Impossibile recuperare i messaggi salvati: file " + messagesFile.toString() + " non trovato.");
                return messagesList;
            }

            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(fis);
            }
            catch (IOException ex) {
                Log.e(LOG_TAG, "Impossibile inizializzare lo stream di input per leggere i messaggi memorizzati: " + ex.toString());
                return messagesList;
            }

            //Crea la lista dei messaggi
            try {
                for (Message message = (Message) ois.readObject(); ; message = (Message) ois.readObject()) {
                    messagesList.add(message);
                }
            }
            catch (OptionalDataException ex) {
                Log.e(LOG_TAG, "Impossibile caricare tutti i messaggi dalla memoria interna: " + ex.toString());
            }
            catch(ClassNotFoundException ex) {
                Log.e(LOG_TAG, "Impossibile caricare tutti i messaggi dalla memoria interna: " + ex.toString());
            }
            catch(EOFException ex) {
                //Si è raggiunto la fine del file. Nulla di male.
            }
            catch (IOException ex) {
                Log.e(LOG_TAG, "Impossibile caricare tutti i messaggi dalla memoria interna: " + ex.toString());
            }
        }
        return messagesList;
    }

    public static MessagesStore getInstance() throws MessagesStoreNotInitializedException {
        if (instance != null)
            return instance;
        throw new MessagesStoreNotInitializedException();
    }
}
