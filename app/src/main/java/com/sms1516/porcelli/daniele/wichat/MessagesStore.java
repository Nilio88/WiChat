package com.sms1516.porcelli.daniele.wichat;

import android.content.Context;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.LinkedList;

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
    public void saveMessage(Message message) throws IOException {

        //Apre (e crea se non esistente) il file dove memorizzare il messaggio
        File messagesFile = new File(context.getFilesDir(), message.getSender().replace(":", ""));

        if (!messagesFile.exists())
            messagesFile.createNewFile();

        FileOutputStream fos = new FileOutputStream(messagesFile, true);
        ObjectOutputStream oos = new ObjectOutputStream(fos);

        //Scrive il messaggio sul file
        oos.writeObject(message);
        oos.flush();
        oos.close();
    }

    /**
     * Salva una lista di messaggi nella memoria interna.
     *
     * @param messageList La lista di messaggi da salvare.
     * @throws IOException Quando si verifica un errore durante le operazioni di I/O.
     */
    public void saveMessagesList(List<Message> messageList) throws IOException {

        //Apre e crea il file dove verrà salvata la lista dei messaggi
        File messagesFile = new File(context.getFilesDir(), messageList.get(0).getSender().replace(":", ""));

        if (!messagesFile.exists())
            messagesFile.createNewFile();

        FileOutputStream fos = new FileOutputStream(messagesFile, true);
        ObjectOutputStream oos = new ObjectOutputStream(fos);

        //Scrive i messaggi sul file
        for (Message message: messageList) {
            oos.writeObject(message);
        }

        oos.flush();
        oos.close();
    }

    public List<Message> loadMessagesList(String device) throws IOException, ClassNotFoundException {
        List<Message> messagesList = new LinkedList<>();

        //Apre il file per recuperare i messaggi salvati in esso
        File messagesFile = new File(context.getFilesDir(), device.replace(":", ""));
        if(messagesFile.exists()) {
            FileInputStream fis = new FileInputStream(messagesFile);
            ObjectInputStream ois = new ObjectInputStream(fis);

            //Crea la lista dei messaggi
            try {
                Message message = (Message) ois.readObject();
                while (message != null) {

                }
            }
            catch (EOFException ex) {

                //Abbiamo raggiunto la fine del file (corrotto o meno che sia),
                //quindi restituiamo la lista popolata fino a questo punto.
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
