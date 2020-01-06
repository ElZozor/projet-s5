package ui;

import backend.data.Groupe;
import backend.data.Message;
import backend.data.Ticket;
import backend.data.Utilisateur;

import javax.swing.*;
import java.util.TreeSet;

public abstract class InteractiveUI extends JFrame {
    public abstract void updateRelatedGroups(TreeSet<Groupe> relatedGroups);

    public abstract void updateGroupsList(TreeSet<String> allGroups);

    public abstract void updateGroupe(Groupe entryAsGroupe);

    public abstract void updateTicket(Groupe entryRelatedGroup, Ticket entryAsTicket);

    public abstract void updateMessage(Groupe entryRelatedGroup, Ticket entryRelatedTicket, Message entryAsMessage);

    public abstract void deleteUser(Utilisateur entryAsUser);

    public abstract void deleteGroupe(Groupe entryAsGroupe);

    public abstract void deleteTicket(Groupe entryRelatedGroup, Ticket entryAsTicket);

    public abstract void deleteMessage(Groupe entryRelatedGroup, Ticket entryRelatedTicket, Message entryAsMessage);

    public abstract void addGroupe(Groupe entryAsGroupe);

    public abstract void addTicket(Groupe relatedGroupEntry, Ticket entryAsTicket);

    public abstract void addMessage(Groupe entryRelatedGroup, Ticket entryRelatedTicket, Message entryAsMessage);
}
