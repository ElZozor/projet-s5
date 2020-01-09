package ui.Server;

import backend.data.Groupe;
import backend.data.Message;
import backend.data.Ticket;
import backend.data.Utilisateur;
import backend.modele.GroupModel;
import backend.modele.MessageModel;
import backend.modele.TicketModel;
import backend.modele.UserModel;
import backend.server.client.Client;
import debug.Debugger;
import ui.InteractiveUI;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.TreeSet;

public class ServerUI extends InteractiveUI {
    private static String SERVER_FRAME_TITLE = "Administration";

    private Container mainPanel;
    private Container secondPanel = null;
    private ServerUIPanel uiPanel;
    public Client client;

    private UserModel userModel;
    private GroupModel groupModel;
    private TicketModel ticketModel;
    private MessageModel messageModel;

    public ServerUI(Client client) {
        super();

        setTitle(SERVER_FRAME_TITLE);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setMinimumSize(new Dimension(800, 600));
        this.setLocationRelativeTo(null);

        mainPanel = new JPanel();
        initPanel();

        this.setVisible(true);


        this.client = client;
        client.setRequestEverything(true);
        client.setUI(this);
        client.retrieveAllModels();
        client.start();
    }


    public void setMainPanel() {
        if (secondPanel != null) {
            getContentPane().remove(secondPanel);
            getContentPane().add(mainPanel, BorderLayout.CENTER);
            secondPanel = null;

            getContentPane().revalidate();
            getContentPane().repaint();
            pack();
        }
    }


    void edit(final String table_name, Object arg) {
        switch (table_name) {
            case "Utilisateur":
                editUserPanel((Utilisateur) arg);
                break;

            case "Groupe":
                editGroupPanel((Groupe) arg);
                break;

            default:
                break;
        }
    }


    void editUserPanel(Utilisateur arg) {
        getContentPane().remove(mainPanel);
        secondPanel = new EditUserPanel(this, arg);
        getContentPane().add(secondPanel, BorderLayout.CENTER);

        this.pack();
    }


    void editGroupPanel(Groupe arg) {
        getContentPane().remove(mainPanel);
        secondPanel = new EditGroupPanel(this, arg);
        getContentPane().add(secondPanel, BorderLayout.CENTER);

        this.pack();
    }

    private void initPanel() {
        uiPanel = new ServerUIPanel(this);
        getContentPane().add(uiPanel, BorderLayout.CENTER);
        mainPanel = uiPanel;
    }


    public void setAllModels(UserModel userModel, GroupModel groupModel, TicketModel ticketModel, MessageModel messageModel) {
        this.userModel = userModel;
        this.groupModel = groupModel;
        this.ticketModel = ticketModel;
        this.messageModel = messageModel;


        Debugger.logMessage("ServerUI", "Sending all of theses models to the panel");
        uiPanel.updateModels(userModel, groupModel, ticketModel, messageModel);
    }

    @Override
    public void updateRelatedGroups(TreeSet<Groupe> relatedGroups) {

    }

    @Override
    public void updateGroupsList(TreeSet<String> allGroups) {

    }

    @Override
    public void updateGroupe(Groupe entryAsGroupe) {
        groupModel.updateEntry(entryAsGroupe);
    }

    @Override
    public void updateTicket(Groupe entryRelatedGroup, Ticket entryAsTicket) {
        ticketModel.updateEntry(entryAsTicket);
    }

    @Override
    public void updateMessage(Groupe entryRelatedGroup, Ticket entryRelatedTicket, Message entryAsMessage) {
        messageModel.updateEntry(entryAsMessage);
    }

    public void updateUser(Utilisateur user) {
        userModel.updateEntry(user);
    }

    @Override
    public void deleteUser(Utilisateur entryAsUser) {
        userModel.removeEntry(entryAsUser.getID());
    }

    @Override
    public void deleteGroup(Groupe entryAsGroupe) {
        groupModel.removeEntry(entryAsGroupe.getID());
    }

    @Override
    public void deleteTicket(Groupe entryRelatedGroup, Ticket entryAsTicket) {
        ticketModel.removeEntry(entryAsTicket.getID());
    }

    @Override
    public void deleteTicket(Ticket entryAsTicket) {
        ticketModel.removeEntry(entryAsTicket.getID());
    }

    @Override
    public void deleteMessage(Groupe entryRelatedGroup, Ticket entryRelatedTicket, Message entryAsMessage) {
        messageModel.removeEntry(entryAsMessage.getID());
    }

    public void addUser(Utilisateur user) {
        userModel.addRow(user);
    }

    @Override
    public void addGroupe(Groupe entryAsGroupe) {
        groupModel.addRow(entryAsGroupe);
    }

    @Override
    public void addTicket(Groupe relatedGroupEntry, Ticket entryAsTicket) {
        ticketModel.addRow(entryAsTicket);
        for (Message message : entryAsTicket.getMessages()) {
            addMessage(relatedGroupEntry, entryAsTicket, message);
        }
    }

    @Override
    public void addMessage(Groupe entryRelatedGroup, Ticket entryRelatedTicket, Message entryAsMessage) {
        messageModel.addRow(entryAsMessage);
    }

    @Override
    public void dispose() {
        super.dispose();

        try {
            client.disconnect(null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
