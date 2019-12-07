package backend.data;

import org.jetbrains.annotations.NotNull;

import java.util.TreeSet;

public class Groupe implements Comparable<Groupe> {

    private final Long mID;
    private final String mLabel;

    private TreeSet<Ticket> mTickets;

    public Groupe(final Long id, final String label) {
        mID    = id;
        mLabel = label;
    }

    public long getID() {
        return mID;
    }

    public String getLabel() {
        return mLabel;
    }

    @Override
    public int compareTo(@NotNull Groupe groupe) {
        return this.getLabel().compareTo(groupe.getLabel());
    }
}
