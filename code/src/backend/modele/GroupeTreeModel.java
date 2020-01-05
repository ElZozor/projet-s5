package backend.modele;

import backend.data.Groupe;
import backend.data.Ticket;
import backend.server.communication.classic.ClassicMessage;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.TreeSet;

public class GroupeTreeModel extends DefaultMutableTreeNode {

    private TreeSet<Groupe> groups = new TreeSet<>();

    public GroupeTreeModel(TreeSet<Groupe> groupes) {
        setContents(groupes);
    }


    public GroupeTreeModel(ClassicMessage groupes) {
        if (!groupes.isLocalUpdateResponse()) {
            return;
        }

        TreeSet<Groupe> groupsSet = groupes.getLocalUpdateResponseRelatedGroups();

        setContents(groupsSet);

        // Todo update groups
    }

    private void setContents(TreeSet<Groupe> groupes) {
        for (Groupe groupe : groupes) {
            DefaultMutableTreeNode groupe_node = new DefaultMutableTreeNode(groupe);

            for (Ticket ticket : groupe.getTickets()) {
                DefaultMutableTreeNode ticket_node = new DefaultMutableTreeNode(ticket);

                groupe_node.add(ticket_node);
            }

            add(groupe_node);
        }
    }
}
