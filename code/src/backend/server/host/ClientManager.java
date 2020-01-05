package backend.server.host;

import backend.data.*;
import backend.database.DatabaseManager;
import backend.server.Server;
import backend.server.communication.classic.ClassicMessage;
import debug.Debugger;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.security.NoSuchAlgorithmException;
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

    private boolean isAdminOrStaff() {
        if (user == null) {
            return false;
        }

        return user.getType().equals("admin") || user.getType().equals("staff");
    }

    /**
     * Main function overridden from the parent class "Thread".
     * It will run in background until the Host.running value became "false"
     * or the main program stop.
     * <p>
     * It handle the client message including connection, disconnection,
     * and all actions that the client can perform.
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
            } catch (SQLException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * Handle a message sent by the client.
     * You must check that the message contains the key "type"
     * before send it to this function.
     * <p>
     * Otherwise, the function will crash..
     * <p>
     * It will check the message "type" value and redirect it
     * to the proper function that must handle the action.
     *
     * @param classicMessage The message to handle
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
                handleLocalUpdateMessage(classicMessage);
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
        }

    }


    /**
     * Function that handles a client connection when a client send his
     * credential ( not when the socket connection begins ).
     *
     * @param classicMessage The connection message
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
        } catch (NoSuchAlgorithmException e) {
            fail_reason = ERROR_MESSAGE_SERVER_ERROR;
        }


        try {
            if (queryResult) {
                sendData(ClassicMessage.createAck());
            } else {
                sendData(ClassicMessage.createNack(fail_reason));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Function that handles a ticket message.
     * Used to create a new ticket.
     *
     * @param classicMessage The message that contains the ticket informations
     */
    private void handleTicketCreation(ClassicMessage classicMessage) {

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
                            relatedGroup.getID()
                    );

                    Host.broadcast(
                            message,
                            relatedGroup.getLabel()
                    );

                    sendData(message);
                }
            }

        } catch (SQLException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Used to handle a classic message.
     *
     * @param classicMessage The message data.
     */
    private void handleClassicMessage(ClassicMessage classicMessage) {
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
                    Long userID = database.ticketCreator(insertedMessage.getTicketID());
                    if (group != null) {
                        ClassicMessage message = ClassicMessage.createMessageAddedMessage(
                                TABLE_NAME_MESSAGE,
                                insertedMessage,
                                group.getID(),
                                insertedMessage.getTicketID()
                        );

                        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Broadcasting to group : " + group);
                        Host.broadcast(message, group.getLabel());
                        Host.sendToClient(userID, message);
                    }

                }

            }

        } catch (SQLException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }


    private void handleLocalUpdateMessage(ClassicMessage classicMessage) {

        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Handling a local update message");

        try {
            TreeSet<Groupe> relatedGroups = DatabaseManager.getInstance().treatLocalUpdateMessage(user);
            TreeSet<String> allGroups = DatabaseManager.getInstance().getAllGroups();

            sendData(ClassicMessage.createLocalUpdateResponse(relatedGroups, allGroups));
        } catch (SQLException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

    }

    private void handleTicketClickedMessage(ClassicMessage classicMessage) {

        try {
            Ticket ticket = DatabaseManager.getInstance().getTicket(classicMessage.getTicketClickedID());
            Groupe groupe = DatabaseManager.getInstance().relatedTicketGroup(ticket.getID());
            sendData(ClassicMessage.createTicketUpdatedMessage(TABLE_NAME_TICKET, ticket, groupe.getID()));

        } catch (SQLException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

    }


    private void handleDeleteMessage(ClassicMessage classicMessage) {

        if (!isAdminOrStaff()) {
            return;
        }

        ProjectTable entry = classicMessage.getEntry();
        boolean success = true;
        boolean shouldBroadcast = false;
        Groupe relatedGroup = null;

        try {
            switch (classicMessage.getTable()) {
                case TABLE_NAME_UTILISATEUR:
                    DatabaseManager.getInstance().deleteUser(entry.getID());
                    break;


                case TABLE_NAME_GROUPE:
                    DatabaseManager.getInstance().deleteGroup(entry.getID());
                    relatedGroup = ((Groupe) entry);
                    shouldBroadcast = true;
                    break;


                case TABLE_NAME_TICKET:
                    DatabaseManager.getInstance().deleteTicket(entry.getID());
                    relatedGroup = DatabaseManager.getInstance().relatedTicketGroup(entry.getID());
                    shouldBroadcast = true;
                    break;


                case TABLE_NAME_MESSAGE:
                    DatabaseManager.getInstance().deleteMessage(entry.getID());
                    relatedGroup = DatabaseManager.getInstance().relatedTicketGroup(((Message) entry).getTicketID());
                    shouldBroadcast = true;
                    break;
            }
        } catch (NoSuchAlgorithmException | SQLException e) {
            e.printStackTrace();
            success = false;
        }


        if (success) {
            if (shouldBroadcast && relatedGroup != null) {
                Host.broadcast(
                        ClassicMessage.createEntryDeletedMessage(
                                classicMessage.getTable(),
                                classicMessage.getEntry()
                        ),
                        relatedGroup.getLabel()
                );
            }
        }
    }

    private void handleUpdateMessage(ClassicMessage classicMessage) {

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
                    final String[] groups = user.getGroups();

                    success = DatabaseManager.getInstance().editExistingUser(
                            entry.getID(),
                            INE,
                            nom,
                            prenom,
                            type,
                            String.join(";", groups)
                    );

                    break;
                }


                case TABLE_NAME_GROUPE:
                    Groupe groupe = (Groupe) entry;
                    relatedGroup = DatabaseManager.getInstance().retrieveGroupForGivenID(entry.getID()).getLabel();
                    success = DatabaseManager.getInstance().editExistingGroup(
                            groupe.getID(),
                            groupe.getLabel()
                    );

                    if (success) {
                        Host.changeGroupName(relatedGroup, groupe.getLabel());
                    }

                    relatedGroup = groupe.getLabel();

                    break;
            }
        } catch (NoSuchAlgorithmException | SQLException e) {
            e.printStackTrace();
            success = false;
        }


        if (success && relatedGroup != null) {
            Host.broadcast(ClassicMessage.createEntryUpdatedMessage(classicMessage.getTable(), entry), relatedGroup);
        }

    }

    private void handleAddMessage(ClassicMessage classicMessage) {

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

                    success = DatabaseManager.getInstance().registerNewUser(
                            INE,
                            password,
                            nom,
                            prenom,
                            type,
                            String.join(";", groups)
                    ).next();

                    relatedGroup = DatabaseManager.getInstance().relatedUserGroup(INE);

                    break;
                }


                case TABLE_NAME_GROUPE:
                    Groupe groupe = (Groupe) entry;
                    success = DatabaseManager.getInstance().createNewGroup(
                            groupe.getLabel()
                    ).next();

                    relatedGroup = ((Groupe) entry).getLabel();

                    break;
            }
        } catch (NoSuchAlgorithmException | SQLException e) {
            e.printStackTrace();
            success = false;
        }


        if (success) {
            Host.broadcast(ClassicMessage.createEntryAddedMessage(classicMessage.getTable(), entry), relatedGroup);
        }


    }

    private void handleTableModelRequestMessage() {

        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Table request received");
        if (!isAdminOrStaff()) {
            Debugger.logColorMessage(DBG_COLOR, "ClientManager", "But user isn't an admin");
            return;
        }

        try {
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            List<Utilisateur> users = databaseManager.retrieveAllUsers();
            List<Groupe> groups = databaseManager.retrieveAllGroups();
            List<Ticket> tickets = databaseManager.retrieveAllTickets();
            List<Message> messages = databaseManager.retrieveAllMessages();

            sendData(ClassicMessage.createTableModel(
                    users,
                    groups,
                    tickets,
                    messages
            ));

        } catch (NoSuchAlgorithmException | SQLException | IOException e) {
            e.printStackTrace();
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
}
