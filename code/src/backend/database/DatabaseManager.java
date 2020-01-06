package backend.database;

import backend.data.Groupe;
import backend.data.Message;
import backend.data.Ticket;
import backend.data.Utilisateur;
import backend.modele.GroupModel;
import backend.modele.MessageModel;
import backend.modele.TicketModel;
import com.mysql.jdbc.StringUtils;
import debug.Debugger;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Date;
import java.util.*;

import static backend.database.Keys.*;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:mysql://localhost/projets5";
    private static final String username = "projet";
    private static final String password = "";


    private static DatabaseManager mDatabase;
    private Connection databaseConnection;
    private MessageDigest digest = MessageDigest.getInstance("SHA-256");

    private DatabaseManager() throws SQLException, NoSuchAlgorithmException {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Cannot launch the database driver");
            e.printStackTrace();
        }

        databaseConnection = DriverManager.getConnection(DB_URL, username, password);
    }

    public static void initDatabaseConnection() throws SQLException, NoSuchAlgorithmException {
        mDatabase = new DatabaseManager();
    }

    /**
     * As this class is a Singleton, this function returns
     * the unique instance of this class.
     *
     * @return An instance of DataBaseManager
     * @throws SQLException Can throw an exception if the database can't be reached
     */
    public static DatabaseManager getInstance() {
        return mDatabase;
    }

    /**
     * Used to hash a password
     *
     * @param password The password to hash
     * @return The hashed password converted into base64
     */
    public String hashPassword(@NotNull String password) {
        return Base64.getEncoder().encodeToString(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Variadic function that test if an given
     * bunch of String contains an empty or null String
     *
     * @param args The bunch of string
     * @return true if there is an empty string false otherwise
     */
    private Boolean containsNullOrEmpty(String... args) {
        for (String s : args) {
            if (StringUtils.isNullOrEmpty(s)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Check whether an user is present into the database
     *
     * @param ine the user ine
     * @return true if present false otherwise
     * @throws SQLException Can throw an exception if the database can't be reached
     */
    public Boolean userExists(String ine) throws SQLException {
        if (containsNullOrEmpty(ine)) {
            return false;
        }

        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "SELECT * FROM %s WHERE %s='%s'",
                TABLE_NAME_UTILISATEUR, UTILISATEUR_INE, ine
        );

        ResultSet queryResult = statement.executeQuery(request);

        return queryResult.next();
    }


    /**
     * Check if the user credentials are valid or not.
     *
     * @param ine      The user ine
     * @param password The user password
     * @return A boolean
     * @throws SQLException Can be thrown during request
     */
    public ResultSet credentialsAreValid(String ine, String password) throws SQLException {
        if (ine == null || password == null) {
            return null;
        }

        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "SELECT * FROM %s WHERE %s='%s' AND %s='%s'",
                TABLE_NAME_UTILISATEUR,
                UTILISATEUR_INE, ine,
                UTILISATEUR_MDP, hashPassword(password)
        );

        ResultSet queryResult = statement.executeQuery(request);

        return queryResult;
    }


    private Boolean addUserGroupRelation(String ine, String group_label) throws SQLException {
        String request = String.format(
                "INSERT INTO %s (%s, %s) " +
                        "SELECT DISTINCT %s.%s, %s.%s " +
                        "FROM %s, %s " +
                        "WHERE %s.%s = '%s' AND %s.%s = '%s'",

                TABLE_NAME_APPARTENIR, APPARTENIR_UTILISATEUR_ID, APPARTENIR_GROUPE_ID,
                TABLE_NAME_UTILISATEUR, UTILISATEUR_ID, TABLE_NAME_GROUPE, GROUPE_ID,
                TABLE_NAME_UTILISATEUR, TABLE_NAME_GROUPE,
                TABLE_NAME_UTILISATEUR, UTILISATEUR_INE, ine, TABLE_NAME_GROUPE, GROUPE_LABEL, group_label
        );

        PreparedStatement statement = databaseConnection.prepareStatement(request);

        return statement.executeUpdate() > 0;
    }

    /**
     * Register a new user in the user database
     *
     * @param ine      the user ine
     * @param password the user password
     * @param name     the user name
     * @param surname  the user surname
     * @param type     the user type
     * @return whether the request is successful
     * @throws SQLException Can throw an exception if the database can't be reached
     */
    public ResultSet registerNewUser(String ine, String password, String name, String surname, String type, String groups) throws SQLException {
        if (containsNullOrEmpty(ine, password, name, surname, type, groups)) {
            return null;
        }

        String request = String.format(
                "INSERT INTO %s (%s, %s, %s, %s, %s) VALUES ('%s', '%s', '%s', '%s', '%s')",
                TABLE_NAME_UTILISATEUR,
                UTILISATEUR_INE, UTILISATEUR_MDP, UTILISATEUR_NOM, UTILISATEUR_PRENOM, UTILISATEUR_TYPE,
                ine, hashPassword(password), name, surname, type
        );

        PreparedStatement statement = databaseConnection.prepareStatement(request, Statement.RETURN_GENERATED_KEYS);

        Debugger.logMessage("DataBaseManager", "Executing following request: " + request);

        if (statement.executeUpdate() != 1) {
            return null;
        }

        ResultSet generatedKeys = statement.getGeneratedKeys();

        System.out.println(groups);
        for (String g : groups.split(";")) {
            System.out.println("Relation : " + g);
            try {
                if (!addUserGroupRelation(ine, g)) {
                    createNewGroup(g);
                    addUserGroupRelation(ine, g);
                }

            } catch (SQLException e) {
                e.printStackTrace();

                try {
                    createNewGroup(g);
                    addUserGroupRelation(ine, g);
                } catch (SQLException f) {
                    f.printStackTrace();
                }

            }
        }

        return generatedKeys;
    }


    public ResultSet createNewGroup(String label) throws SQLException {
        if (containsNullOrEmpty(label)) {
            return null;
        }

        String request = String.format(
                "INSERT INTO %s (%s) VALUES ('%s')",
                TABLE_NAME_GROUPE,
                GROUPE_LABEL,
                label
        );

        PreparedStatement statement = databaseConnection.prepareStatement(request, Statement.RETURN_GENERATED_KEYS);

        Debugger.logMessage("DataBaseManager", "Executing following request: " + request);

        if (statement.executeUpdate() == 1) {
            return statement.getGeneratedKeys();
        }

        return null;
    }


    private Boolean addMessageVuRelation(long message_id, long ticketID) throws SQLException {
        String request = String.format(
                "INSERT INTO %s(%s, %s) " +
                        "SELECT %s, %s.%s " +
                        "FROM %s, %s, %s " +
                        "WHERE %s.%s = %s " +
                        "AND %s.%s = %s.%s " +
                        "AND %s.%s = %s.%s",
                TABLE_NAME_VU, VU_MESSAGE_ID, VU_UTILISATEUR_ID,
                message_id, TABLE_NAME_UTILISATEUR, UTILISATEUR_ID,
                TABLE_NAME_UTILISATEUR, TABLE_NAME_APPARTENIR, TABLE_NAME_TICKET,
                TABLE_NAME_TICKET, TICKET_ID, ticketID,
                TABLE_NAME_TICKET, TICKET_GROUP_ID, TABLE_NAME_APPARTENIR, APPARTENIR_GROUPE_ID,
                TABLE_NAME_UTILISATEUR, UTILISATEUR_ID, TABLE_NAME_APPARTENIR, APPARTENIR_UTILISATEUR_ID
        );

        PreparedStatement statement = databaseConnection.prepareStatement(request);

        return statement.executeUpdate() > 0;
    }

    public TreeSet<String> getRemainingReadUsernames(Long id) throws SQLException {
        TreeSet<String> result = new TreeSet<>();

        final Statement statement = databaseConnection.createStatement();
        final String query = String.format(
                "SELECT %s.%s " +
                        "FROM %s, %s " +
                        "WHERE %s.%s = '%s' " +
                        "AND %s.%s = %s.%s",
                TABLE_NAME_UTILISATEUR, UTILISATEUR_NOM,
                TABLE_NAME_UTILISATEUR, TABLE_NAME_VU,
                TABLE_NAME_VU, VU_MESSAGE_ID, id,
                TABLE_NAME_UTILISATEUR, UTILISATEUR_ID, TABLE_NAME_VU, VU_UTILISATEUR_ID
        );

        ResultSet set = statement.executeQuery(query);
        while (set.next()) {
            result.add(set.getString(UTILISATEUR_NOM));
        }

        return result;

    }

    public Message insertNewMessage(final String contenu, final long ticketid, final long userID) throws SQLException {
        // Message creation in the "message" table
        final String messageRequest = String.format(
                "INSERT INTO %s (%s, %s, %s) VALUES ('%s', '%s', '%s')",
                TABLE_NAME_MESSAGE, MESSAGE_CONTENU, MESSAGE_TICKET_ID, MESSAGE_UTILISATEUR_ID,
                contenu, ticketid, userID
        );

        Debugger.logMessage("DatabaseManager", "Request: " + messageRequest);

        PreparedStatement statement = databaseConnection.prepareStatement(messageRequest, Statement.RETURN_GENERATED_KEYS);
        Debugger.logMessage("DatabaseManager", "Affected rows: " + statement.executeUpdate());

        ResultSet result = statement.getGeneratedKeys();
        if (!result.next()) {
            Debugger.logMessage("DatabaseManager", "No key inserted after query ");
            return null;
        }

        final long id = result.getInt(1);
        String query = String.format(
                "SELECT * FROM %s WHERE %s.%s = '%s'",
                TABLE_NAME_MESSAGE, TABLE_NAME_MESSAGE, MESSAGE_ID, id
        );

        result = statement.executeQuery(query);
        addMessageVuRelation(id, ticketid);

        result.next();
        final Date postDate = result.getTimestamp(MESSAGE_HEURE_ENVOIE);
        final int state = result.getInt(MESSAGE_STATE);

        Message resultingMessage = new Message(id, userID, ticketid, postDate, contenu, getRemainingReadUsernames(id));
        Debugger.logMessage("DatabaseManager", "Resulting message: " + resultingMessage.toJSON());

        return resultingMessage;
    }

    private ResultSet insertNewTicket(long userID, String title, String group_label) throws SQLException {
        // Ticket creation in the "ticket" table
        final String ticketRequest = String.format(
                "INSERT INTO %s (%s, %s, %s) " +
                        "SELECT DISTINCT '%s', '%s', %s.%s " +
                        "FROM %s " +
                        "WHERE %s.%s = '%s' ",
                TABLE_NAME_TICKET, TICKET_TITRE, TICKET_UTILISATEUR_ID, TICKET_GROUP_ID,
                title, userID, TABLE_NAME_GROUPE, GROUPE_ID,
                TABLE_NAME_GROUPE,
                TABLE_NAME_GROUPE, GROUPE_LABEL, group_label
        );

        PreparedStatement statement = databaseConnection.prepareStatement(ticketRequest, Statement.RETURN_GENERATED_KEYS);

        // We execute the request and then get the resulting keys
        statement.executeUpdate();
        return statement.getGeneratedKeys();
    }

    private ResultSet createUserMessageLink(final String userID, final String messageID) throws SQLException {
        Statement statement = databaseConnection.createStatement();

        final String linkRequest = String.format(
                "INSERT INTO %s (%s, %s) VALUES ('%s', '%s')",
                TABLE_NAME_VU, VU_UTILISATEUR_ID, VU_MESSAGE_ID,
                userID, messageID
        );

        statement.executeUpdate(linkRequest);

        return statement.getGeneratedKeys();
    }


    private ResultSet insertTicketMessageUserRelation(long ticketID, long groupID, long messageID) throws SQLException {

        final String query = String.format(
                "INSERT INTO %s (%s, %s) " +
                        "SELECT '%s', %s.%s " +
                        "from %s, %s " +
                        "WHERE %s.%s = %s " +
                        "AND %s.%s = %s.%s",
                TABLE_NAME_VU, VU_MESSAGE_ID, VU_UTILISATEUR_ID,
                messageID, TABLE_NAME_UTILISATEUR, UTILISATEUR_ID,
                TABLE_NAME_UTILISATEUR, TABLE_NAME_APPARTENIR,
                TABLE_NAME_APPARTENIR, APPARTENIR_GROUPE_ID, groupID,
                TABLE_NAME_UTILISATEUR, UTILISATEUR_ID, TABLE_NAME_APPARTENIR, APPARTENIR_UTILISATEUR_ID
        );
        System.out.println(query);

        PreparedStatement statement = databaseConnection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        statement.executeUpdate();


        return statement.getGeneratedKeys();
    }


    /**
     * Create a new ticket into the database
     *
     * @param userID     The user who creates the ticket
     * @param title      The ticket title
     * @param message    The main message of the ticket
     * @param groupLabel The concerned groups
     * @return Whether the creation is successful
     * @throws SQLException Can throw an exception if the database can't be reached
     */
    public Ticket createNewTicket(long userID, String title, String message, String groupLabel) throws SQLException {

        if (containsNullOrEmpty(title, message, groupLabel)) {
            return null;
        }

        final ResultSet ticketBDD = insertNewTicket(userID, title, groupLabel);
        if (!ticketBDD.next()) {
            Debugger.logMessage("DatabaseManager", "No next, returning null");
            return null;
        }

        long ticketID = ticketBDD.getLong(1);
        Groupe relatedGroup = relatedTicketGroup(ticketID);

        final Message messageBDD = insertNewMessage(message, ticketBDD.getLong(1), userID);
        if (messageBDD != null) {
            TreeSet<Message> messages = new TreeSet<>();
            messages.add(messageBDD);

            return new Ticket(ticketID, title, messages);
        }
        Debugger.logMessage("DatabaseManager", "messageBDD = null || ticketMessageUserRelation == false");
        return null;
    }


    /**
     * Insert a new message into the database
     *
     * @param ine      The used id
     * @param ticketid The ticket id
     * @param contents The message contents
     * @return Whether the request is a success
     * @throws SQLException Can thow an exception if the database can't be reached
     */
    public Boolean addNewMessage(String ine, String ticketid, String contents) throws SQLException {

        if (containsNullOrEmpty(ine, ticketid, contents)) {
            return false;
        }

        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "SELECT groups FROM %s WHERE %s='%s'",
                TABLE_NAME_UTILISATEUR, UTILISATEUR_INE, ine
        );

        if (statement.execute(request)) {
            request = String.format(
                    "INSERT INTO %s (%s, %s, %s) VALUES ('%s', '%s', '%s')",
                    TABLE_NAME_MESSAGE, MESSAGE_TICKET_ID, MESSAGE_UTILISATEUR_ID, MESSAGE_CONTENU,
                    ticketid, ine, contents
            );

            return statement.executeUpdate(request) > 0;
        }


        return false;

    }


    public ArrayList<Utilisateur> retrieveAllUsers() throws SQLException {
        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "SELECT * FROM %s",
                TABLE_NAME_UTILISATEUR
        );

        ArrayList<Utilisateur> result = new ArrayList<>();
        ResultSet set = statement.executeQuery(request);
        while (set.next()) {
            result.add(new Utilisateur(set));
        }

        return result;
    }

    public ArrayList<Groupe> retrieveAllGroups() throws SQLException {
        Statement statement = databaseConnection.createStatement();

        String request = String.format(
                "SELECT * FROM %s",
                TABLE_NAME_GROUPE
        );

        ArrayList<Groupe> result = new ArrayList<>();
        ResultSet set = statement.executeQuery(request);
        while (set.next()) {
            result.add(new Groupe(set));
        }

        return result;
    }

    public ArrayList<Ticket> retrieveAllTickets() throws SQLException {
        Statement statement = databaseConnection.createStatement();

        String request = String.format(
                "SELECT * FROM %s",
                TABLE_NAME_TICKET
        );

        ArrayList<Ticket> result = new ArrayList<>();
        ResultSet set = statement.executeQuery(request);
        while (set.next()) {
            result.add(new Ticket(set.getLong(TICKET_ID), set.getString(TICKET_TITRE), new TreeSet<>()));
        }

        return result;
    }

    public ArrayList<Message> retrieveAllMessages() throws SQLException {
        Statement statement = databaseConnection.createStatement();

        String request = String.format(
                "SELECT * FROM %s",
                TABLE_NAME_MESSAGE
        );

        ArrayList<Message> result = new ArrayList<>();
        ResultSet set = statement.executeQuery(request);
        while (set.next()) {
            result.add(new Message(set, getRemainingReadUsernames(set.getLong(MESSAGE_ID))));
        }

        return result;
    }

    public GroupModel retrieveGroupModel() throws SQLException {
        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "SELECT * FROM %s",
                TABLE_NAME_GROUPE
        );

        return new GroupModel(statement.executeQuery(request));
    }


    public MessageModel retrieveMessageModel() throws SQLException {
        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "SELECT * FROM %s",
                TABLE_NAME_MESSAGE
        );

        return new MessageModel(statement.executeQuery(request));
    }

    public TicketModel retrieveTicketModel() throws SQLException {
        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "SELECT * FROM %s",
                TABLE_NAME_TICKET
        );

        return new TicketModel(statement.executeQuery(request));
    }

    public Boolean deleteUser(Long id) throws SQLException {
        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "DELETE FROM %s where %s = '%s'",
                TABLE_NAME_UTILISATEUR,
                UTILISATEUR_ID,
                id.toString()
        );

        return statement.executeUpdate(request) == 1;
    }

    public Boolean deleteGroup(Long id) throws SQLException {
        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "DELETE FROM %s where %s = '%s'",
                TABLE_NAME_GROUPE,
                GROUPE_ID,
                id.toString()
        );

        return statement.executeUpdate(request) == 1;
    }

    public Boolean deleteTicket(Long id) throws SQLException {
        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "DELETE FROM %s where %s = '%s'",
                TABLE_NAME_TICKET,
                TICKET_ID,
                id.toString()
        );

        return statement.executeUpdate(request) == 1;
    }

    public Boolean deleteMessage(Long id) throws SQLException {
        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "DELETE FROM %s where %s = '%s'",
                TABLE_NAME_MESSAGE,
                MESSAGE_ID,
                id.toString()
        );

        return statement.executeUpdate(request) == 1;
    }

    public Boolean editExistingGroup(long id, String label) throws SQLException {
        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "UPDATE %s SET %s = '%s' WHERE %s = '%s'",
                TABLE_NAME_GROUPE,
                GROUPE_LABEL, label,
                GROUPE_ID, id
        );

        return statement.executeUpdate(request) == 1;
    }


    private void updateExistingUserGroups(long id, String ine, String groups) {
        try {
            Statement statement = databaseConnection.createStatement();
            String request = String.format(
                    "DELETE FROM %s WHERE %s.%s = '%s'",
                    TABLE_NAME_APPARTENIR, TABLE_NAME_APPARTENIR, APPARTENIR_UTILISATEUR_ID, id
            );

            Debugger.logMessage("updateExistingUserGroup", "Request: " + request);
            statement.executeUpdate(request);


            for (String g : groups.split(";")) {
                try {
                    Debugger.logMessage("updateExistingUserGroups", "Group: " + g);
                    if (!addUserGroupRelation(ine, g)) {
                        createNewGroup(g);

                        addUserGroupRelation(ine, g);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Boolean editExistingUser(long id, String ine, String name, String surname, String type, String groups) throws SQLException {
        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "UPDATE %s "
                        + "SET "
                        + "%s = '%s', "
                        + "%s = '%s', "
                        + "%s = '%s', "
                        + "%s = '%s' "
                        + "WHERE %s = '%s'",
                TABLE_NAME_UTILISATEUR,
                UTILISATEUR_INE, ine,
                UTILISATEUR_NOM, name,
                UTILISATEUR_PRENOM, surname,
                UTILISATEUR_TYPE, type,
                UTILISATEUR_ID, id
        );

        Boolean result = statement.executeUpdate(request) == 1;
        updateExistingUserGroups(id, ine, groups);

        return result;
    }


    public Boolean editExistingUser(long id, String ine, String name, String surname, String type, String groups, String password) throws SQLException {
        if (containsNullOrEmpty(password)) {
            Debugger.logMessage("DatabaseManager", "password is null or empty edditing the classic way");
            return editExistingUser(id, ine, name, surname, type, groups);
        }

        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "UPDATE %s "
                        + "SET "
                        + "%s = '%s', "
                        + "%s = '%s', "
                        + "%s = '%s', "
                        + "%s = '%s', "
                        + "%s = '%s' "
                        + "WHERE %s = '%s'",
                TABLE_NAME_UTILISATEUR,
                UTILISATEUR_INE, ine,
                UTILISATEUR_NOM, name,
                UTILISATEUR_PRENOM, surname,
                UTILISATEUR_TYPE, type,
                UTILISATEUR_MDP, hashPassword(password),
                UTILISATEUR_ID, id
        );

        Boolean result = statement.executeUpdate(request) == 1;
        updateExistingUserGroups(id, ine, groups);

        return result;
    }

    public String relatedUserGroup(String ine) throws SQLException {
        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "SELECT %s.%s " +
                        "FROM %s, %s, %s " +
                        "WHERE %s.%s = '%s' " +
                        "AND %s.%s = %s.%s " +
                        "AND %s.%s = %s.%s ",
                TABLE_NAME_GROUPE, GROUPE_LABEL,
                TABLE_NAME_GROUPE, TABLE_NAME_APPARTENIR, TABLE_NAME_UTILISATEUR,
                TABLE_NAME_UTILISATEUR, UTILISATEUR_INE, ine,
                TABLE_NAME_APPARTENIR, APPARTENIR_UTILISATEUR_ID, TABLE_NAME_UTILISATEUR, UTILISATEUR_ID,
                TABLE_NAME_GROUPE, GROUPE_ID, TABLE_NAME_APPARTENIR, APPARTENIR_GROUPE_ID
        );

        ResultSet result = statement.executeQuery(request);

        StringBuilder groups = new StringBuilder();
        while (result.next()) {
            groups.append(result.getString(GROUPE_LABEL)).append(";");
        }

        if (groups.length() > 0) {
            return groups.toString().substring(0, groups.length() - 1);
        } else {
            return "";
        }
    }


    public TreeSet<Message> getAllMessagesForGivenTicket(long ticketid) throws SQLException {

        final String messageRequest = String.format(
                "SELECT * FROM %s WHERE %s = '%s'",
                TABLE_NAME_MESSAGE, MESSAGE_TICKET_ID, ticketid
        );

        Statement statement = databaseConnection.createStatement();
        ResultSet result = statement.executeQuery(messageRequest);

        TreeSet<Message> messages = new TreeSet<>();

        while (result.next()) {
            messages.add(new Message(result, getRemainingReadUsernames(result.getLong(MESSAGE_ID))));
        }

        return messages;
    }


    public TreeSet<Ticket> getAllTicketForGivenGroup(long groupid) throws SQLException {

        final String ticketRequest = String.format(
                "SELECT * FROM %s WHERE %s = '%s'",
                TABLE_NAME_TICKET, TICKET_GROUP_ID, groupid
        );

        Statement statement = databaseConnection.createStatement();
        ResultSet result = statement.executeQuery(ticketRequest);

        TreeSet<Ticket> tickets = new TreeSet<>();

        while (result.next()) {

            final long id = result.getLong(TICKET_ID);
            final String title = result.getString(TICKET_TITRE);

            tickets.add(new Ticket(id, title, getAllMessagesForGivenTicket(id)));
        }

        return tickets;
    }

    public TreeSet<Groupe> getRelatedGroups(Utilisateur user) throws SQLException {

        TreeSet<Groupe> groupes = new TreeSet<>();
        final String query = String.format(
                "SELECT * FROM %s, %s " +
                        "WHERE %s.%s = '%s' " +
                        "AND %s.%s = %s.%s",
                TABLE_NAME_GROUPE, TABLE_NAME_APPARTENIR,
                TABLE_NAME_APPARTENIR, APPARTENIR_UTILISATEUR_ID, user.getID(),
                TABLE_NAME_GROUPE, GROUPE_ID, TABLE_NAME_APPARTENIR, APPARTENIR_GROUPE_ID
        );

        Statement statement = databaseConnection.createStatement();
        ResultSet set = statement.executeQuery(query);

        while (set.next()) {
            final Long id = set.getLong(GROUPE_ID);
            final String label = set.getString(GROUPE_LABEL);
            TreeSet<Ticket> tickets = getAllTicketForGivenGroup(id);

            Groupe groupe = new Groupe(id, label, tickets);
            groupes.add(groupe);
        }

        TreeSet<Groupe> others = getRelatedTickets(user);
        groupes.addAll(others);

        return groupes;

    }

    private TreeSet<Groupe> getRelatedTickets(Utilisateur user) throws SQLException {

        HashMap<Long, Groupe> groupes = new HashMap<>();

        Statement statement = databaseConnection.createStatement();
        final String query = String.format(
                "SELECT * FROM %s WHERE %s.%s = '%s'",
                TABLE_NAME_TICKET, TABLE_NAME_TICKET, TICKET_UTILISATEUR_ID, user.getID()
        );


        ResultSet set = statement.executeQuery(query);
        while (set.next()) {
            final Long id = set.getLong(TICKET_ID);
            final String titre = set.getString(TICKET_TITRE);

            Ticket ticket = new Ticket(id, titre, getAllMessagesForGivenTicket(id));
            Groupe groupe = relatedTicketGroup(id);

            if (groupes.containsKey(groupe.getID())) {
                groupes.get(groupe.getID()).addTicket(ticket);
            } else {
                groupe.addTicket(ticket);
                groupes.put(groupe.getID(), groupe);
            }
        }

        return new TreeSet<>(groupes.values());
    }

    public TreeSet<Groupe> treatLocalUpdateMessage(Utilisateur user) throws SQLException {
        return getRelatedGroups(user);
    }

    public TreeSet<String> getAllGroups() throws SQLException {

        TreeSet<String> groups = new TreeSet<>();
        final String groupRequest = String.format(
                "SELECT * FROM %s", TABLE_NAME_GROUPE
        );

        Statement statement = databaseConnection.createStatement();
        ResultSet result = statement.executeQuery(groupRequest);


        while (result.next()) {
            final String label = result.getString(GROUPE_LABEL);
            groups.add(label);
        }

        return groups;

    }

    public Groupe relatedTicketGroup(long ticketID) throws SQLException {
        final String query = String.format(
                "SELECT DISTINCT %s.* " +
                        "FROM %s, %s " +
                        "WHERE %s.%s = %s " +
                        "AND %s.%s = %s.%s",
                TABLE_NAME_GROUPE,
                TABLE_NAME_GROUPE, TABLE_NAME_TICKET,
                TABLE_NAME_TICKET, TICKET_ID, ticketID,
                TABLE_NAME_TICKET, TICKET_GROUP_ID, TABLE_NAME_GROUPE, GROUPE_ID
        );

        Statement statement = databaseConnection.createStatement();
        ResultSet set = statement.executeQuery(query);
        if (set.next()) {
            final Long id = set.getLong(GROUPE_ID);
            final String label = set.getString(GROUPE_LABEL);

            return new Groupe(id, label);
        }

        return null;
    }


    public Ticket getTicket(long ticketid) throws SQLException {
        final Statement statement = databaseConnection.createStatement();
        final String query = String.format(
                "SELECT * FROM %s WHERE %s = '%s'",
                TABLE_NAME_TICKET, TICKET_ID, ticketid
        );

        ResultSet set = statement.executeQuery(query);
        TreeSet<Message> messages = getAllMessagesForGivenTicket(ticketid);
        if (set.next()) {
            return new Ticket(set.getLong(TICKET_ID), set.getString(TICKET_TITRE), messages);
        }

        return null;
    }

    public Groupe retrieveGroupForGivenID(Long id) throws SQLException {
        final Statement statement = databaseConnection.createStatement();
        final String query = String.format(
                "SELECT * FROM %s WHERE %s = '%s'",
                TABLE_NAME_GROUPE, GROUPE_ID, id
        );

        ResultSet set = statement.executeQuery(query);
        if (set.next()) {
            return new Groupe(id, set.getString(GROUPE_LABEL));
        }

        return null;
    }

    public Long ticketCreator(Long ticketID) throws SQLException {
        Statement statement = databaseConnection.createStatement();
        final String query = String.format(
                "SELECT DISTINCT %s.%s " +
                        "FROM %s, %s " +
                        "WHERE %s.%s = '%s' " +
                        "AND %s.%s = %s.%s",
                TABLE_NAME_UTILISATEUR, UTILISATEUR_ID,
                TABLE_NAME_UTILISATEUR, TABLE_NAME_TICKET,
                TABLE_NAME_TICKET, TICKET_ID, ticketID,
                TABLE_NAME_TICKET, TICKET_UTILISATEUR_ID, TABLE_NAME_UTILISATEUR, UTILISATEUR_ID
        );

        ResultSet set = statement.executeQuery(query);
        if (set.next()) {
            return set.getLong(UTILISATEUR_ID);
        }

        return 0L;
    }

    public void setMessagesFromTicketRead(Long ticketID, Long userID) throws SQLException {

        Statement statement = databaseConnection.createStatement();

        final String query = String.format(
                "SELECT DISTINCT %s.%s FROM %s, %s, %s " +
                        "WHERE %s.%s = '%s' " +
                        "AND %s.%s = %s.%s AND " +
                        "%s.%s = '%s'",
                TABLE_NAME_VU, VU_MESSAGE_ID, TABLE_NAME_VU, TABLE_NAME_UTILISATEUR, TABLE_NAME_MESSAGE,
                TABLE_NAME_MESSAGE, MESSAGE_TICKET_ID, ticketID,
                TABLE_NAME_VU, VU_MESSAGE_ID, TABLE_NAME_MESSAGE, MESSAGE_ID,
                TABLE_NAME_VU, UTILISATEUR_ID, userID
        );

        System.out.println(query);

        ResultSet set = statement.executeQuery(query);
        Statement other = databaseConnection.createStatement();
        while (set.next()) {
            Long messageID = set.getLong(VU_MESSAGE_ID);
            final String update = String.format(
                    "DELETE FROM %s WHERE %s.%s = '%s' AND %s.%s = '%s'",
                    TABLE_NAME_VU, TABLE_NAME_VU, VU_MESSAGE_ID, messageID,
                    TABLE_NAME_VU, VU_UTILISATEUR_ID, userID
            );

            other.execute(update);
        }

    }

    public TreeSet<Utilisateur> getAllUsers() throws SQLException {
        final TreeSet<Utilisateur> users = new TreeSet<>();
        final String query = String.format(
                "SELECT * FROM %s",
                TABLE_NAME_UTILISATEUR
        );

        Statement statement = databaseConnection.createStatement();
        ResultSet set = statement.executeQuery(query);

        while (set.next()) {
            users.add(new Utilisateur(set));
        }

        return users;
    }
}
