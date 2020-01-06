package ui.Server;

import backend.data.Groupe;
import backend.data.Message;
import backend.data.Ticket;
import backend.data.Utilisateur;
import backend.modele.GroupModel;
import backend.modele.MessageModel;
import backend.modele.TicketModel;
import backend.modele.UserModel;
import backend.server.Server;
import backend.server.client.Client;
import backend.server.communication.classic.ClassicMessage;
import debug.Debugger;
import ui.Client.ConnexionScreen;
import ui.InteractiveUI;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
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
        setTitle(SERVER_FRAME_TITLE);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setMinimumSize(new Dimension(800, 600));
        this.setLocationRelativeTo(null);

        mainPanel = this.getContentPane();
        initPanel();

        this.setVisible(true);

        try {
            client.sendData(ClassicMessage.createRequestEverything());
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.client = client;
        client.setUI(this);
        client.retrieveAllModels();
        client.start();
    }

    public static void main(String[] args) {
        System.setProperty("javax.net.ssl.keyStore", "res/keystore");
        System.setProperty("javax.net.ssl.trustStore", "res/keystore");
        System.setProperty("javax.net.ssl.keyStorePassword", "passphrase");
        System.setProperty("javax.net.ssl.trustStorePassword", "passphrase");

        Debugger.isDebugging = true;

        try {
            Client client = new Client((SSLSocket) SSLContext.getDefault().getSocketFactory().createSocket("localhost", 6666));
            SwingUtilities.invokeLater(() -> new ConnexionScreen(client, true));
        } catch (IOException | Server.ServerInitializationFailedException | NoSuchAlgorithmException e) {
            // Do something on client connection refused
            JOptionPane.showMessageDialog(null, "Connexion au serveur impossible !", "Erreur", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }


    public void setMainPanel() {
        this.setContentPane(mainPanel);
        secondPanel = null;

        this.pack();
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
        mainPanel = this.getContentPane();

        secondPanel = new EditUserPanel(this, arg);
        this.setContentPane(secondPanel);

        this.pack();
    }


    void editGroupPanel(Groupe arg) {
        mainPanel = this.getContentPane();

        secondPanel = new EditGroupPanel(this, arg);
        this.setContentPane(secondPanel);

        this.pack();
    }

    private void initPanel() {
        Container pane = this.getContentPane();
        uiPanel = new ServerUIPanel(this);
        pane.add(uiPanel, BorderLayout.CENTER);
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
        uiPanel.update();
    }

    @Override
    public void deleteUser(Utilisateur entryAsUser) {
        userModel.removeEntry(entryAsUser.getID());
        uiPanel.update();
    }

    @Override
    public void deleteGroupe(Groupe entryAsGroupe) {
        groupModel.removeEntry(entryAsGroupe.getID());
        uiPanel.update();
    }

    @Override
    public void deleteTicket(Groupe entryRelatedGroup, Ticket entryAsTicket) {
        ticketModel.removeEntry(entryAsTicket.getID());
        uiPanel.update();
    }

    @Override
    public void deleteMessage(Groupe entryRelatedGroup, Ticket entryRelatedTicket, Message entryAsMessage) {
        messageModel.removeEntry(entryAsMessage.getID());
        uiPanel.update();
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
            client.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
