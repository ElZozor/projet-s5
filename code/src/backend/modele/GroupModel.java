package backend.modele;

import backend.data.Groupe;
import com.mysql.jdbc.StringUtils;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

public class GroupModel extends SearchableModel {

    private static final String[] columnNames = {
            "ID",
            "LABEL"
    };
    ArrayList<Groupe> groups = new ArrayList<>();


    public GroupModel(ResultSet set) {
        try {
            for (; set.next(); ) {
                groups.add(new Groupe(set));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private GroupModel() {

    }

    public void addRow(Groupe newUser) {
        groups.add(newUser);
    }

    @Override
    public int getRowCount() {
        return groups.size();
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
        Groupe u = groups.get(ligne);

        if (colonne == 0) {
            return u.getID();
        } else {
            return u.getLabel();
        }
    }

    @Override
    public void setValueAt(Object o, int ligne, int colonne) {
        Groupe g = groups.get(ligne);

        if (colonne == 0) {
            g.setID((Long) o);
        } else {
            g.setLabel((String) o);
        }
    }

    @Override
    public void addTableModelListener(TableModelListener tableModelListener) {

    }

    @Override
    public void removeTableModelListener(TableModelListener tableModelListener) {

    }

    @Override
    public TableModel retrieveSearchModel(String searched) {
        if (StringUtils.isEmptyOrWhitespaceOnly(searched)) {
            return this;
        }

        GroupModel groupModel = new GroupModel();

        Iterator<Groupe> ite = groups.iterator();
        for (; ite.hasNext(); ) {
            Groupe g = ite.next();
            final String label = g.getLabel();

            int i = 0, j = 0;
            while (i < label.length() && j < searched.length()) {
                if (label.charAt(i) == searched.charAt(j)) {
                    ++j;
                }

                ++i;
            }

            if (j == searched.length()) {
                groupModel.addRow(g);
            }
        }

        return groupModel;
    }
}
