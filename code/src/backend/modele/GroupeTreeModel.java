package backend.modele;

import backend.data.Groupe;
import backend.data.Ticket;
import backend.server.communication.CommunicationMessage;
import org.json.JSONArray;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.TreeSet;

public class GroupeTreeModel extends DefaultMutableTreeNode {

    private TreeSet<Groupe> groups = new TreeSet<>();

    public GroupeTreeModel(TreeSet<Groupe> groupes) {
        setContents(groupes);
    }


    public GroupeTreeModel(CommunicationMessage groupes) {
        if (!groupes.isLocalUpdateResponse()) {
            return;
        }

        TreeSet<Groupe> groupsSet = new TreeSet<>();

        JSONArray array = groupes.getLocalUpdateResponseGroups();
        for (int i = 0; i < array.length(); ++i) {
            groupsSet.add(new Groupe(array.getJSONObject(i)));
        }

        setContents(groupsSet);

        // Todo update groups
    }

    private void setContents(TreeSet<Groupe> groupes) {
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
