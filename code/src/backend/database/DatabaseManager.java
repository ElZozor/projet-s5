package backend.database;

import java.sql.*;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:mysql://localhost/EMP";
    private static final String USER_TABLE_NAME     = "users";
    private static final String TICKETS_TABLE_NAME  = "tickets";
    private static final String MESSAGE_TABLE_NAME  = "messages";

    private static DatabaseManager mDatabase;


    /**
     * As this class is a Singleton, this function returns
     * the unique instance of this class.
     *
     * @return                  An instance of DataBaseManager
     * @throws SQLException     Can throw an exception if the database can't be reached
     */
    public static DatabaseManager getInstance() throws SQLException {
        if (mDatabase == null) {
            mDatabase = new DatabaseManager();
        }

        return mDatabase;
    }





    private Connection databaseConnection;

    private DatabaseManager() throws SQLException {
        databaseConnection = DriverManager.getConnection(DB_URL);
    }

    /**
     * Check whether an user is present into the database
     *
     * @param id            the user id
     * @return              true if present otherwise false
     * @throws SQLException Can throw an exception if the database can't be reached
     */
    public Boolean checkUserPresence(String id, String password, Boolean checkPassword) throws SQLException {
        if (id == null) {
            return false;
        }

        if (checkPassword && password == null) {
            return false;
        }

        Statement statement = databaseConnection.createStatement();
        String request;
        if (checkPassword) {
            request = String.format(
                    "SELECT * FROM %s WHERE id='%s'",
                    id
            );
        } else {
            request = String.format(
                    "SELECT * FROM %s WHERE id='%s' AND password='%s'",
                    id, password
            );
        }

        ResultSet queryResult = statement.executeQuery(request);

        return queryResult.next();
    }




    /**
     * Register a new user in the user database
     *
     * @param id            the user id
     * @param password      the user password
     * @param name          the user name
     * @param surname       the user surname
     * @return              whether the request is successful
     * @throws SQLException Can throw an exception if the database can't be reached
     */
    public Boolean registerNewUser(String id, String password, String name, String surname) throws SQLException {
        if (id == null || password == null || name == null || surname == null) {
            return false;
        }

        if (id.isEmpty() || password.isEmpty() || name.isEmpty() || surname.isEmpty()) {
            return false;
        }

        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "INSERT INTO %s (id, password, name, surname) VALUES ('%s' '%s' '%s' '%s')",
                USER_TABLE_NAME, id, password, name, surname
        );

        statement.executeQuery(request);

        return true;
    }




    /**
     * Create a new ticket into the database
     *
     * @param id                The used who creates the tickets
     * @param title             The ticket title
     * @param message           The main message of the ticket
     * @param groups            The concerned groups
     * @param date              The create date
     * @return                  Whether the creation is successful
     * @throws SQLException Can throw an exception if the database can't be reached
     */
    public Boolean createNewTicket(String id, String title, String message, String groups, long date) throws SQLException {

        if (id == null || title == null || message == null || groups == null) {
            return false;
        }

        if (id.isEmpty() || title.isEmpty() || message.isEmpty() || groups.isEmpty()) {
            return false;
        }

        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "INSERT INTO %s (id, title, message, groups, date) VALUES ('%s' '%s' '%s' '%s' '%s')",
                id, title, message, groups, Long.toString(date)
        );

        return true;
    }


    /**
     * Insert a new message into the database
     *
     * @param id            The used id
     * @param ticketid      The ticket id
     * @param contents      The message contents
     * @return              Whether the request is a success
     * @throws SQLException Can thow an exception if the database can't be reached
     */
    public Boolean addNewMessage(String id, String ticketid, String contents) throws SQLException {

        if (id == null || ticketid == null || contents == null) {
            return false;
        }

        if (id.isEmpty() || ticketid.isEmpty() || contents.isEmpty()) {
            return false;
        }

        Statement statement = databaseConnection.createStatement();
        String request = String.format(
                "SELECT groups FROM %s WHERE id='%s'",
                USER_TABLE_NAME, id
        );

        if (statement.execute(request)) {
            request = String.format(
                    "INSERT INTO %s (id, ticketid, contents) VALUES ('%s' '%s' '%s')",
                    id, ticketid, contents
            );

            return statement.execute(request);
        }


        return false;

    }

}
