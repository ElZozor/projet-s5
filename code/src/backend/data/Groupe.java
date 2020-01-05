package backend.data;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeSet;

import static backend.database.Keys.GROUPE_ID;
import static backend.database.Keys.GROUPE_LABEL;

public class Groupe extends ProjectTable implements Comparable<Groupe> {

    private Long mID;
    private String mLabel;

    private TreeSet<Ticket> mTickets = new TreeSet<>();

    public Groupe(final Long id, final String label) {
        mID = id;
        mLabel = label;
    }

    public Groupe(final Long id, final String label, final TreeSet<Ticket> tickets) {
        mID = id;
        mLabel = label;
        mTickets = tickets;
    }

    public Groupe(ResultSet set) throws SQLException {
        mID = set.getLong(1);
        mLabel = set.getString(2);
    }

    public Groupe(JSONObject jsonObject) {
        mID = jsonObject.getLong(GROUPE_ID);
        mLabel = jsonObject.getString(GROUPE_LABEL);

        JSONArray array = jsonObject.getJSONArray("tickets");
        for (int i = 0; i < array.length(); ++i) {
            JSONObject o = array.getJSONObject(i);

            mTickets.add(new Ticket(o));
        }
    }

    public Groupe(String label) {
        super();

        setLabel(label);
    }

    public Long getID() {
        return mID;
    }

    public void setID(Long id) {
        mID = id;
    }

    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        mLabel = label;
    }

    public void addTicket(Ticket ticket) {
        mTickets.add(ticket);
    }

    public TreeSet<Ticket> getTickets() {
        return mTickets;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put(GROUPE_ID, mID);
        jsonObject.put(GROUPE_LABEL, mLabel);

        JSONArray array = new JSONArray();
        for (Ticket ticket : mTickets) {
            array.put(ticket.toJSON());
        }

        jsonObject.put("tickets", array);

        return jsonObject;
    }

    @Override
    public int compareTo(@NotNull Groupe groupe) {
        return this.getLabel().compareTo(groupe.getLabel());
    }

    @Override
    public String toString() {
        return getLabel();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Groupe) {
            return getID().equals(((Groupe) obj).getID());
        }

        return false;
    }
}
