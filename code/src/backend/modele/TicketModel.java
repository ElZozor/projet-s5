package backend.modele;

import backend.data.Ticket;
import com.mysql.jdbc.StringUtils;

import javax.swing.event.TableModelListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.ListIterator;

public class TicketModel extends SearchableModel<Ticket> {
    private static final String[] columnNames = {
            "ID",
            "TITRE",
            "PREMIER_MESSAGE",
            "DERNIER MESSAGE",
            "ID USER",
            "ID GROUPE"
    };


    public TicketModel(ResultSet set) {
        try {
            for (; set.next(); ) {
                //tickets.add(new Ticket(set));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private TicketModel() {

    }

    public void addRow(Ticket newTicket) {
        ListIterator<Ticket> iterator = elements.listIterator();

        boolean inserted = false;
        for (; iterator.hasNext() && !inserted; ) {
            Ticket t = iterator.next();
            if (t.getID() > newTicket.getID()) {
                iterator.previous();
                iterator.add(newTicket);

                inserted = true;
            }
        }

        elements.add(newTicket);
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int i) {
        return columnNames[i];
    }

    @Override
    public Class<?> getColumnClass(int i) {
        if (i == 0) {
            return Integer.class;
        }

        Object o = getValueAt(0, i);
        if (o != null) {
            return o.getClass();
        }

        return "".getClass();
    }

    @Override
    public boolean isCellEditable(int i, int i1) {
        return false;
    }

    @Override
    public Object getValueAt(int ligne, int colonne) {
        Ticket t = elements.get(ligne);

        /*

        id_ticket       INT                 NOT NULL AUTO_INCREMENT,
                titre           VARCHAR(50)         NOT NULL,
        premier_message LONGTEXT            NOT NULL,
        dernier_message LONGTEXT            NOT NULL,
        id_util         INT                 NOT NULL,
        id_groupe       INT                 NOT NULL,
        switch (colonne) {
            case 0 : return t.getID();
            case 1 :
                return t.getTitre();

            case 2 :
                return t.premierMessage();

            case 3 :
                return t.dernierMessage();

            case 4 :
                return t.idUtilisateur();

            case 5:
                return t.idGroupe();

            default : return "ohohoh";
        }

        */

        return "";
    }

    @Override
    public void setValueAt(Object o, int ligne, int colonne) {

    }

    @Override
    public void addTableModelListener(TableModelListener tableModelListener) {

    }

    @Override
    public void removeTableModelListener(TableModelListener tableModelListener) {

    }

    @Override
    public SearchableModel<Ticket> retrieveSearchModel(String searched) {
        if (StringUtils.isEmptyOrWhitespaceOnly(searched)) {
            return this;
        }

        TicketModel ticketModel = new TicketModel();

        Iterator<Ticket> ite = elements.iterator();
        for (; ite.hasNext(); ) {
            Ticket t = ite.next();
            final String titre = t.getTitre();

            int i = 0, j = 0;
            while (i < titre.length() && j < searched.length()) {
                if (titre.charAt(i) == searched.charAt(j)) {
                    ++j;
                }

                ++i;
            }

            if (j == searched.length()) {
                ticketModel.addRow(t);
            }
        }

        return ticketModel;
    }
}
