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

public class Ticket implements Comparable<Ticket> {

    private static final String KEY_MESSAGES = "messages";

    private SortedSet<Message> mMessages;

    private Long mID;
    private final String mTitre;

    public static Ticket fromJSON(JSONObject ticket) {
        Long id = ticket.getLong(TICKET_ID);
        String titre = ticket.getString(TICKET_TITRE);

        JSONArray array = ticket.getJSONArray(KEY_MESSAGES);
        SortedSet<Message> messages = new TreeSet<>();

        for (int i = 0; i < array.length(); ++i) {
            messages.add(Message.fromJSON(array.getJSONObject(i)));
        }

        return new Ticket(id, titre, messages);
    }

    public static JSONObject toJSON(Ticket ticket) {
        JSONObject ticketAsJSON = new JSONObject();

        ticketAsJSON.put(TICKET_ID, ticket.getID());
        ticketAsJSON.put(TICKET_TITRE, ticket.getID());

        JSONArray messages = new JSONArray();
        for (Message m : ticket.getMessages()) {
            messages.put(Message.toJSON(m));
        }

        ticketAsJSON.put(KEY_MESSAGES, messages);

        return ticketAsJSON;
    }

    public Ticket(String titre, SortedSet<Message> messages) throws NoSuchElementException {
        mMessages       = messages;
        mTitre          = titre;
    }

    public Ticket(Long id, String titre, SortedSet<Message> messages) throws NoSuchElementException {
        mID             = id;
        mMessages       = messages;
        mTitre          = titre;
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

    @Override
    public int compareTo(@NotNull Ticket ticket) {
        return this.getTitre().compareTo(ticket.getTitre());
    }
}
