package backend.modele;

import backend.data.Utilisateur;
import com.mysql.jdbc.StringUtils;

import javax.swing.event.TableModelListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.ListIterator;

public class UserModel extends SearchableModel<Utilisateur> {

    private static final String[] columnNames = {
            "ID",
            "INE",
            "NOM",
            "PRENOM",
            "TYPE"
    };


    public UserModel(ResultSet set) {
        try {
            for (; set.next(); ) {
                elements.add(new Utilisateur(set));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private UserModel() {

    }

    public void addRow(Utilisateur newUser) {
        ListIterator<Utilisateur> iterator = elements.listIterator();

        boolean inserted = false;
        for (; iterator.hasNext() && !inserted; ) {
            Utilisateur u = iterator.next();
            if (u.getID() > newUser.getID()) {
                iterator.previous();
                iterator.add(newUser);

                inserted = true;
            }
        }

        elements.add(newUser);
    }

    @Override
    public int getRowCount() {
        return elements.size();
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
        Utilisateur u = elements.get(ligne);

        switch (colonne) {
            case 0:
                return u.getID();
            case 1:
                return u.getINE();
            case 2:
                return u.getNom();
            case 3:
                return u.getPrenom();
            case 4:
                return u.getType();
            case 5:
                return "ohohoh";
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object o, int ligne, int colonne) {
        Utilisateur u = elements.get(ligne);

        switch (colonne) {
            case 0:
                u.setINE((String) o);
                break;
            case 1:
                u.setNom((String) o);
                break;
            case 2:
                u.setPrenom((String) o);
                break;

            case 4:
                u.setType((String) o);
                break;

            default:
                break;
        }
    }

    @Override
    public void addTableModelListener(TableModelListener tableModelListener) {

    }

    @Override
    public void removeTableModelListener(TableModelListener tableModelListener) {

    }

    @Override
    public SearchableModel<Utilisateur> retrieveSearchModel(String searched) {
        if (StringUtils.isEmptyOrWhitespaceOnly(searched)) {
            return this;
        }

        UserModel userModel = new UserModel();

        Iterator<Utilisateur> ite = elements.iterator();
        for (; ite.hasNext(); ) {
            Utilisateur u = ite.next();
            final String strs[] = {u.getPrenom(), u.getNom(), u.getINE(), u.getType()};

            for (String s : strs) {
                int i = 0, j = 0;
                while (i < s.length() && j < searched.length()) {
                    if (s.charAt(i) == searched.charAt(j)) {
                        ++j;
                    }

                    ++i;
                }

                if (j == searched.length()) {
                    userModel.addRow(u);
                    break;
                }
            }
        }

        return userModel;
    }
}
