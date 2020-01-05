package ui;

import backend.data.Groupe;
import backend.data.Message;
import backend.data.Ticket;

import javax.swing.*;
import java.util.TreeSet;

public abstract class InteractiveUI extends JFrame {
    public abstract void updateRelatedGroups(TreeSet<Groupe> relatedGroups);

    public abstract void updateGroupsList(TreeSet<String> allGroups);

    public abstract void updateGroupe(Groupe entryAsGroupe);

    public abstract void updateTicket(Long entryRelatedGroup, Ticket entryAsTicket);

    public abstract void updateMessage(Long entryRelatedGroup, Long entryRelatedTicket, Message entryAsMessage);

    public abstract void deleteGroupe(Groupe entryAsGroupe);

    public abstract void deleteTicket(Long entryRelatedGroup, Ticket entryAsTicket);

    public abstract void deleteMessage(Long entryRelatedGroup, Long entryRelatedTicket, Message entryAsMessage);

    public abstract void addGroupe(Groupe entryAsGroupe);

    public abstract void addTicket(Long relatedGroupEntry, Ticket entryAsTicket);

    public abstract void addMessage(Long entryRelatedGroup, Long entryRelatedTicket, Message entryAsMessage);
}
