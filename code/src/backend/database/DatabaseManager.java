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


    public String hashPassword(@NotNull String password) {
        return Base64.getEncoder().encodeToString(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
    }

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
                TABLE_NAME_UTILISATEUR, UTILISATEUR_INE, UTILISATEUR_MDP,
                ine, hashPassword(password)
        );

        ResultSet queryResult = statement.executeQuery(request);

        return queryResult.next();
    }


    /**
     * Retrieve all the groups that the concerned user is affialiated with.
     *
     * @param userINE The user INE
     * @return All the groups as a Collection<String>
     * @throws SQLException Can be thrown while accessing the database
     */
    public Collection<String> retrieveAffiliatedGroups(String userINE) throws SQLException {
        if (userINE == null) {
            return null;
        }

        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "SELECT DISTINCT %s.%s FROM %s, %s, %s "
                        + " WHERE %s.%s = %s.%s AND %s.%s = %s.%s",
                TABLE_NAME_GROUPE, GROUPE_LABEL, TABLE_NAME_GROUPE, TABLE_NAME_APPARTENIR, TABLE_NAME_UTILISATEUR,
                TABLE_NAME_GROUPE, GROUPE_ID, TABLE_NAME_APPARTENIR, APPARTENIR_GROUPE_ID, TABLE_NAME_UTILISATEUR,
                UTILISATEUR_INE, TABLE_NAME_APPARTENIR, APPARTENIR_UTILISATEUR_INE
        );

        ResultSet queryResult = statement.executeQuery(request);

        List<String> groups = new LinkedList<>();
        while (queryResult.next()) {
            groups.add(queryResult.getString(GROUPE_LABEL));
        }

        return groups;
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
    public ResultSet registerNewUser(String ine, String password, String name, String surname, String type) throws SQLException {
        if (containsNullOrEmpty(ine, password, name, surname, type)) {
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

        if (statement.executeUpdate() == 1) {
            return statement.getGeneratedKeys();
        }

        return null;
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


    public ResultSet insertNewMessage(String message) throws SQLException {
        Statement statement = databaseConnection.createStatement();

        // Message creation in the "message" table
        final String messageRequest = String.format(
                "INSERT INTO %s (%s) VALUES ('%s')",
                TABLE_NAME_MESSAGE, MESSAGE_CONTENU,
                message
        );

        // We execute the request and then get the resulting keys
        statement.executeUpdate(messageRequest);
        return statement.getGeneratedKeys();
    }

    private ResultSet insertNewTicket(String userID, String title, String groupID) throws SQLException {
        Statement statement = databaseConnection.createStatement();

        // Ticket creation in the "ticket" table
        final String ticketRequest = String.format(
                "INSERT INTO %s (%s %s %s) VALUES ('%s', '%s', '%s')",
                TABLE_NAME_TICKET, TICKET_UTILISATEUR_ID, TICKET_TITRE, TICKET_GROUP_ID,
                userID, title, groupID
        );

        // We execute the request and then get the resulting keys
        statement.executeUpdate(ticketRequest);
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
        Statement statement = databaseConnection.createStatement();


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

        if (title == null || message == null || groupID == null) {
            return false;
        }

        if (title.isEmpty() || groupID.isEmpty()) {
            return false;
        }

        final ResultSet ticketBDD = insertNewTicket(userINE, title, groupID);
        if (! ticketBDD.next()) {
            return false;
        }

        final ResultSet messageBDD = insertNewMessage(message);
        if (! messageBDD.next()) {
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

    public Boolean editExistingUser(long id, String ine, String name, String surname, String type) throws SQLException {
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

        return statement.executeUpdate(request) == 1;
    }


}
