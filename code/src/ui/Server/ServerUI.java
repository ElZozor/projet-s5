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
        pane.add(new ServerUIPanel(this), BorderLayout.CENTER);
    }

    @Override
    public void updateRelatedGroups(TreeSet<Groupe> relatedGroups) {

    }

    @Override
    public void updateGroupsList(TreeSet<String> allGroups) {

    }

    @Override
    public void updateGroupe(Groupe entryAsGroupe) {

    }

    @Override
    public void updateTicket(Long entryRelatedGroup, Ticket entryAsTicket) {

    }

    @Override
    public void updateMessage(Long entryRelatedGroup, Long entryRelatedTicket, Message entryAsMessage) {

    }

    @Override
    public void deleteGroupe(Groupe entryAsGroupe) {

    }

    @Override
    public void deleteTicket(Long entryRelatedGroup, Ticket entryAsTicket) {

    }

    @Override
    public void deleteMessage(Long entryRelatedGroup, Long entryRelatedTicket, Message entryAsMessage) {

    }

    @Override
    public void addGroupe(Groupe entryAsGroupe) {

    }

    @Override
    public void addTicket(Long relatedGroupEntry, Ticket entryAsTicket) {

    }

    @Override
    public void addMessage(Long entryRelatedGroup, Long entryRelatedTicket, Message entryAsMessage) {

    }

    public void setAllModels(UserModel userModel, GroupModel groupModel, TicketModel ticketModel, MessageModel messageModel) {
        this.userModel = userModel;
        this.groupModel = groupModel;
        this.ticketModel = ticketModel;
        this.messageModel = messageModel;


        Debugger.logMessage("ServerUI", "Sending all of theses models to the panel");
        ServerUIPanel.updateModels(userModel, groupModel, ticketModel, messageModel);
    }
}
