package ui.Client.mainscreen.leftpanel;

import backend.data.Groupe;
import backend.data.Ticket;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class TicketTreeRenderer extends DefaultTreeCellRenderer {

    private final ImageIcon baseIcon;
    private final ImageIcon groupIcon;
    private final ImageIcon ticketIcon;
    private final ImageIcon ticketSeenIcon;

    public TicketTreeRenderer() {
        baseIcon = new ImageIcon("res/base.png");
        groupIcon = new ImageIcon("res/group.png");
        ticketIcon = new ImageIcon("res/ticket.png");
        ticketSeenIcon = new ImageIcon("res/ticket_seen.png");
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);


        Object o = value;
        if (value instanceof DefaultMutableTreeNode) {
            o = ((DefaultMutableTreeNode) value).getUserObject();
        }

        String text = "";
        ImageIcon icon = baseIcon;
        if (o instanceof Ticket) {
            Ticket ticket = (Ticket) o;

            int remainingMsg = ticket.getNotSeenMessages();
            text = ticket.getTitre();
            if (remainingMsg > 0) {
                text = String.format("<b>%s (%d)</b>", text, remainingMsg);
                icon = ticketIcon;
            } else {
                icon = ticketSeenIcon;
            }

        } else if (o instanceof Groupe) {
            Groupe groupe = (Groupe) o;
            text = groupe.getLabel();

            icon = groupIcon;
        }

        setIcon(icon);
        setText("<html>" + text + "</html>");


        return this;
    }
}
