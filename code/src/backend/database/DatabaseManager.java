package backend.database;

import java.sql.*;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:mysql://localhost/EMP";
    private static final String USER_TABLE_NAME = "users";

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
     * @param password      the user password
     * @return              true if present otherwise false
     * @throws SQLException Can throw an exception if the database can't be reached
     */
    public Boolean checkUserPresence(String id, String password) throws SQLException {
        if (id == null || password == null) {
            return false;
        }

        Statement statement = databaseConnection.createStatement();
        String request = "SELECT * FROM " + USER_TABLE_NAME + " WHERE id=" + id + " AND password=" + password;

        ResultSet queryResult = statement.executeQuery(request);

        return queryResult.next();
    }

}
