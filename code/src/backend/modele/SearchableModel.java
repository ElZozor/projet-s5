package backend.modele;

import backend.data.ProjectTable;

import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.ListIterator;

public abstract class SearchableModel<T extends ProjectTable> implements TableModel {

    protected ArrayList<T> elements = new ArrayList<>();

    public abstract SearchableModel<T> retrieveSearchModel(final String searched);

    @Override
    public int getRowCount() {
        return elements.size();
    }

    public final boolean removeEntry(Long id) {
        ListIterator<T> ite = elements.listIterator();
        boolean deleted = false;

        for (; ite.hasNext() && !deleted; ) {
            T t = ite.next();
            System.out.println(t.getID() + " " + id);
            if (t.getID().equals(id)) {
                ite.previous();
                ite.remove();

                deleted = true;
            }
        }

        return deleted;
    }

    public final T getReferenceTo(Long id) {
        for (T t : elements) {
            if (t.getID().equals(id)) {
                return t;
            }
        }

        return null;
    }

    public final T getReferenceTo(int index) {
        return elements.get(index);
    }

    public void updateEntry(T updatedEntry) {
        removeEntry(updatedEntry.getID());
        elements.add(updatedEntry);
    }

    public void addRow(T ts) {
        ListIterator<T> iterator = elements.listIterator();

        boolean inserted = false;
        for (; iterator.hasNext() && !inserted; ) {
            T t = iterator.next();
            if (t.getID() > t.getID()) {
                iterator.previous();
                iterator.add(ts);
            }

            inserted = t.getID() >= ts.getID();
        }

        if (!inserted) {
            elements.add(ts);
        }
    }
}
