package ui.Client;

import backend.data.Groupe;
import backend.modele.GroupeTreeModel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.TreeSet;

public class GroupePanel extends JPanel {

    JScrollPane scrollPane;
    JTree ticketTree;
    DefaultMutableTreeNode groupeModel;

    public GroupePanel(TreeSet<Groupe> groupes) {
        super(new BorderLayout());

        initPanel(groupes);
    }

    private void initPanel(TreeSet<Groupe> groupes) {
        groupeModel = new GroupeTreeModel(groupes);
        ticketTree = new JTree(groupeModel);

        scrollPane = new JScrollPane();
        scrollPane.setViewportView(ticketTree);

        add(scrollPane);
    }

}
