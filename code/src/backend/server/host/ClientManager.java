package backend.server.host;

import backend.data.*;
import backend.database.DatabaseManager;
import backend.server.Server;
import backend.server.communication.CommunicationMessage;
import backend.server.communication.classic.ClassicMessage;
import debug.Debugger;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.security.PublicKey;
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

    private PublicKey mOtherPublicKey;

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
                ClassicMessage classicMessage = readData();
                if (classicMessage == null) {
                    continue;
                }
                System.out.println("Received: " + classicMessage.toString());


                handleMessage(classicMessage);

            } catch (IOException | ClassicMessage.InvalidMessageException e) {
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
                Host.removeClient(new ArrayList<>(Arrays.asList(groups.split(";"))), user.getID(), this);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * Traite un message envoyé par le client et le dispache
     * dans les différentes fonctions appropriés
     *
     * @param classicMessage Le message à traiter
     */
    private void handleMessage(ClassicMessage classicMessage) {

        switch (classicMessage.getType()) {

            case CONNECTION:
                handleConnection(classicMessage);
                break;

            case TICKET:
                handleTicketCreation(classicMessage);
                break;

            case MESSAGE:
                handleClassicMessage(classicMessage);
                break;

            case LOCAL_UPDATE:
                handleLocalUpdateMessage();
                break;

            case TICKET_CLICKED:
                handleTicketClickedMessage(classicMessage);
                break;

            case DELETE:
                handleDeleteMessage(classicMessage);
                break;

            case UPDATE:
                handleUpdateMessage(classicMessage);
                break;

            case ADD:
                handleAddMessage(classicMessage);
                break;

            case TABLE_MODEL_REQUEST:
                handleTableModelRequestMessage();
                break;

            case REQUEST_EVERYTHING:
                handleRequestEverythingMessage();
                break;

            case MESSAGE_RECEIVED:
                handleMessageReceivedMessage(classicMessage);
                break;
        }

    }


    /**
     * Fonction qui traite une connexion.
     *
     * @param classicMessage Le message de connexion
     */
    private void handleConnection(ClassicMessage classicMessage) {

        boolean queryResult = false;
        String fail_reason = "";


        try {

            if (classicMessage.isConnection()) {
                DatabaseManager database = DatabaseManager.getInstance();
                ResultSet set = database.credentialsAreValid(classicMessage.getConnectionINE(), classicMessage.getConnectionPassword());
                queryResult = set.next();

                if (queryResult) {
                    user = new Utilisateur(set);

                    String groups = database.relatedUserGroup(user.getINE());
                    Debugger.logColorMessage(DBG_COLOR, "Client Manager", "Affiliated groupe for " + user.getINE() + ": " + groups);
                    Host.addClient(new ArrayList<>(Arrays.asList(groups.split(";"))), user.getID(), this);

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
            sendData(ClassicMessage.createAck());
        } else {
            sendData(ClassicMessage.createNack(fail_reason));
        }

    }


    /**
     * Fonction qui traite la création d'un ticket
     *
     * @param classicMessage Le message qui contient le ticket
     */
    private synchronized void handleTicketCreation(ClassicMessage classicMessage) {

        try {

            if (classicMessage.isTicket()) {

                final String title = classicMessage.getTicketTitle();
                final String contents = classicMessage.getTicketMessage();
                final String group = classicMessage.getTicketGroup();

                DatabaseManager databaseManager = DatabaseManager.getInstance();
                Ticket inserted = databaseManager.createNewTicket(user.getID(), title, contents, group);

                Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Inserted is " + (inserted == null ? "null" : "not null"));
                if (inserted != null) {
                    Groupe relatedGroup = databaseManager.relatedTicketGroup(inserted.getID());
                    Debugger.logColorMessage(DBG_COLOR, "ClientManager",
                            "Host must send : \n" + inserted.toJSON() + "\nto : " + relatedGroup.getLabel());

                    ClassicMessage message = ClassicMessage.createTicketAddedMessage(
                            TABLE_NAME_TICKET,
                            inserted,
                            relatedGroup
                    );

                    Host.broadcastToGroup(
                            message,
                            relatedGroup.getLabel()
                    );

                    System.out.println("Sending to " + user.getID());
                    sendData(message);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    /**
     * Fonction qui traite le post d'un message.
     *
     * @param classicMessage Le message à ajouter.
     */
    private synchronized void handleClassicMessage(ClassicMessage classicMessage) {
        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Classic message: \n" + classicMessage.toString());

        try {

            if (classicMessage.isMessage()) {

                Long ticketid = classicMessage.getMessageTicketID();
                String contents = classicMessage.getMessageContents();

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
                        ClassicMessage message = ClassicMessage.createMessageAddedMessage(
                                TABLE_NAME_MESSAGE,
                                insertedMessage,
                                group,
                                ticket
                        );

                        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Broadcasting to group : " + group);
                        Host.broadcastToGroup(message, group.getLabel());
                        Host.sendToClient(userID, message);
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

            sendData(ClassicMessage.createLocalUpdateResponse(relatedGroups, allGroups, users));
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Fonction qui traite un click sur un ticket
     *
     * @param classicMessage Le ticket
     */
    private synchronized void handleTicketClickedMessage(ClassicMessage classicMessage) {

        try {

            DatabaseManager manager = DatabaseManager.getInstance();

            int editted = manager.setMessagesFromTicketRead(classicMessage.getTicketClickedID(), user.getID());

            if (editted > 0) {
                Ticket ticket = manager.getTicket(classicMessage.getTicketClickedID());
                if (ticket != null) {
                    Groupe groupe = manager.relatedTicketGroup(ticket.getID());
                    if (groupe != null) {
                        ClassicMessage message = ClassicMessage.createTicketUpdatedMessage(TABLE_NAME_TICKET, ticket, groupe);
                        Host.broadcastToGroup(message, groupe.getLabel());

                        Long ticketCreator = manager.ticketCreator(ticket.getID());
                        Host.sendToClient(ticketCreator, message);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Fonction qui traite la suppression d'une entrée ( uniquement admin )
     *
     * @param classicMessage L'entrée à supprimer et sa table
     */
    private synchronized void handleDeleteMessage(ClassicMessage classicMessage) {

        if (!isAdminOrStaff()) {
            return;
        }

        ProjectTable entry = classicMessage.getEntry();
        boolean success = true;
        Groupe relatedGroup = null;
        ClassicMessage message = null;

        try {
            DatabaseManager database = DatabaseManager.getInstance();

            switch (classicMessage.getTable()) {
                case TABLE_NAME_UTILISATEUR:
                    database.deleteUser(entry.getID());
                    message = ClassicMessage.createEntryDeletedMessage(TABLE_NAME_UTILISATEUR, classicMessage.getEntry());
                    break;


                case TABLE_NAME_GROUPE:
                    relatedGroup = classicMessage.getEntryAsGroupe();
                    database.deleteGroup(entry.getID());
                    message = ClassicMessage.createEntryDeletedMessage(TABLE_NAME_GROUPE, classicMessage.getEntry());
                    break;


                case TABLE_NAME_TICKET:
                    relatedGroup = database.relatedTicketGroup(entry.getID());
                    database.deleteTicket(entry.getID());
                    message = ClassicMessage.createTicketDeletedMessage(
                            classicMessage.getTable(), classicMessage.getEntryAsTicket(), relatedGroup
                    );

                    break;


                case TABLE_NAME_MESSAGE:

                    Ticket ticket = database.relatedMessageTicket(classicMessage.getEntryAsMessage().getID());
                    if (ticket != null) {
                        relatedGroup = database.relatedTicketGroup(ticket.getID());
                        if (relatedGroup != null) {
                            if (database.deleteMessage(entry.getID())) {
                                message = ClassicMessage.createMessageDeletedMessage(
                                        classicMessage.getTable(), classicMessage.getEntryAsMessage(),
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
     * @param classicMessage L'entrée et sa table
     */
    private synchronized void handleUpdateMessage(ClassicMessage classicMessage) {

        if (!isAdminOrStaff()) {
            return;
        }

        ProjectTable entry = classicMessage.getEntry();
        boolean success = true;

        try {
            switch (classicMessage.getTable()) {
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
            ClassicMessage message = ClassicMessage.createEntryUpdatedMessage(classicMessage.getTable(), entry);
            Host.broadcast(message);
        }

    }


    /**
     * Fonction qui traite l'ajout d'une entrée ( admin uniquement )
     *
     * @param classicMessage L'entrée et sa table
     */
    private synchronized void handleAddMessage(ClassicMessage classicMessage) {

        if (!isAdminOrStaff()) {
            return;
        }

        ProjectTable entry = classicMessage.getEntry();
        boolean success = true;
        String relatedGroup = null;

        try {
            switch (classicMessage.getTable()) {
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

                    break;
                }


                case TABLE_NAME_GROUPE:
                    Groupe groupe = (Groupe) entry;
                    entry = DatabaseManager.getInstance().createNewGroup(
                            groupe.getLabel()
                    );

                    success = entry != null;

                    if (success) relatedGroup = groupe.getLabel();

                    break;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }


        if (success) {
            if (relatedGroup != null) {
                Host.broadcastToGroup(ClassicMessage.createEntryAddedMessage(classicMessage.getTable(), entry), relatedGroup);
            } else {
                Host.broadcast(ClassicMessage.createEntryAddedMessage(classicMessage.getTable(), entry));
            }
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

            sendData(ClassicMessage.createTableModel(
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
     * @param classicMessage - Le message et l'utilisateur
     */
    private synchronized void handleMessageReceivedMessage(ClassicMessage classicMessage) {

        DatabaseManager database = DatabaseManager.getInstance();
        ArrayList<Message> messages = classicMessage.getMessagesReceived();

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
                                ClassicMessage msg = ClassicMessage.createMessageUpdatedMessage(TABLE_NAME_MESSAGE, m, groupe, ticket);
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
