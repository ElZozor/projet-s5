package backend.server.host;

import backend.data.*;
import backend.database.DatabaseManager;
import backend.server.Server;
import backend.server.communication.CommunicationMessage;
import debug.Debugger;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import static backend.database.Keys.*;

public class ClientManager extends Thread implements Server {

    private final static String DBG_COLOR = Debugger.YELLOW;

    private final SSLSocket mSocket;
    private BufferedWriter mWriteStream;
    private BufferedReader mReadStream;

    private Utilisateur user;


    public ClientManager(final SSLSocket socket) throws ServerInitializationFailedException {

        try {

            mSocket = socket;
            mWriteStream = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
            mReadStream = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

            Debugger.logColorMessage(DBG_COLOR, "ClientManager", "New client manager created");
        } catch (IOException e) {
            e.printStackTrace();

            throw new ServerInitializationFailedException("Cannot initialize client connection");
        }


    }

    /**
     * @return - Si le client a les droits administrateurs ou non
     */
    private boolean isAdminOrStaff() {
        if (user == null) {
            return false;
        }

        return user.getType().equals("admin") || user.getType().equals("staff");
    }

    /**
     * Boucle principale du client manager
     * Tourne en fond et traite les messages reçus du client.
     */
    @Override
    public void run() {
        super.run();

        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "client manager is listening");

        boolean running = true;
        while (Host.isRunning && running) {
            System.out.println("trying to read");
            try {
                CommunicationMessage communicationMessage = readData();
                if (communicationMessage == null) {
                    continue;
                }

                handleMessage(communicationMessage);

            } catch (IOException | CommunicationMessage.InvalidMessageException e) {
                e.printStackTrace();
            } catch (SocketDisconnectedException e) {
                running = false;
            }

        }

        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Client is now disconnected, cleaning things..");

        if (user != null) {
            final String groups;
            try {
                groups = DatabaseManager.getInstance().relatedUserGroup(user.getINE());
                Host.removeClient(new ArrayList<>(Arrays.asList(groups.split(";"))), user, this);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        interrupt();
    }


    /**
     * Traite un message envoyé par le client et le dispache
     * dans les différentes fonctions appropriés
     *
     * @param communicationMessage Le message à traiter
     */
    private void handleMessage(CommunicationMessage communicationMessage) {

        switch (communicationMessage.getType()) {

            case CONNECTION:
                handleConnection(communicationMessage);
                break;

            case TICKET:
                handleTicketCreation(communicationMessage);
                break;

            case MESSAGE:
                handleClassicMessage(communicationMessage);
                break;

            case LOCAL_UPDATE:
                handleLocalUpdateMessage();
                break;

            case TICKET_CLICKED:
                handleTicketClickedMessage(communicationMessage);
                break;

            case DELETE:
                handleDeleteMessage(communicationMessage);
                break;

            case UPDATE:
                handleUpdateMessage(communicationMessage);
                break;

            case ADD:
                handleAddMessage(communicationMessage);
                break;

            case TABLE_MODEL_REQUEST:
                handleTableModelRequestMessage();
                break;

            case REQUEST_EVERYTHING:
                handleRequestEverythingMessage();
                break;

            case MESSAGE_RECEIVED:
                handleMessageReceivedMessage(communicationMessage);
                break;
        }

    }


    /**
     * Fonction qui traite une connexion.
     *
     * @param communicationMessage Le message de connexion
     */
    private void handleConnection(CommunicationMessage communicationMessage) {

        boolean queryResult = false;
        String fail_reason = "";


        try {

            if (communicationMessage.isConnection()) {
                DatabaseManager database = DatabaseManager.getInstance();
                ResultSet set = database.credentialsAreValid(communicationMessage.getConnectionINE(), communicationMessage.getConnectionPassword());
                queryResult = set.next();

                if (queryResult) {
                    user = new Utilisateur(set);

                    String groups = database.relatedUserGroup(user.getINE());
                    Debugger.logColorMessage(DBG_COLOR, "Client Manager", "Affiliated groupe for " + user.getINE() + ": " + groups);
                    Host.addClient(new ArrayList<>(Arrays.asList(groups.split(";"))), user, this);
                } else {
                    fail_reason = "Erreur nom utilisateur / mot de passe";
                }

            } else {

                fail_reason = ERROR_MESSAGE_EMPTY_FIELD;

            }

        } catch (SQLException e) {
            e.printStackTrace();
            fail_reason = ERROR_MESSAGE_DATABASE_ERROR;
        }


        if (queryResult) {
            sendData(CommunicationMessage.createAck());
        } else {
            sendData(CommunicationMessage.createNack(fail_reason));
        }

    }


    /**
     * Fonction qui traite la création d'un ticket
     *
     * @param communicationMessage Le message qui contient le ticket
     */
    private synchronized void handleTicketCreation(CommunicationMessage communicationMessage) {

        try {

            if (communicationMessage.isTicket()) {

                final String title = communicationMessage.getTicketTitle();
                final String contents = communicationMessage.getTicketMessage();
                final String group = communicationMessage.getTicketGroup();

                DatabaseManager databaseManager = DatabaseManager.getInstance();
                Ticket inserted = databaseManager.createNewTicket(user.getID(), title, contents, group);

                Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Inserted is " + (inserted == null ? "null" : "not null"));
                if (inserted != null) {
                    Groupe relatedGroup = databaseManager.relatedTicketGroup(inserted.getID());
                    Debugger.logColorMessage(DBG_COLOR, "ClientManager",
                            "Host must send : \n" + inserted.toJSON() + "\nto : " + relatedGroup.getLabel());

                    CommunicationMessage message = CommunicationMessage.createTicketAddedMessage(
                            TABLE_NAME_TICKET,
                            inserted,
                            relatedGroup
                    );

                    Host.broadcastToGroup(
                            message,
                            relatedGroup.getLabel()
                    );

                    Host.sendToClient(user, message);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    /**
     * Fonction qui traite le post d'un message.
     *
     * @param communicationMessage Le message à ajouter.
     */
    private synchronized void handleClassicMessage(CommunicationMessage communicationMessage) {
        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Classic message: \n" + communicationMessage.toString());

        try {

            if (communicationMessage.isMessage()) {

                Long ticketid = communicationMessage.getMessageTicketID();
                String contents = communicationMessage.getMessageContents();

                DatabaseManager database = DatabaseManager.getInstance();
                Message insertedMessage = database.insertNewMessage(contents, ticketid, user.getID());

                Debugger.logColorMessage(DBG_COLOR, "ClientManager",
                        "InsertedMessage is " + (insertedMessage == null ? "null" : "not null"));

                if (insertedMessage != null) {
                    Debugger.logColorMessage(DBG_COLOR, "ClientManager", insertedMessage.toJSON().toString());

                    Groupe group = database.relatedTicketGroup(insertedMessage.getTicketID());
                    Ticket ticket = database.getTicket(insertedMessage.getTicketID());
                    Long userID = database.ticketCreator(insertedMessage.getTicketID());
                    if (group != null) {
                        CommunicationMessage message = CommunicationMessage.createMessageAddedMessage(
                                TABLE_NAME_MESSAGE,
                                insertedMessage,
                                group,
                                ticket
                        );

                        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Broadcasting to group : " + group);
                        Host.broadcastToGroup(message, group.getLabel());
                        Host.sendToClient(user, message);
                    }

                }

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    /**
     * Fonction qui traite une demande de maj locale.
     */
    private void handleLocalUpdateMessage() {

        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Handling a local update message");

        try {
            TreeSet<Groupe> relatedGroups = DatabaseManager.getInstance().treatLocalUpdateMessage(user);
            TreeSet<String> allGroups = DatabaseManager.getInstance().getAllGroups();
            TreeSet<Utilisateur> users = DatabaseManager.getInstance().getAllUsers();

            sendData(CommunicationMessage.createLocalUpdateResponse(relatedGroups, allGroups, users));
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Fonction qui traite un click sur un ticket
     *
     * @param communicationMessage Le ticket
     */
    private synchronized void handleTicketClickedMessage(CommunicationMessage communicationMessage) {

        try {

            DatabaseManager manager = DatabaseManager.getInstance();
            if (manager.getTicket(communicationMessage.getTicketClickedID()) != null) {
                int editted = manager.setMessagesFromTicketRead(communicationMessage.getTicketClickedID(), user.getID());

                if (editted > 0) {
                    Ticket ticket = manager.getTicket(communicationMessage.getTicketClickedID());
                    if (ticket != null) {
                        Groupe groupe = manager.relatedTicketGroup(ticket.getID());
                        if (groupe != null) {
                            CommunicationMessage message = CommunicationMessage.createTicketUpdatedMessage(TABLE_NAME_TICKET, ticket, groupe);
                            Host.broadcastToGroup(message, groupe.getLabel());

                            Long ticketCreator = manager.ticketCreator(ticket.getID());
                            Host.sendToClient(ticketCreator, message);
                        }
                    }
                }
            } else {
                sendData(
                        CommunicationMessage.createTicketDeletedMessage(
                                TABLE_NAME_TICKET,
                                communicationMessage.getEntryAsTicket()
                        )
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Fonction qui traite la suppression d'une entrée ( uniquement admin )
     *
     * @param communicationMessage L'entrée à supprimer et sa table
     */
    private synchronized void handleDeleteMessage(CommunicationMessage communicationMessage) {

        if (!isAdminOrStaff()) {
            return;
        }

        ProjectTable entry = communicationMessage.getEntry();
        boolean success = true;
        Groupe relatedGroup = null;
        CommunicationMessage message = null;

        try {
            DatabaseManager database = DatabaseManager.getInstance();

            switch (communicationMessage.getTable()) {
                case TABLE_NAME_UTILISATEUR:
                    database.deleteUser(entry.getID());
                    message = CommunicationMessage.createEntryDeletedMessage(TABLE_NAME_UTILISATEUR, communicationMessage.getEntry());
                    break;


                case TABLE_NAME_GROUPE:
                    Groupe groupe = database.getGroup(communicationMessage.getEntryAsGroupe().getID());
                    if (groupe != null) {
                        database.deleteGroup(entry.getID());
                        message = CommunicationMessage.createEntryDeletedMessage(TABLE_NAME_GROUPE, groupe);
                    }

                    break;


                case TABLE_NAME_TICKET: {
                    Ticket ticket = database.getTicket(entry.getID());
                    if (ticket != null) {
                        relatedGroup = database.relatedTicketGroup(ticket.getID());
                        database.deleteTicket(ticket.getID());
                        message = CommunicationMessage.createTicketDeletedMessage(
                                communicationMessage.getTable(), ticket
                        );
                    }
                }

                break;


                case TABLE_NAME_MESSAGE:
                    Message msg = database.getMessage(entry.getID());
                    Ticket ticket = database.relatedMessageTicket(msg.getID());
                    if (ticket != null) {
                        relatedGroup = database.relatedTicketGroup(ticket.getID());
                        if (relatedGroup != null) {
                            if (database.deleteMessage(entry.getID())) {
                                message = CommunicationMessage.createMessageDeletedMessage(
                                        communicationMessage.getTable(), msg,
                                        relatedGroup, ticket
                                );
                            } else {
                                Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Deletion failed");
                                success = false;
                            }
                        } else {
                            Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Group is null");
                            success = false;
                        }
                    } else {
                        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Ticket is null");
                        success = false;
                    }

                    break;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }

        Debugger.logColorMessage(DBG_COLOR, "ClientManager",
                String.format("Deletion, success %s relatedGroup %s", success, relatedGroup));

        if (success) {
            Host.broadcast(message);
        }
    }


    /**
     * Fonction qui traite la mise à jour d'une entrée ( admin uniquement )
     *
     * @param communicationMessage L'entrée et sa table
     */
    private synchronized void handleUpdateMessage(CommunicationMessage communicationMessage) {

        if (!isAdminOrStaff()) {
            return;
        }

        ProjectTable entry = communicationMessage.getEntry();
        boolean success = true;

        try {
            switch (communicationMessage.getTable()) {
                case TABLE_NAME_UTILISATEUR: {
                    Utilisateur user = (Utilisateur) entry;
                    final String INE = user.getINE();
                    final String nom = user.getNom();
                    final String prenom = user.getPrenom();
                    final String type = user.getType();
                    final String[] groups = user.getGroups();
                    final String password = user.getPassword();


                    success = DatabaseManager.getInstance().editExistingUser(
                            entry.getID(),
                            INE,
                            nom,
                            prenom,
                            type,
                            String.join(";", groups),
                            password
                    );

                    break;
                }


                case TABLE_NAME_GROUPE:
                    Groupe groupe = (Groupe) entry;
                    String relatedGroup = DatabaseManager.getInstance().retrieveGroupForGivenID(entry.getID()).getLabel();
                    success = DatabaseManager.getInstance().editExistingGroup(
                            groupe.getID(),
                            groupe.getLabel()
                    );

                    if (success) {
                        Host.changeGroupName(relatedGroup, groupe.getLabel());
                    }

                    break;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }


        if (success) {
            CommunicationMessage message = CommunicationMessage.createEntryUpdatedMessage(communicationMessage.getTable(), entry);
            Host.broadcast(message);
        }

    }


    /**
     * Fonction qui traite l'ajout d'une entrée ( admin uniquement )
     *
     * @param communicationMessage L'entrée et sa table
     */
    private synchronized void handleAddMessage(CommunicationMessage communicationMessage) {

        if (!isAdminOrStaff()) {
            return;
        }

        ProjectTable entry = communicationMessage.getEntry();
        boolean success = true;

        try {
            switch (communicationMessage.getTable()) {
                case TABLE_NAME_UTILISATEUR: {
                    Utilisateur user = (Utilisateur) entry;
                    final String INE = user.getINE();
                    final String nom = user.getNom();
                    final String prenom = user.getPrenom();
                    final String type = user.getType();
                    final String password = user.getPassword();
                    final String[] groups = user.getGroups();

                    ResultSet set = DatabaseManager.getInstance().registerNewUser(
                            INE,
                            password,
                            nom,
                            prenom,
                            type,
                            String.join(";", groups)
                    );

                    if (set.next()) {
                        user.setID(set.getLong(1));
                    }

                    user.setPassword("");

                    break;
                }


                case TABLE_NAME_GROUPE:
                    Groupe groupe = (Groupe) entry;
                    entry = DatabaseManager.getInstance().createNewGroup(
                            groupe.getLabel()
                    );

                    success = entry != null;

                    break;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }


        if (success) {
            Host.broadcast(CommunicationMessage.createEntryAddedMessage(communicationMessage.getTable(), entry));
        }


    }

    /**
     * Fonction qui traite la demande des table de modèle
     */
    private void handleTableModelRequestMessage() {

        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Table request received");
        if (!isAdminOrStaff()) {
            Debugger.logColorMessage(DBG_COLOR, "ClientManager", "But user isn't an admin");
            return;
        }

        try {
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            List<Utilisateur> users = databaseManager.retrieveAllUsers();
            for (Utilisateur u : users) {
                u.setGroups(databaseManager.relatedUserGroup(u.getINE()).split(";"));
            }

            List<Groupe> groups = databaseManager.retrieveAllGroups();
            List<Ticket> tickets = databaseManager.retrieveAllTickets();
            List<Message> messages = databaseManager.retrieveAllMessages();

            sendData(CommunicationMessage.createTableModel(
                    users,
                    groups,
                    tickets,
                    messages
            ));

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Fonction qui traite le fait qu'un client doit
     * recevoir tous les messages sortant du serveur
     */
    private synchronized void handleRequestEverythingMessage() {

        if (!isAdminOrStaff()) {
            return;
        }

        Host.addAdmin(this);

    }


    /**
     * Fonction qui traite le fait qu'un message soit recu par un utilisateur
     *
     * @param communicationMessage - Le message et l'utilisateur
     */
    private synchronized void handleMessageReceivedMessage(CommunicationMessage communicationMessage) {

        DatabaseManager database = DatabaseManager.getInstance();
        ArrayList<Message> messages = communicationMessage.getMessagesReceived();

        Debugger.logColorMessage(Debugger.BLUE, "ClientManager", user.getNom() + " has received " + messages);

        for (Message message : messages) {
            try {
                int entryUpdated = database.setMessageReceived(message, user);
                Debugger.logColorMessage(Debugger.BLUE, "ClientManager", entryUpdated + " entries updated");
                if (entryUpdated > 0) {
                    Message m = database.getMessage(message.getID());
                    if (m != null) {
                        Ticket ticket = database.relatedMessageTicket(message.getID());
                        if (ticket != null) {
                            Groupe groupe = database.relatedTicketGroup(ticket.getID());
                            if (groupe != null) {
                                CommunicationMessage msg = CommunicationMessage.createMessageUpdatedMessage(TABLE_NAME_MESSAGE, m, groupe, ticket);
                                Host.broadcastToGroup(msg, groupe.getLabel());
                                Host.sendToClient(database.ticketCreator(ticket.getID()), msg);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public BufferedWriter getSocketWriter() {
        return mWriteStream;
    }

    @Override
    public BufferedReader getSocketReader() {
        return mReadStream;
    }

    /**
     * Ici nous ne faisons rien, en effet, lorsque le client
     * se reconnecte, le serveur crée un nouveau socket et
     * récupère de nouveau toutes les données qui lui sont
     * destinées.
     *
     * @param message - Un message
     */
    @Override
    public void addPendingMessage(CommunicationMessage message) {

    }
}
