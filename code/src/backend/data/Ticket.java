package backend.data;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static backend.database.Keys.TICKET_ID;
import static backend.database.Keys.TICKET_TITRE;

public class Ticket extends ProjectTable implements Comparable<Ticket> {

    private static final String KEY_MESSAGES = "messages";
    private String mTitre;
    private SortedSet<Message> mMessages = new TreeSet<>();
    private Long mID;

    public Ticket(JSONObject ticket) {
        mID = ticket.getLong(TICKET_ID);
        mTitre = ticket.getString(TICKET_TITRE);

        JSONArray array = ticket.getJSONArray(KEY_MESSAGES);

        for (int i = 0; i < array.length(); ++i) {
            mMessages.add(new Message(array.getJSONObject(i)));
        }
    }

    public Ticket(String titre, SortedSet<Message> messages) throws NoSuchElementException {
        mMessages = messages;
        mTitre = titre;
    }

    public Ticket(Long id, String titre, SortedSet<Message> messages) throws NoSuchElementException {
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

    public Set<Message> getMessages() {
        return mMessages;
    }

    public Long getID() {
        return mID;
    }

    public String getTitre() {
        return mTitre;
    }

    public Message premierMessage() {
        return mMessages.first();
    }

    public Message dernierMessage() {
        return mMessages.last();
    }

    public void addMessage(Message message) {
        mMessages.add(message);
    }

    @Override
    public int compareTo(@NotNull Ticket ticket) {
        return this.getTitre().compareTo(ticket.getTitre());
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
}
