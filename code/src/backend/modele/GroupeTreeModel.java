package backend.modele;

import backend.data.Groupe;
import backend.data.Ticket;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.TreeSet;

public class GroupeTreeModel extends DefaultMutableTreeNode {

    public GroupeTreeModel(TreeSet<Groupe> groupes) {
        for (Groupe groupe : groupes) {
            DefaultMutableTreeNode groupe_node = new DefaultMutableTreeNode(groupe.getLabel());

            for (Ticket ticket : groupe.getTickets()) {
                DefaultMutableTreeNode ticket_node = new DefaultMutableTreeNode(ticket.getTitre());

                groupe_node.add(ticket_node);
            }

            add(groupe_node);
        }
    }
}
