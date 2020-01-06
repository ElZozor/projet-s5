package backend.modele;

import backend.data.Message;
import com.mysql.jdbc.StringUtils;

import javax.swing.event.TableModelListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MessageModel extends SearchableModel<Message> {
    private static final String[] columnNames = {
            "ID",
            "CONTENU",
            "HEURE ENVOI",
            "ID TICKET",
            "ID USER"
    };


    public MessageModel(ResultSet set) {
        try {
            for (; set.next(); ) {
                elements.add(new Message(set, new ArrayList<>(), new ArrayList<>()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private MessageModel() {

    }

    public MessageModel(List<Message> messages) {
        elements.addAll(messages);
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
        Message m = elements.get(ligne);

        switch (colonne) {
            case 0:
                return m.getID();
            case 1:
                String contenu = m.getContenu();
                if (contenu.length() < 20) {
                    return contenu;
                } else {
                    return contenu.substring(0, 15) + "[...]";
                }

            case 2:
                return m.getHeureEnvoie().toString();

            case 3:
                return m.getTicketID();

            case 4:
                return m.getUtilisateurID();

            default:
                return "ohohoh";
        }
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
    public SearchableModel<Message> retrieveSearchModel(String searched) {
        if (StringUtils.isEmptyOrWhitespaceOnly(searched)) {
            return this;
        }

        MessageModel messageModel = new MessageModel();

        Iterator<Message> ite = elements.iterator();
        for (; ite.hasNext(); ) {
            Message m = ite.next();
            final String[] strs = {m.getUtilisateurID().toString(), m.getTicketID().toString(), m.getHeureEnvoie().toString(), m.getContenu()};

            for (String s : strs) {
                int i = 0, j = 0;
                while (i < s.length() && j < searched.length()) {
                    if (s.charAt(i) == searched.charAt(j)) {
                        ++j;
                    }

                    ++i;
                }

                if (j == searched.length()) {
                    messageModel.addRow(m);
                    break;
                }
            }
        }

        return messageModel;
    }
}
