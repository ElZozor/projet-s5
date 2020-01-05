package ui.Client.mainscreen.leftpanel;

import backend.data.Groupe;
import backend.modele.GroupeTreeModel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.TreeSet;

public class TicketTree extends JPanel {

    JScrollPane scrollPane;
    JTree ticketTree;
    DefaultMutableTreeNode groupeModel;

    TreeItemSelectedListener listener;

    public TicketTree(TreeSet<Groupe> groupes) {
        super(new BorderLayout());

        initPanel(groupes);
    }

    private void initPanel(TreeSet<Groupe> groupes) {
        groupeModel = new GroupeTreeModel(groupes);
        ticketTree = new JTree(groupeModel);

        scrollPane = new JScrollPane();
        scrollPane.setViewportView(ticketTree);

        add(scrollPane);


        ticketTree.addTreeSelectionListener(tsl -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) ticketTree.getLastSelectedPathComponent();

            if (node == null) {
                return;
            }

            Object nodeInfo = node.getUserObject();
            if (listener != null) {
                listener.selected(nodeInfo);
            }
        });
    }

    public void addTreeSelectionListener(TreeItemSelectedListener isl) {
        listener = isl;
    }

    public interface TreeItemSelectedListener {
        void selected(Object object);
    }

}
