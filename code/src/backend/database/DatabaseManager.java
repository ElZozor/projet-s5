package backend.database;

import backend.modele.GroupModel;
import backend.modele.MessageModel;
import backend.modele.TicketModel;
import backend.modele.UserModel;
import com.mysql.jdbc.StringUtils;
import debug.Debugger;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static backend.database.Keys.*;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:mysql://localhost/projets5";


    private static DatabaseManager mDatabase;


    /**
     * As this class is a Singleton, this function returns
     * the unique instance of this class.
     *
     * @return                  An instance of DataBaseManager
     * @throws SQLException     Can throw an exception if the database can't be reached
     */
    public static DatabaseManager getInstance() throws SQLException, NoSuchAlgorithmException {
        if (mDatabase == null) {
            mDatabase = new DatabaseManager();
        }

        return mDatabase;
    }


    private Connection databaseConnection;
    private MessageDigest digest = MessageDigest.getInstance("SHA-256");

    private DatabaseManager() throws SQLException, NoSuchAlgorithmException {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Cannot launch the database driver");
            e.printStackTrace();
        }

        databaseConnection = DriverManager.getConnection(DB_URL, "projet", "");
    }

    /**
     * Used to hash a password
     * @param       password The password to hash
     * @return      The hashed password converted into base64
     */
    public String hashPassword(@NotNull String password) {
        return Base64.getEncoder().encodeToString(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Variadic function that test if an given
     * bunch of String contains an empty or null String
     *
     * @param args  The bunch of string
     * @return      true if there is an empty string false otherwise
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
     * @return true if present otherwise false
     * @throws SQLException Can throw an exception if the database can't be reached
     */
    public Boolean userExists(String ine) throws SQLException {
        if (ine == null) {
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
    public Boolean credentialsAreValid(String ine, String password) throws SQLException {
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

        return queryResult.next();
    }


    private Boolean addUserGroupRelation(String ine, String group_label) throws SQLException {
        String request = String.format(
                "INSERT INTO %s (%s, %s) " +
                        "SELECT DISTINCT %s.%s, %s.%s " +
                        "FROM %s, %s " +
                        "WHERE %s.%s = '%s' AND %s.%s = '%s'",

                TABLE_NAME_APPARTENIR, APPARTENIR_UTILISATEUR_INE, APPARTENIR_GROUPE_ID,
                TABLE_NAME_UTILISATEUR, UTILISATEUR_ID, TABLE_NAME_GROUPE, GROUPE_ID,
                TABLE_NAME_UTILISATEUR, TABLE_NAME_GROUPE,
                TABLE_NAME_UTILISATEUR, UTILISATEUR_INE, ine, TABLE_NAME_GROUPE, GROUPE_LABEL, group_label
        );

        PreparedStatement statement = databaseConnection.prepareStatement(request);

        return statement.execute();
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


    private Boolean addMessageVuRelation(int message_id, String ticketID) throws SQLException {
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
                TABLE_NAME_UTILISATEUR, UTILISATEUR_ID, TABLE_NAME_APPARTENIR, APPARTENIR_UTILISATEUR_INE
        );

        PreparedStatement statement = databaseConnection.prepareStatement(request);

        return statement.executeUpdate() > 0;
    }


    public ResultSet insertNewMessage(String contenu, String ticketid, String ine) throws SQLException {
        // Message creation in the "message" table
        final String messageRequest = String.format(
                "INSERT INTO %s (%s, %s, %s) VALUES ('%s', '%s', '%s')",
                TABLE_NAME_MESSAGE, MESSAGE_CONTENU, MESSAGE_TICKET_ID, MESSAGE_UTILISATEUR_INE,
                contenu, ticketid, ine
        );

        PreparedStatement statement = databaseConnection.prepareStatement(messageRequest, Statement.RETURN_GENERATED_KEYS);

        // We execute the request and then get the resulting keys
        statement.executeUpdate();

        ResultSet result = statement.getGeneratedKeys();
        result.next();
        addMessageVuRelation(result.getInt(1), ticketid);

        result.previous();
        return statement.getGeneratedKeys();
    }

    private ResultSet insertNewTicket(String userINE, String title, String group_label) throws SQLException {
        // Ticket creation in the "ticket" table
        final String ticketRequest = String.format(
                "INSERT INTO %s (%s, %s, %s) " +
                        "SELECT DISTINCT '%s', %s.%s, %s.%s " +
                        "FROM %s, %s " +
                        "WHERE %s.%s = '%s' " +
                        "AND %s.%s = '%s' ",
                TABLE_NAME_TICKET, TICKET_TITRE, TICKET_UTILISATEUR_ID, TICKET_GROUP_ID,
                title, TABLE_NAME_UTILISATEUR, UTILISATEUR_ID, TABLE_NAME_GROUPE, GROUPE_ID,
                TABLE_NAME_UTILISATEUR, TABLE_NAME_GROUPE,
                TABLE_NAME_UTILISATEUR, UTILISATEUR_INE, userINE,
                TABLE_NAME_GROUPE, GROUPE_LABEL, group_label
        );

        PreparedStatement statement = databaseConnection.prepareStatement(ticketRequest, Statement.RETURN_GENERATED_KEYS);

        // We execute the request and then get the resulting keys
        statement.executeUpdate();
        return statement.getGeneratedKeys();
    }

    private ResultSet createUserMessageLink(String userID, String messageID) throws SQLException {
        Statement statement = databaseConnection.createStatement();

        final String linkRequest = String.format(
                "INSERT INTO %s (%s, %s) VALUES ('%s', '%s')",
                TABLE_NAME_VU, VU_UTILISATEUR_ID, VU_MESSAGE_ID,
                userID, messageID
        );

        statement.executeUpdate(linkRequest);

        return statement.getGeneratedKeys();
    }


    private ResultSet insertTicketMessageUserRelation(String ticketID, String userID, String messageID) throws SQLException {

        final String query = String.format(
                "INSERT INTO %s(%s, %s) " +
                        "SELECT '%s', %s.%s " +
                        "FROM %s, %s, %s " +
                        "WHERE %s.%s = '%s' " +
                        "AND %s.%s = %s.%s " +
                        "AND %s.%s = %s.%s",
                TABLE_NAME_VU, VU_MESSAGE_ID, VU_UTILISATEUR_ID,
                messageID, TABLE_NAME_UTILISATEUR, UTILISATEUR_ID,
                TABLE_NAME_UTILISATEUR, TABLE_NAME_APPARTENIR, TABLE_NAME_TICKET,
                TABLE_NAME_TICKET, TICKET_ID, ticketID,
                TABLE_NAME_TICKET, TICKET_GROUP_ID, TABLE_NAME_APPARTENIR, APPARTENIR_GROUPE_ID,
                TABLE_NAME_UTILISATEUR, UTILISATEUR_ID, TABLE_NAME_APPARTENIR, UTILISATEUR_ID
        );

        PreparedStatement statement = databaseConnection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        statement.executeUpdate();


        return statement.getGeneratedKeys();
    }


    /**
     * Create a new ticket into the database
     *
     * @param userINE           The user who creates the ticket
     * @param title             The ticket title
     * @param message           The main message of the ticket
     * @param groupID           The concerned groups
     * @return                  Whether the creation is successful
     * @throws SQLException     Can throw an exception if the database can't be reached
     */
    public Boolean createNewTicket(String userINE, String title, String message, String groupID) throws SQLException {

        if (containsNullOrEmpty(title, message, groupID)) {
            return false;
        }

        final ResultSet ticketBDD = insertNewTicket(userINE, title, groupID);
        if (!ticketBDD.next()) {
            return false;
        }

        int ticketID = ticketBDD.getInt(1);
        final ResultSet messageBDD = insertNewMessage(message, Integer.toString(ticketBDD.getInt(1)), userINE);
        if (!messageBDD.next()) {
            return false;
        }


        return insertTicketMessageUserRelation(
                ticketBDD.getString(TICKET_ID),
                userINE,
                messageBDD.getString(MESSAGE_ID)
        ).next();
    }


    /**
     * Insert a new message into the database
     *
     * @param ine           The used id
     * @param ticketid      The ticket id
     * @param contents      The message contents
     * @return              Whether the request is a success
     * @throws SQLException Can thow an exception if the database can't be reached
     */
    public Boolean addNewMessage(String ine, String ticketid, String contents) throws SQLException {

        if (ine == null || ticketid == null || contents == null) {
            return false;
        }

        if (ine.isEmpty() || ticketid.isEmpty() || contents.isEmpty()) {
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
                    TABLE_NAME_MESSAGE, MESSAGE_TICKET_ID, MESSAGE_UTILISATEUR_INE, MESSAGE_CONTENU,
                    ticketid, ine, contents
            );

            return statement.executeUpdate(request) > 0;
        }


        return false;

    }


    public UserModel retrieveUserModel() throws SQLException {
        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "SELECT * FROM %s",
                TABLE_NAME_UTILISATEUR
        );

        return new UserModel(statement.executeQuery(request));
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
                    TABLE_NAME_APPARTENIR, TABLE_NAME_APPARTENIR, APPARTENIR_UTILISATEUR_INE, id
            );

            Debugger.logMessage("updateExistingUserGroup", "Request: " + request);
            statement.execute(request);


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
                TABLE_NAME_APPARTENIR, APPARTENIR_UTILISATEUR_INE, TABLE_NAME_UTILISATEUR, UTILISATEUR_ID,
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


}
