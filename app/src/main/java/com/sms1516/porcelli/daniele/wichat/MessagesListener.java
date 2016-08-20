package com.sms1516.porcelli.daniele.wichat;

/**
 * Questa interfaccia verrà usata dal servizio WiChatService
 * per astrarre l'attività che si registrerà come listener dei
 * messaggi ricevuti tramite il network service discovery.
 *
 * @author Daniele Porcelli
 */
public interface MessagesListener {

    /**
     * Metodo di callback invocato quando viene ricevuto
     * un nuovo messaggio.
     *
     * @param message Il messaggio ricevuto.
     */
    public void onMessageReceived(Message message);
}
