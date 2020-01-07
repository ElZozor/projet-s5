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

    public Ticket(JSONObject ticket) {
        mID = ticket.getLong(TICKET_ID);
        mTitre = ticket.getString(TICKET_TITRE);

        JSONArray array = ticket.getJSONArray(KEY_MESSAGES);

        for (int i = 0; i < array.length(); ++i) {
            mMessages.add(new Message(array.getJSONObject(i)));
        }
    }

    public Ticket(String titre, TreeSet<Message> messages) throws NoSuchElementException {
        mMessages = messages;
        mTitre = titre;
    }

    public Ticket(Long id, String titre, TreeSet<Message> messages) throws NoSuchElementException {
        mID = id;
        mMessages = messages;
        mTitre = titre;
    }

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

    public TreeSet<Message> getMessages() {
        return mMessages;
    }

    public void setMessage(TreeSet<Message> message) {
        mMessages = message;
    }

    public Long getID() {
        return mID;
    }

    public String getTitre() {
        return mTitre;
    }

    public Message premierMessage() {
        if (mMessages.size() > 0) {
            return mMessages.first();
        }

        return null;
    }

    public Message dernierMessage() {
        if (mMessages.size() > 0) {
            return mMessages.last();
        }

        return null;
    }

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

        return messageComparison;
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
