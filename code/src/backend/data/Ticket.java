package backend.data;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.NoSuchElementException;
import java.util.TreeSet;

import static backend.database.Keys.TICKET_ID;
import static backend.database.Keys.TICKET_TITRE;

public class Ticket extends ProjectTable implements Comparable<Ticket> {

    private static final String KEY_MESSAGES = "messages";
    private String mTitre;
    private TreeSet<Message> mMessages = new TreeSet<>();
    private Long mID;
    
    /**
     * Constructeur de l'objet Ticket à partir d'un message au format json
     *
     * @param ticket objet au format json contenant les informations du ticket
    **/
    public Ticket(JSONObject ticket) {
        mID = ticket.getLong(TICKET_ID);
        mTitre = ticket.getString(TICKET_TITRE);

        JSONArray array = ticket.getJSONArray(KEY_MESSAGES);

        for (int i = 0; i < array.length(); ++i) {
            mMessages.add(new Message(array.getJSONObject(i)));
        }
    }
    
    /**
     * Constructeur de l'objet Ticket à partir de son titre et d'un ensemble de messages
     *
     * @param titre - titre du ticket
     * @param messages - ensemble de message triés sur le ticket
     * @throws NoSuchElementException peut etre renvoyé si messages est vide
    **/
    public Ticket(String titre, TreeSet<Message> messages) throws NoSuchElementException {
        mMessages = messages;
        mTitre = titre;
    }
    
    /**
     * Constructeur de l'objet Ticket à partir de son identifiant, de son titre et d'un ensemble de messages
     *
     * @param id - identifiant unique du ticket
     * @param titre - titre du ticket
     * @param message - ensemble de messages triés postés sur le ticket
     * @throws NoSuchElementException peut être renvoyé si message est vide
    **/
    public Ticket(Long id, String titre, TreeSet<Message> messages) throws NoSuchElementException {
        mID = id;
        mMessages = messages;
        mTitre = titre;
    }
    
    /**
     * methode codant un objet Ticket en un objet au format json
     *
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

        return ticketAsJSON;
    }
    
    /**
     * Accesseur sur l'ensemble des messages du ticket
     *
     * @return l'ensemble trié de messages présents sur le ticket
    **/
    public TreeSet<Message> getMessages() {
        return mMessages;
    }
    
    /**
     * Mutateur sur l'ensemble de message du ticket
     *
     * @param message - nouvel ensemble de messages posté sur le ticket
    **/
    public void setMessage(TreeSet<Message> message) {
        mMessages = message;
    }
    
    /**
     * Accesseur sur l'identifiant du ticket
     *
     * @return l'identifiant unique du ticket
    **/
    public Long getID() {
        return mID;
    }
    
    /**
     * Accesseur sur le titre du ticket
     *
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
        int titleComparison = this.getTitre().compareTo(ticket.getTitre());
        if (titleComparison == 0) {
            return getID().compareTo(ticket.getID());
        }

        return titleComparison;
    }

    @Override
    public String toString() {
        return getTitre();
    }

    public void setTitre(String titre) {
        mTitre = titre;
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
     *
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
}
