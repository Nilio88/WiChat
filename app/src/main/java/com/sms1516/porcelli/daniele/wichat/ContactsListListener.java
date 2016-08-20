package com.sms1516.porcelli.daniele.wichat;

import java.util.List;

/**
 * Questa interfaccia verrà usata dal servizio WiChatService
 * per astrarre l'attività che si registrerà come listener della
 * lista di dispositivi trovati tramite il network service discovery.
 *
 * @author Daniele Porcelli
 */
public interface ContactsListListener {

    /**
     * Metodo di callback invocato quando la lista dei contatti
     * è cambiata.
     *
     * @param contactsList La lista dei contatti aggiornata.
     */
    public void onContactsListChanged(List contactsList);
}
