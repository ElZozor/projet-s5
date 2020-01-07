package backend.server.client;


import backend.data.Groupe;
import backend.data.Message;
import backend.data.Ticket;
import backend.data.Utilisateur;
import backend.modele.UserModel;
import backend.server.Server;
import backend.server.communication.CommunicationMessage;
import backend.server.communication.classic.ClassicMessage;
import debug.Debugger;
import ui.InteractiveUI;
import ui.Server.ServerUI;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.swing.*;
import java.io.*;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Stack;
import java.util.TreeSet;

import static backend.database.Keys.*;

public class Client extends Thread implements Server {

    private final static String DBG_COLOR = Debugger.YELLOW;
    private final static int SOCKET_TIMEOUT = 5000;

    private SSLSocket mSocket;

    private BufferedWriter mWriteStream;
    private BufferedReader mReadStream;

    private InteractiveUI ui;
    private Boolean running = false;
    private Boolean connected = true;
    private Boolean requestEverything = false;

    private Utilisateur myUser;

    private Stack<CommunicationMessage> pendingMessages = new Stack<>();

    /**
     * This class is used on the client side.
     * It's used to communicate with the host.
     *
     * @param socket The connexion socket
     * @throws ServerInitializationFailedException When the server can't be init
     */
    public Client(SSLSocket socket) throws ServerInitializationFailedException {
        try {
            mSocket = socket;

            mSocket.setSoTimeout(SOCKET_TIMEOUT);

            mWriteStream = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
            mReadStream = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));


        } catch (IOException e) {
            e.printStackTrace();
            throw new ServerInitializationFailedException("Something went wrong while initializing connexion");
        }
    }

    public void setUI(InteractiveUI ui) {
        this.ui = ui;
    }


    /**
     * A synchronized function that send a message and wait for the return value.
     *
     * @param classicMessage The message you want to send
     * @return The data send by the host
     * @throws IOException Can be thrown while writing/reading into the fd
     */
    public ClassicMessage sendAndWaitForReturn(ClassicMessage classicMessage) throws IOException {
        sendData(classicMessage);

        try {
            return readData();
        } catch (IOException | ClassicMessage.InvalidMessageException e) {
            e.printStackTrace();

            return null;
        } catch (SocketDisconnectedException e) {
            return null;
        }
    }


    /**
     * Function used to send a connection message to the host.
     * Will return the message received from the host.
     *
     * @param INE      The user INE
     * @param password The user password
     * @return
     */
    public ClassicMessage sendConnectionMessage(String INE, String password) {

        ClassicMessage returnedData = null;

        try {
            returnedData = sendAndWaitForReturn(
                    ClassicMessage.createConnection(INE, password)
            );

            if (returnedData != null && returnedData.isAck()) {
                myUser = new Utilisateur(0L, "", "", INE, "");
                myUser.setPassword(password);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return returnedData;

    }


    /**
     * This function is used when the user want end the communication.
     * Will return the message received from the host.
     *
     * @throws IOException Can be thrown while closing the socket.
     */
    public void disconnect() throws IOException {

        mWriteStream.close();
        mReadStream.close();
        mSocket.close();
        running = false;

    }

    @Override
    /**
     * methode bouclant à l'infini tant qu'il n'y a pas de fermeture de l'application,
     * cette methode attend la reception d'un message et le transmet à handleMessage pour le traiter.
     * En cas de perte de connexion il y'a tentative de reconnection jusqu'a reussite ou fermeture de l'application
    **/
    public void run() {

        try {
            mSocket.setSoTimeout(0);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        running = true;

        while (running) {

            try {
                ClassicMessage message = readData();
                if (message == null || ui == null) {
                    continue;
                }

                handleMessage(message);

            } catch (SocketDisconnectedException e) {
                reconnect();
            } catch (IOException | ClassicMessage.InvalidMessageException e) {
                e.printStackTrace();
            }

        }

    }


    private void reconnect() {
        connected = false;
        System.out.println("Entering socket reconnection");
        while (running && !connected) {
            try {
                mSocket = (SSLSocket) SSLContext.getDefault().getSocketFactory().createSocket("localhost", 6666);
                mSocket.setSoTimeout(SOCKET_TIMEOUT);

                mWriteStream = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
                mReadStream = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

                ClassicMessage message = sendConnectionMessage(myUser.getINE(), myUser.getPassword());
                connected = (message != null && message.isAck());

                mSocket.setSoTimeout(0);
            } catch (IOException | NoSuchAlgorithmException ex) {
                try {
                    sleep(1000);
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }
        }

        if (connected) {
            setRequestEverything(requestEverything);
            sendPendingMessages();
        }
    }

    private void sendPendingMessages() {
        CommunicationMessage[] pending = new CommunicationMessage[pendingMessages.size()];
        for (int i = 0; i < pending.length; ++i) {
            pending[i] = pendingMessages.pop();
        }

        for (CommunicationMessage message : pending) {
            sendData(message);
        }
    }


    /**
     * Methode recevant un message et le redirigeant vers les fonction de traitement adaptées au type du message
     *
     * @param message - objet ClassiqueMessage étant le message reçu
    **/
    private void handleMessage(ClassicMessage message) {

        switch (message.getType()) {
            case LOCAL_UPDATE_RESPONSE:
                handleLocalUpdate(message);
                break;

            case ENTRY_ADDED:
                handleEntryAdded(message);
                break;

            case ENTRY_DELETED:
                handleEntryDeleted(message);
                break;

            case ENTRY_UPDATED:
                handleEntryUpdated(message);
                break;

            case TABLE_MODEL:
                handleTableModelMessage(message);
                break;
        }

    }

    private void handleTableModelMessage(ClassicMessage message) {

        if (ui instanceof ServerUI) {
            Debugger.logMessage("Client", "Table model received, sending to the ui");
            ServerUI serverUI = (ServerUI) ui;

            final UserModel userModel = message.getTableModelUserModel();
            final String password = myUser.getPassword();
            myUser = userModel.getReferenceTo(myUser.getINE());
            myUser.setPassword(password);

            serverUI.setAllModels(
                    message.getTableModelUserModel(),
                    message.getTableModelGroupModel(),
                    message.getTableModelTicketModel(),
                    message.getTableModelMessageModel()
            );
        }

    }


    private void handleLocalUpdate(ClassicMessage message) {
        TreeSet<Groupe> relatedGroups = message.getLocalUpdateResponseRelatedGroups();
        TreeSet<String> allGroups = message.getLocalUpdateResponseAllGroups();
        TreeSet<Utilisateur> users = message.getLocalUpdateResponseUsers();

        for (Utilisateur user : users) {
            if (user.getINE().equals(myUser.getINE())) {
                final String password = myUser.getPassword();
                myUser = user;
                myUser.setPassword(password);
                break;
            }
        }

        Utilisateur.setInstances(users);

        ui.updateRelatedGroups(relatedGroups);
        ui.updateGroupsList(allGroups);

        ArrayList<Message> received = new ArrayList<>();
        for (Groupe groupe : relatedGroups) {
            for (Ticket ticket : groupe.getTickets()) {
                for (Message msg : ticket.getMessages()) {
                    if (msg.state() < 3) {
                        received.add(msg);
                    }
                }
            }
        }

        sendData(ClassicMessage.createMessageReceived(received));
    }


    private void handleEntryAdded(ClassicMessage message) {
        ArrayList<Message> received = new ArrayList<>();

        switch (message.getTable()) {
            case TABLE_NAME_UTILISATEUR:
                Utilisateur user = message.getEntryAsUtilisateur();
                Utilisateur.addInstance(user);

                if (ui instanceof ServerUI) {
                    ((ServerUI) ui).addUser(user);
                }

                break;

            case TABLE_NAME_GROUPE:
                ui.addGroupe(message.getEntryAsGroupe());
                break;

            case TABLE_NAME_TICKET:
                ui.addTicket(message.getEntryRelatedGroup(), message.getEntryAsTicket());
                TreeSet<Message> messages = message.getEntryAsTicket().getMessages();
                if (messages != null) {
                    received.addAll(messages);
                }

                break;

            case TABLE_NAME_MESSAGE:
                ui.addMessage(message.getEntryRelatedGroup(), message.getEntryRelatedTicket(), message.getEntryAsMessage());
                received.add(message.getEntryAsMessage());
                break;
        }


        if (!received.isEmpty()) {
            sendData(ClassicMessage.createMessageReceived(received));
        }
    }


    private void handleEntryDeleted(ClassicMessage message) {
        switch (message.getTable()) {
            case TABLE_NAME_UTILISATEUR:
                Utilisateur user = message.getEntryAsUtilisateur();
                Utilisateur.removeInstance(user.getID());

                if (user.equals(myUser)) {
                    JOptionPane.showMessageDialog(ui,
                            "Votre utilisateur a été supprimé !",
                            "Déconnexion",
                            JOptionPane.ERROR_MESSAGE
                    );
                    ui.dispose();

                } else {
                    ui.deleteUser(user);
                }
                break;

            case TABLE_NAME_GROUPE:
                ui.deleteGroupe(message.getEntryAsGroupe());
                break;

            case TABLE_NAME_TICKET:
                ui.deleteTicket(message.getEntryRelatedGroup(), message.getEntryAsTicket());
                break;

            case TABLE_NAME_MESSAGE:
                ui.deleteMessage(message.getEntryRelatedGroup(), message.getEntryRelatedTicket(), message.getEntryAsMessage());
                break;
        }
    }


    private void handleEntryUpdated(ClassicMessage message) {
        ArrayList<Message> received = new ArrayList<>();

        switch (message.getTable()) {
            case TABLE_NAME_UTILISATEUR:
                Utilisateur user = message.getEntryAsUtilisateur();
                Utilisateur.updateInstance(user);
                if (user.getID().equals(myUser.getID())) {
                    myUser = user;
                }

                if (ui instanceof ServerUI) {
                    ((ServerUI) ui).updateUser(user);
                }

                break;

            case TABLE_NAME_GROUPE:
                ui.updateGroupe(message.getEntryAsGroupe());
                break;

            case TABLE_NAME_TICKET:
                Ticket ticket = message.getEntryAsTicket();
                ui.updateTicket(message.getEntryRelatedGroup(), ticket);

                TreeSet<Message> messages = ticket.getMessages();
                if (messages != null) {
                    received.addAll(messages);
                }
                break;

            case TABLE_NAME_MESSAGE:
                ui.updateMessage(message.getEntryRelatedGroup(), message.getEntryRelatedTicket(), message.getEntryAsMessage());
                received.add(message.getEntryAsMessage());
                break;
        }

        if (!received.isEmpty()) {
            sendData(ClassicMessage.createMessageReceived(received));
        }
    }


    /**
     * This function is used to create a new ticket.
     * Will return the message received from the host.
     *
     * @param title          The ticket title
     * @param messageContent The message contents
     * @param group          The concerned group
     * @return The message received from the host
     */
    public Boolean createANewTicket(String title, String messageContent, String group) {


        try {
            sendData(ClassicMessage.createTicket(title, group, messageContent));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }


    /**
     * Used to post a message into a ticket.
     * Will return the data retrieved by the host.
     *
     * @param ticketid The ticket id.
     * @param contents The contents;
     * @return The data retrieved by the host.
     */
    public Boolean postAMessage(Long ticketid, String contents) {
        return sendData(ClassicMessage.createMessage(ticketid, contents));
    }


    /**
     * This function is used to update the local database
     * from the host database.
     * It will send an "udpateMessage" to the host with
     * the last update date and the host will retrieve
     * all the messages / tickets that are newer than the
     * given date.
     *
     * @return The data retrieved by the server.
     */
    public Boolean updateLocalDatabase() {
        return sendData(ClassicMessage.createLocalUpdate(new Date(0)));
    }

    public Boolean sendNotificationTicketClicked(Ticket ticket) {
        return sendData(ClassicMessage.createTicketClicked(ticket));
    }


    public Boolean retrieveAllModels() {
        return sendData(ClassicMessage.createTableModelRequest());
    }

    @Override
    public BufferedWriter getSocketWriter() {
        return mWriteStream;
    }

    @Override
    public BufferedReader getSocketReader() {
        return mReadStream;
    }

    @Override
    public void addPendingMessage(CommunicationMessage message) {
        pendingMessages.push(message);
    }

    public void setRequestEverything(boolean b) {
        requestEverything = b;

        if (requestEverything) {
            sendData(ClassicMessage.createRequestEverything());
        }
    }
}