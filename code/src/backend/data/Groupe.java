package backend.data;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeSet;

public class Groupe extends ProjectTable implements Comparable<Groupe> {

    private Long mID;
    private String mLabel;

    private TreeSet<Ticket> mTickets;

    public Groupe(final Long id, final String label) {
        mID = id;
        mLabel = label;
    }

    public Groupe(ResultSet set) throws SQLException {
        mID = set.getLong(1);
        mLabel = set.getString(2);
    }

    public Long getID() {
        return mID;
    }

    public String getLabel() {
        return mLabel;
    }

    public void setID(Long id) {
        mID = id;
    }

    public void setLabel(String label) {
        mLabel = label;
    }

    @Override
    public int compareTo(@NotNull Groupe groupe) {
        return this.getLabel().compareTo(groupe.getLabel());
    }
}
