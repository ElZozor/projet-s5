package ui.Client.mainscreen.leftpanel;

import backend.data.Groupe;
import backend.modele.GroupeTreeModel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
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
        ticketTree = new JTree(new TicketTreeObject(new DefaultMutableTreeNode(groupes)));
        ticketTree.setCellRenderer(new TicketTreeRenderer());

        scrollPane = new JScrollPane();
        scrollPane.setViewportView(ticketTree);

        add(scrollPane);


        ticketTree.addTreeSelectionListener(tsl -> {
            Object node = ticketTree.getLastSelectedPathComponent();

            if (node == null) {
                return;
            }

            if (listener != null) {
                listener.selected(node);
            }
        });
    }

    public void updateTree(TreeSet<Groupe> groupes) {
        ((DefaultTreeModel) ticketTree.getModel()).setRoot((DefaultMutableTreeNode) ticketTree.getModel().getRoot());
    }

    public void addTreeSelectionListener(TreeItemSelectedListener isl) {
        listener = isl;
    }

    public interface TreeItemSelectedListener {
        void selected(Object object);
    }

}
