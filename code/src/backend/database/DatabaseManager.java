package backend.database;

import debug.Debugger;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Base64;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:mysql://localhost";

    private static final String TABLE_NAME_UTILISATEUR      = "utilisateur";
    private static final String UTILISATEUR_ID              = "id";
    private static final String UTILISATEUR_MDP             = "mot_de_passe";
    private static final String UTILISATEUR_NOM             = "nom";
    private static final String UTILISATEUR_PRENOM          = "prenom";
    private static final String UTILISATEUR_HEURE_DER_MAJ   = "heure_derniere_maj";
    private static final String UTILISATEUR_INE             = "ine";
    private static final String UTILISATEUR_TYPE            = "type";

    private static final String TABLE_NAME_TICKET           = "ticket";
    private static final String TICKET_ID                   = "id";
    private static final String TICKET_UTILISATEUR_ID       = "utilisateur_id";
    private static final String TICKET_GROUP_ID             = "groupe_id";
    private static final String TICKET_TITRE                = "titre";
    private static final String TICKET_PREMIER_MESSAGE      = "premier_message";
    private static final String TICKET_DERNIER_MESSAGE      = "dernier_message";

    private static final String TABLE_NAME_MESSAGE          = "message";
    private static final String MESSAGE_ID                  = "id";
    private static final String MESSAGE_CONTENU             = "contenu";
    private static final String MESSAGE_HEURE_ENVOIE        = "heure_envoie";
    private static final String MESSAGE_UTILISATEUR_INE     = "utilisateur_ine";
    private static final String MESSAGE_TICKET_ID           = "ticket_id";


    private static final String TABLE_NAME_GROUPE           = "groupe";
    private static final String GROUPE_ID                   = "id";
    private static final String GROUPE_LABEL                = "label";

    private static final String TABLE_NAME_VU               = "vu";
    private static final String VU_UTILISATEUR_ID           = "utilisateur_id";
    private static final String VU_MESSAGE_ID               = "message_id";

    private static final String TABLE_NAME_ECRIRE           = "ecrire";
    private static final String ECRIRE_UTILISATEUR_ID       = "utilisateur_id";
    private static final String ECRIRE_MESSAGE_ID           = "message_id";
    private static final String ECRIRE_TICKET_ID            = "ticket_id";



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
            e.printStackTrace();
        }

        databaseConnection = DriverManager.getConnection(DB_URL, "root", "");
    }


    public String hashPassword(@NotNull String password) {
        return Base64.getEncoder().encodeToString(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
    }



    /**
     * Check whether an user is present into the database
     *
     * @param ine           the user ine
     * @return              true if present otherwise false
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


    public ResultSet credentialsAreValid(String ine, String password) throws SQLException {
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

        return queryResult;
    }




    /**
     * Register a new user in the user database
     *
     * @param ine           the user ine
     * @param password      the user password
     * @param name          the user name
     * @param surname       the user surname
     * @return              whether the request is successful
     * @throws SQLException Can throw an exception if the database can't be reached
     */
    public Boolean registerNewUser(String ine, String password, String name, String surname) throws SQLException {
        if (ine == null || password == null || name == null || surname == null) {
            return false;
        }

        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "INSERT INTO %s (%s, %s, %s, %s) VALUES ('%s', '%s', '%s', '%s')",
                TABLE_NAME_UTILISATEUR,
                UTILISATEUR_INE, UTILISATEUR_MDP, UTILISATEUR_NOM, UTILISATEUR_PRENOM,
                ine, hashPassword(password), name, surname
        );

        Debugger.logMessage("DataBaseManager", "Executing following request: " + request);


        return (statement.executeUpdate(request) == 1);
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

        final String ticketMessageUserRequest = String.format(
                "INSERT INTO %s (%s, %s, %s) VALUES ('%s', '%s', '%s')",
                TABLE_NAME_ECRIRE, ECRIRE_TICKET_ID, ECRIRE_UTILISATEUR_ID, ECRIRE_MESSAGE_ID,
                ticketID, userID, messageID
        );

        statement.executeUpdate(ticketMessageUserRequest);

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

}
