package backend.data;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class Ticket implements Comparable<Ticket> {

    private static final String KEY_ID              = "id";
    private static final String KEY_TITRE           = "titre";
    private static final String KEY_PREMIER_MESSAGE = "premier_message";
    private static final String KEY_DERNIER_MESSAGE = "dernier_message";
    private static final String KEY_MESSAGES        = "messages";

    private Set<Message> mMessages;

    private final Long mID;
    private final String mTitre;
    private final Message mPremierMessage;
    private final Message mDernierMessage;

    public static Ticket fromJSON(JSONObject ticket) {
        Long id = ticket.getLong(KEY_ID);
        String titre = ticket.getString(KEY_TITRE);

        JSONArray array = ticket.getJSONArray(KEY_MESSAGES);
        SortedSet<Message> messages = new TreeSet<>();

        for (int i = 0; i < array.length(); ++i) {
            messages.add(Message.fromJSON(array.getJSONObject(i)));
        }

        return new Ticket(id, titre, messages);
    }

    public static JSONObject toJSON(Ticket ticket) {
        JSONObject ticketAsJSON = new JSONObject();

        ticketAsJSON.put(KEY_ID, ticket.getID());
        ticketAsJSON.put(KEY_TITRE, ticket.getID());
        ticketAsJSON.put(KEY_PREMIER_MESSAGE, ticket.getID());
        ticketAsJSON.put(KEY_DERNIER_MESSAGE, ticket.getID());

        JSONArray messages = new JSONArray();
        for (Message m : ticket.getMessages()) {
            messages.put(Message.toJSON(m));
        }

        ticketAsJSON.put(KEY_MESSAGES, messages);

        return ticketAsJSON;
    }

    public Ticket(Long id, String titre, SortedSet<Message> messages) throws NoSuchElementException {
        mID             = id;
        mMessages       = messages;
        mTitre          = titre;
        mPremierMessage = messages.first();
        mDernierMessage = messages.last();
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

    public Message getPremierMessage() {
        return mPremierMessage;
    }

    public Message getDernierMessage() {
        return mDernierMessage;
    }

    @Override
    public int compareTo(@NotNull Ticket ticket) {
        return this.getTitre().compareTo(ticket.getTitre());
    }
}
