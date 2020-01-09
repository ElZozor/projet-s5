package backend.data;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import static backend.database.Keys.TICKET_ID;
import static backend.database.Keys.TICKET_TITRE;

public class Ticket extends ProjectTable implements Comparable<Ticket> {

    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_PENDING = "pendings";

    private Long mID;
    private String mTitre;
    private TreeSet<Message> mMessages = new TreeSet<>();
    private ArrayList<Message> pendingMessages = new ArrayList<>();



    /**
     * Constructeur de l'objet Ticket à partir d'un message au format json
     * @param ticket objet au format json contenant les informations du ticket
     **/
    public Ticket(JSONObject ticket) {
        mID = ticket.getLong(TICKET_ID);
        mTitre = ticket.getString(TICKET_TITRE);

        JSONArray array = ticket.getJSONArray(KEY_MESSAGES);
        for (int i = 0; i < array.length(); ++i) {
            mMessages.add(new Message(array.getJSONObject(i)));
        }

        array = ticket.getJSONArray(KEY_PENDING);
        for (int i = 0; i < array.length(); ++i) {
            pendingMessages.add(new Message(array.getJSONObject(i)));
        }
    }


    /**
     * Constructeur de l'objet Ticket à partir de son titre et d'un ensemble de messages
     *
     * @param titre    - titre du ticket
     * @param messages - ensemble de message triés sur le ticket
     * @throws NoSuchElementException peut etre renvoyé si messages est vide
     **/
    public Ticket(String titre, TreeSet<Message> messages) throws NoSuchElementException {
        mMessages = messages;
        mTitre = titre;
    }


    /**
     * Constructeur de l'objet Ticket à partir de son identifiant, de son titre et d'un ensemble de messages
     * @param id       - identifiant unique du ticket
     * @param titre    - titre du ticket
     * @param messages - ensemble de messages triés postés sur le ticket
     * @throws NoSuchElementException peut être renvoyé si message est vide
     **/
    public Ticket(Long id, String titre, TreeSet<Message> messages) throws NoSuchElementException {
        mID = id;
        mMessages = messages;
        mTitre = titre;
    }


    /**
     * methode codant un objet Ticket en un objet au format json
     * @return un objet json contenant les information du ticket
    **/
    public JSONObject toJSON() {
        JSONObject ticketAsJSON = new JSONObject();

        ticketAsJSON.put(TICKET_ID, getID());
        ticketAsJSON.put(TICKET_TITRE, getTitre());

        JSONArray messages = new JSONArray();
        for (Message m : getMessages()) {
            messages.put(m.toJSON());
        }
        ticketAsJSON.put(KEY_MESSAGES, messages);

        messages = new JSONArray();
        for (Message m : pendingMessages) {
            messages.put(m.toJSON());
        }
        ticketAsJSON.put(KEY_PENDING, messages);

        return ticketAsJSON;
    }


    /**
     * Accesseur sur l'ensemble des messages du ticket
     * @return l'ensemble trié de messages présents sur le ticket
    **/
    public TreeSet<Message> getMessages() {
        return mMessages;
    }


    /**
     * Mutateur sur l'ensemble de message du ticket
     * @param message - nouvel ensemble de messages posté sur le ticket
    **/
    public void setMessage(TreeSet<Message> message) {
        mMessages = message;
    }


    /**
     * Accesseur sur l'identifiant du ticket
     * @return l'identifiant unique du ticket
    **/
    public Long getID() {
        return mID;
    }


    /**
     * Accesseur sur le titre du ticket
     * @return titre du ticket
    **/
    public String getTitre() {
        return mTitre;
    }


    /**
     * Accesseur sur le premier message du ticket
     *
     * @return l'objet Message étant le premier message si il y a des message sur le ticket. null sinon
    **/
    public Message premierMessage() {
        if (mMessages.size() > 0) {
            return mMessages.first();
        }

        return null;
    }


     /**
     * Accesseur sur le dernier message du ticket
     *
     * @return l'objet Message étant le dernier message si il y a des message sur le ticket. null sinon
    **/
    public Message dernierMessage() {
        if (mMessages.size() > 0) {
            return mMessages.last();
        }

        return null;
    }


    /**
     * Methode ajoutant un message à l'ensemble des messages sur le ticket
     *
     * @param message - message à ajouter à l'ensemble de messages postés sur le ticket
    **/
    public void addMessage(Message message) {
        mMessages.add(message);
    }


    @Override
    public int compareTo(@NotNull Ticket ticket) {
        Message lastOther = ticket.dernierMessage();
        Message lastThis = dernierMessage();

        int messageComparison = lastOther.compareTo(lastThis);
        if (messageComparison == 0) {
            return getID().compareTo(ticket.getID());
        }

        System.out.println(String.format("%s and %s ", getTitre(), ticket.getTitre()));
        return messageComparison;
    }

    @Override
    public String toString() {
        return getTitre();
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Ticket) {
            return getID().equals(((Ticket) obj).getID());
        }

        return false;
    }


    /**
     * Methode comptant le nombre de messages non lus par tous
     * @return le nombre de messages n'ayant pas été lus par tous
    **/
    public int getNotSeenMessages() {
        int result = 0;
        if (mMessages != null) {
            for (Message message : mMessages) {
                if (message.state() < 4) {
                    ++result;
                }
            }
        }

        return result;
    }


    /**
     * @return true si il y a des messages non lus
     */
    public boolean containsUnreadMessages() {
        return getNotSeenMessages() > 0;
    }

    /**
     * @return true si il y a des utilisateurs qui n'ont pas reçu ce message
     */
    public boolean containsUnreceivedMessages() {
        if (mMessages != null) {
            for (Message message : mMessages) {
                if (message.state() < 3) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @return true si non lu ou non reçu
     */
    public boolean containsUnreadOrUnreceivedMessages() {
        return containsUnreceivedMessages() || containsUnreadMessages();
    }

    /**
     * Ajoute un message en attente de confirmation par le serveur
     *
     * @param message - Le message à mettre en attente
     */
    public void addPendingMessage(Message message) {
        pendingMessages.add(message);
    }

    /**
     * Met à jour le ticket courant en remplaçant ses
     * messages par le ticket passé en paramètre.
     * Ne touche pas aux messages en attente à part
     * s'il sont présent dans les messages reçus.
     *
     * @param ticket - Le nouveau ticket
     */
    public void merge(Ticket ticket) {
        mMessages = ticket.getMessages();

        if (!pendingMessages.isEmpty()) {
            for (Message message : mMessages) {
                removeUselessMessage(message);
            }
        }
    }

    /**
     * Remplace le message correspondant par sa nouvelle valeur
     * et supprime le message en attente correspondant si présent
     *
     * @param entryAsMessage - Le nouveau message
     */
    public void merge(Message entryAsMessage) {
        mMessages.remove(entryAsMessage);
        mMessages.add(entryAsMessage);

        removeUselessMessage(entryAsMessage);
    }

    /**
     * Supprime les messages en attente reçus.
     *
     * @param message - Le nouveau message
     */
    private void removeUselessMessage(Message message) {
        if (!pendingMessages.isEmpty()) {
            for (int i = 0; i < pendingMessages.size(); ++i) {
                if (message.getUtilisateurID().equals(pendingMessages.get(i).getUtilisateurID())
                        && pendingMessages.get(i).getContenu().equals(message.getContenu())) {
                    pendingMessages.remove(i);
                    break;
                }
            }
        }
    }


    /**
     * Retourne tous les messages y compris ceux en attente.
     *
     * @return - Tous les messages.
     */
    public ArrayList<Message> getMessagesWithPending() {
        ArrayList<Message> all = new ArrayList<>(mMessages);
        all.addAll(pendingMessages);

        return all;
    }
}
