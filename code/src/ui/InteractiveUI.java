package ui;

import backend.data.Groupe;
import backend.data.Message;
import backend.data.Ticket;
import backend.data.Utilisateur;

import javax.swing.*;
import java.awt.*;
import java.util.TreeSet;

public abstract class InteractiveUI extends JFrame {

    private final static String CONNECTED_STATUS = "Status : Connecté";
    private final static String RECONNECTION_STATUS = "Status : Reconnection..";

    private JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    private JLabel statusLabel = new JLabel();
    private JPanel panel = new JPanel(new BorderLayout());

    public InteractiveUI() {
        super();

        statusPanel.add(statusLabel);
        panel.add(statusPanel, BorderLayout.SOUTH);

        setContentPane(panel);
    }

    public void setConnectionStatus(boolean connected) {
        statusLabel.setText(connected ? CONNECTED_STATUS : RECONNECTION_STATUS);
        statusLabel.revalidate();
        statusLabel.repaint();
    }

    public abstract void updateRelatedGroups(TreeSet<Groupe> relatedGroups);

    public abstract void updateGroupsList(TreeSet<String> allGroups);

    public abstract void updateGroupe(Groupe entryAsGroupe);

    public abstract void updateTicket(Groupe entryRelatedGroup, Ticket entryAsTicket);

    public abstract void updateMessage(Groupe entryRelatedGroup, Ticket entryRelatedTicket, Message entryAsMessage);

    public abstract void deleteUser(Utilisateur entryAsUser);

    public abstract void deleteGroup(Groupe entryAsGroupe);

    public abstract void deleteTicket(Groupe entryRelatedGroup, Ticket entryAsTicket);

    public abstract void deleteTicket(Ticket entryAsTicket);

    public abstract void deleteMessage(Groupe entryRelatedGroup, Ticket entryRelatedTicket, Message entryAsMessage);

    public abstract void addGroupe(Groupe entryAsGroupe);

    public abstract void addTicket(Groupe relatedGroupEntry, Ticket entryAsTicket);

    public abstract void addMessage(Groupe entryRelatedGroup, Ticket entryRelatedTicket, Message entryAsMessage);
}
