package ui.Client.mainscreen.leftpanel;

import backend.data.Groupe;
import backend.data.Ticket;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.TreeSet;

public class TicketTreeObject extends DefaultTreeModel {

    public TicketTreeObject(TreeNode root) {
        super(root);
    }

    @Override
    public Object getChild(Object parent, int index) {
        Object o = parent;
        if (parent instanceof DefaultMutableTreeNode) {
            o = ((DefaultMutableTreeNode) parent).getUserObject();
        }

        if (o instanceof TreeSet) {
            return ((TreeSet) o).toArray()[index];
        }

        if (o instanceof Groupe) {
            Groupe groupe = (Groupe) o;
            return groupe.getTickets().toArray()[index];
        }

        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        Object o = parent;
        if (parent instanceof DefaultMutableTreeNode) {
            o = ((DefaultMutableTreeNode) parent).getUserObject();
        }

        if (o instanceof TreeSet) {
            return ((TreeSet<Groupe>) o).size();
        }

        if (o instanceof Groupe) {
            Groupe groupe = (Groupe) parent;
            return groupe.getTickets().size();
        }

        return 0;
    }

    @Override
    public boolean isLeaf(Object node) {
        Object o = node;
        if (node instanceof DefaultMutableTreeNode) {
            o = ((DefaultMutableTreeNode) node).getUserObject();
        }
        return (o instanceof Ticket) || getChildCount(o) == 0;
    }

    @Override
    public int getIndexOfChild(Object o, Object o1) {
        int index = 0;
        if (o instanceof TreeSet) {
            index = ((TreeSet<Groupe>) o).headSet((Groupe) o1).size();
        } else if (o instanceof Groupe) {
            Groupe groupe = (Groupe) o;
            index = groupe.getTickets().headSet((Ticket) o1).size();
        }

        return index;
    }
}