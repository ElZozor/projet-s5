package backend.database;

import backend.data.Groupe;
import backend.data.Message;
import backend.data.Ticket;
import backend.data.Utilisateur;
import com.mysql.jdbc.StringUtils;
import debug.Debugger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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


    /**
     * Constructeur de DatabaseManager, privé car c'est un singleton.
     *
     * @throws SQLException             - Peut être jetée si la connection à la database échoue
     * @throws NoSuchAlgorithmException - Ne devrait normalement pas être jetée
     */
    private DatabaseManager() throws SQLException, NoSuchAlgorithmException, IOException {
        try {
            Class.forName("com.mysql.jdbc.Driver");

        } catch (ClassNotFoundException e) {
            System.err.println("Cannot launch the database driver");
            e.printStackTrace();
        }

        databaseConnection = DriverManager.getConnection(DB_URL, username, password);

        checkTableExistance();
    }

    /**
     * Initialise le singleton. Comme le constructeur jète des exceptions, pour éviter celles-ci
     * partout dans le code, il a été préférable d'initialiser la database de cette manière
     *
     * @throws SQLException             - Peut être jeté si la connection à la bdd échoue
     * @throws NoSuchAlgorithmException - Ne devrait normalement pas être jetée
     */
    public static void initDatabaseConnection() throws SQLException, NoSuchAlgorithmException, IOException {
        mDatabase = new DatabaseManager();
    }

    /**
     * Teste si les tables existent et dans le cas contraire, les initialises.
     *
     * @throws SQLException
     * @throws IOException
     */
    private void checkTableExistance() throws SQLException, IOException {
        boolean found = false;
        ResultSet set = databaseConnection.getMetaData().getTables(null, null, null,
                new String[]{"TABLE"});

        for (; set.next() && !found; ) {
            if (set.getString("TABLE_NAME").equals("UTILISATEUR")) {
                found = true;
            }
        }

        if (!found) {
            File file = new File("res/database.sql");
            BufferedReader reader = new BufferedReader(new FileReader(file));

            String line;
            StringBuilder builder = new StringBuilder();
            for (; (line = reader.readLine()) != null; ) {
                builder.append(line);
                if (line.contains("--")) {
                    builder = new StringBuilder();
                } else if (line.contains(";")) {
                    String query = builder.toString();
                    query = query.replaceAll("\\s", " ");
                    System.out.println("Exécution de la requête: " + query);
                    Statement statement = databaseConnection.createStatement();
                    statement.executeUpdate(query);

                    builder = new StringBuilder();
                }
            }
        }
    }

    public void closeConnection() throws SQLException {
        databaseConnection.close();
    }

    /**
     * Comme cette classe est un singleton, cette fonction retourne
     * l'unique instance de "DatabaseManager"
     *
     * @return - L'instance de DatabaseManager
     */
    public static DatabaseManager getInstance() {
        return mDatabase;
    }

    /**
     * Utilisé pour hasher un mot de passe
     * @param password - Le mot de passe à hasher
     * @return - Le mot de passe hashé puis encodé en b64
     */
    public String hashPassword(@NotNull String password) {
        return Base64.getEncoder().encodeToString(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Fonction variadique qui teste si une chaine de caractère parmis
     * celles données à la fonction est vide ou bien nulle
     * @param args - Le groupe de String
     * @return - true si une chaine est vide ou nulle, false sinon
     */
    private Boolean containsNullOrEmpty(String... args) {
        for (String s : args) {
            if (s == null || StringUtils.isEmptyOrWhitespaceOnly(s)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Teste si un utilisateur est présent dans la bdd
     * @param ine - L'ine de l'utilisateur
     * @return - Vrai si il est présent , faux sinon
     * @throws SQLException - Peut être lancée en cas d'erreur sql
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
     * Teste si les identifiants d'un utilisateur sont valides ou non
     * @param ine - L'ine de l'utilisateur
     * @param password - Le mot de passe
     * @return - Un booléen
     * @throws SQLException - Peut être lancée en cas de requête invalide
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


    /**
     * Ajoute la relation entre un groupe et un utilisateur ( appartient )
     *
     * @param ine         - l'ine de l'utilisateur
     * @param group_label - Le label du groupe
     * @return - Si la requête à fonctionnée
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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
     * Enregistre un nouvel utilisateur sur la bdd
     * @param ine - L'ine de l'utilisateur
     * @param password - Le mot de passe
     * @param name - Son nom
     * @param surname - Son prénom
     * @param type - Son type
     * @return - Si la requête à réussi ou non
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
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


    /**
     * Crée un nouveau groupe
     *
     * @param label - Le nom du groupe
     * @return - Le groupe résultant
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
    public Groupe createNewGroup(String label) throws SQLException {
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
            ResultSet set = statement.getGeneratedKeys();
            if (set.next()) {
                return new Groupe(set.getLong(1), label);
            }
        }

        return null;
    }


    /**
     * Ajoute la relation entre un message et un utilisateur pour la table VU
     *
     * @param message_id - Le message correspondant
     * @param ticketID   - Le ticket correspondant
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
    private void addMessageVuRelation(long message_id, long ticketID) throws SQLException {
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

        statement.executeUpdate();
    }


    /**
     * Ajouter la relation entre un message et un utilisateur pour la table RECU
     *
     * @param id       - L'id du message
     * @param ticketid - Le ticket correspondant
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
    private void addMessageRecuRelation(long id, long ticketid) throws SQLException {

        String request = String.format(
                "INSERT INTO %s(%s, %s) " +
                        "SELECT %s, %s.%s " +
                        "FROM %s, %s, %s " +
                        "WHERE %s.%s = %s " +
                        "AND %s.%s = %s.%s " +
                        "AND %s.%s = %s.%s",
                TABLE_NAME_RECU, RECU_MESSAGE_ID, RECU_UTILISATEUR_ID,
                id, TABLE_NAME_UTILISATEUR, UTILISATEUR_ID,
                TABLE_NAME_UTILISATEUR, TABLE_NAME_APPARTENIR, TABLE_NAME_TICKET,
                TABLE_NAME_TICKET, TICKET_ID, ticketid,
                TABLE_NAME_TICKET, TICKET_GROUP_ID, TABLE_NAME_APPARTENIR, APPARTENIR_GROUPE_ID,
                TABLE_NAME_UTILISATEUR, UTILISATEUR_ID, TABLE_NAME_APPARTENIR, APPARTENIR_UTILISATEUR_ID
        );

        PreparedStatement statement = databaseConnection.prepareStatement(request);

        statement.executeUpdate();

    }

    /**
     * Renvoie tous les utilisateur qui doivent lire le message en question
     *
     * @param id - L'id du message
     * @return - Les utilisateur qui doivent lire le message
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
    public ArrayList<String> getRemainingReadUsernames(Long id) throws SQLException {
        ArrayList<String> result = new ArrayList<>();

        final Statement statement = databaseConnection.createStatement();
        final String query = String.format(
                "SELECT %s.* " +
                        "FROM %s, %s " +
                        "WHERE %s.%s = '%s' " +
                        "AND %s.%s = %s.%s",
                TABLE_NAME_UTILISATEUR,
                TABLE_NAME_UTILISATEUR, TABLE_NAME_VU,
                TABLE_NAME_VU, VU_MESSAGE_ID, id,
                TABLE_NAME_UTILISATEUR, UTILISATEUR_ID, TABLE_NAME_VU, VU_UTILISATEUR_ID
        );

        ResultSet set = statement.executeQuery(query);
        while (set.next()) {
            result.add(set.getString(UTILISATEUR_NOM) + " " + set.getString(UTILISATEUR_PRENOM));
        }

        return result;

    }


    /**
     * Renvoie tous les utilisateurs qui doivent recevoir ce message
     *
     * @param id - L'id du message
     * @return - Les utilisateurs qui doivent le recevoir
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
    public ArrayList<String> getRemainingReceiveUsernames(Long id) throws SQLException {
        ArrayList<String> result = new ArrayList<>();

        final Statement statement = databaseConnection.createStatement();
        final String query = String.format(
                "SELECT %s.* " +
                        "FROM %s, %s " +
                        "WHERE %s.%s = '%s' " +
                        "AND %s.%s = %s.%s",
                TABLE_NAME_UTILISATEUR,
                TABLE_NAME_UTILISATEUR, TABLE_NAME_RECU,
                TABLE_NAME_RECU, RECU_MESSAGE_ID, id,
                TABLE_NAME_UTILISATEUR, UTILISATEUR_ID, TABLE_NAME_RECU, RECU_UTILISATEUR_ID
        );

        ResultSet set = statement.executeQuery(query);
        while (set.next()) {
            result.add(set.getString(UTILISATEUR_NOM) + " " + set.getString(UTILISATEUR_PRENOM));
        }

        return result;

    }


    /**
     * Insère un nouveau message dans la base de donnée
     *
     * @param contenu  - Le contenu du message
     * @param ticketid - Le ticket correspondant
     * @param userID   - L'utilisateur qui a posté le message
     * @return - Le message résultant
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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
        addMessageRecuRelation(id, ticketid);

        result.next();
        final Date postDate = result.getTimestamp(MESSAGE_HEURE_ENVOIE);

        Message resultingMessage = new Message(id, userID, ticketid, postDate, contenu, getRemainingReadUsernames(id), getRemainingReceiveUsernames(id));
        Debugger.logMessage("DatabaseManager", "Resulting message: " + resultingMessage.toJSON());

        return resultingMessage;
    }

    /**
     * Insère un nouveau ticket dans la base de donnée
     *
     * @param userID      - L'utilisateur qui a créé le ticket
     * @param title       - Le titre du ticket
     * @param group_label - Le nom du groupe affilié
     * @return - Les clés générés par la requête
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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


    /**
     * Crée un nouveau ticket dans la base de donnée
     * @param userID - L'id de l'utilisateur qui crée le ticket
     * @param title - Le titre du ticket
     * @param message - Le premier message du ticket
     * @param groupLabel - Le groupe concerné
     * @return - Si la création est un succès ou non
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
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
     * Insère un nouveau message dans la base de donnée
     * @param ine - L'ine de l'utilisateur
     * @param ticketid - L'id du ticket concerné
     * @param contents - Le contenu du message
     * @return - Si la requête est un succès ou non
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
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


    /**
     * Retourne tous les utilisateurs de la base de donnée
     *
     * @return - Tous les utilisateur de la base de donnée
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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

    /**
     * Retourne tous les groupes de la base de donnée
     *
     * @return - Tous les groupes de la base de donnée
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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


    /**
     * Retourne tous les ticket de la base de donnée
     *
     * @return - Tous les ticket de la base de donnée
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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

    /**
     * Retourne tous les messages de la base de donnée
     *
     * @return Tous les messages de la base de donnée
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
    public ArrayList<Message> retrieveAllMessages() throws SQLException {
        Statement statement = databaseConnection.createStatement();

        String request = String.format(
                "SELECT * FROM %s",
                TABLE_NAME_MESSAGE
        );

        ArrayList<Message> result = new ArrayList<>();
        ResultSet set = statement.executeQuery(request);
        while (set.next()) {
            final Long id = set.getLong(MESSAGE_ID);
            result.add(new Message(set, getRemainingReadUsernames(id), getRemainingReceiveUsernames(id)));
        }

        return result;
    }


    /**
     * Supprime un utilisateur de la base de donnée
     *
     * @param id - L'id de l'utilisateur
     * @return Si l'utilisateur a bien été supprimé
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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

    /**
     * Supprime un groupe de la base de donnée
     *
     * @param id - L'id du groupe
     * @return - Si le groupe a bien été supprimé
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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

    /**
     * Supprime un ticket de la base de donnée
     *
     * @param id - L'id du ticket
     * @return - Si le ticket a bien été supprimé
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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

    /**
     * Supprime un message de la base de donnée
     *
     * @param id - L'id du message
     * @return - Si le message a bien été supprimé
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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

    /**
     * Edite un groupe existant
     *
     * @param id    - L'id du groupe
     * @param label - Le nom du groupe
     * @return - Si le groupe a bien été edité
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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


    /**
     * Edite les groupes d'un utilisateur existant
     *
     * @param id     - L'id de l'uilisateur
     * @param ine    - Son ine
     * @param groups - Les nouveaux groupes
     */
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


    /**
     * Edite un utilisateur existant
     *
     * @param id      - L'id de l'utilisateur
     * @param ine     - Son ine
     * @param name    - Son nom
     * @param surname - Son prenom
     * @param type    - Son type
     * @param groups  - Ses groupes
     * @return Si l'utilisateur a bien été édité
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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


    /**
     * Edite un utilisateur existant change sont mot de passe
     * si il n'est pas vide ou null sinon appeles l'autre fonction pour éditer
     * les utilisateurs
     *
     * @param id       - L'id de l'utilisateur
     * @param ine      - Son ine
     * @param name     - Son nom
     * @param surname  - Son prenom
     * @param type     - Son type
     * @param groups   - Ses groupes
     * @param password - Son mot de passe
     * @return Si l'utilisateur a bien été édité
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
    public Boolean editExistingUser(long id, String ine, String name, String surname, String type, String groups, String password) throws SQLException {
        if (containsNullOrEmpty(password)) {
            Debugger.logMessage("DatabaseManager", "password is null or empty edditing the classic way");
            return editExistingUser(id, ine, name, surname, type, groups);
        }

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

        PreparedStatement statement = databaseConnection.prepareStatement(request);

        Boolean result = statement.executeUpdate(request) == 1;
        updateExistingUserGroups(id, ine, groups);

        return result;
    }

    /**
     * Retourne tous les groupe relié à un utilisateur
     *
     * @param ine - L'ine de l'utilisateur
     * @return Tous les groupe liés à l'utilisateur
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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

    /**
     * Retourne tous les messages liés à un ticket
     *
     * @param ticketid - L'id du ticket
     * @return Tous les messages liés à ce ticket
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
    public TreeSet<Message> getAllMessagesForGivenTicket(long ticketid) throws SQLException {

        final String messageRequest = String.format(
                "SELECT * FROM %s WHERE %s = '%s'",
                TABLE_NAME_MESSAGE, MESSAGE_TICKET_ID, ticketid
        );

        Statement statement = databaseConnection.createStatement();
        ResultSet result = statement.executeQuery(messageRequest);

        TreeSet<Message> messages = new TreeSet<>();

        while (result.next()) {
            final Long id = result.getLong(MESSAGE_ID);
            messages.add(new Message(result, getRemainingReadUsernames(id), getRemainingReceiveUsernames(id)));
        }

        return messages;
    }


    /**
     * Retourne tous les tickets liés à un groupe
     *
     * @param groupid - L'id du groupe
     * @return - Tous les tickets liés à un groupe
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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

    /**
     * Retourne tous les groupes liés à un utilisateur + tous les tickets liés à cet utilisateur
     *
     * @param user - L'utilisateur en question
     * @return - Tous les groupes liés à un utilisateur + tous les tickets liés à cet utilisateur
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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
            final long id = set.getLong(GROUPE_ID);
            final String label = set.getString(GROUPE_LABEL);
            TreeSet<Ticket> tickets = getAllTicketForGivenGroup(id);

            Groupe groupe = new Groupe(id, label, tickets);
            groupes.add(groupe);
        }

        TreeSet<Groupe> others = getRelatedTickets(user);
        groupes.addAll(others);

        return groupes;

    }

    /**
     * Retourne tous les ticket reliés à un utilisateur
     *
     * @param user - L'utilisateur en question
     * @return - Tous les tickets reliés à un utilisateur
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
    private TreeSet<Groupe> getRelatedTickets(Utilisateur user) throws SQLException {

        HashMap<Long, Groupe> groupes = new HashMap<>();

        Statement statement = databaseConnection.createStatement();
        final String query = String.format(
                "SELECT * FROM %s WHERE %s.%s = '%s'",
                TABLE_NAME_TICKET, TABLE_NAME_TICKET, TICKET_UTILISATEUR_ID, user.getID()
        );


        ResultSet set = statement.executeQuery(query);
        while (set.next()) {
            final long id = set.getLong(TICKET_ID);
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

    /**
     * Traite une mise à jour locale
     *
     * @param user - L'utilisateur qui en fait la demande
     * @return - Toutes les données liés à l'utilisateur
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
    public TreeSet<Groupe> treatLocalUpdateMessage(Utilisateur user) throws SQLException {
        return getRelatedGroups(user);
    }

    /**
     * Retourne tous les noms des groupes
     *
     * @return - Tous les noms des groupes
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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

    /**
     * Retourne tous les groupes reliés à un ticket
     *
     * @param ticketID - Le ticket en question
     * @return - Tous les groupes reliés à ce ticket
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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


    /**
     * Retoure un ticket pour un certain id
     *
     * @param ticketid - L'id
     * @return - Le ticket si dispo sinon null
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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

    /**
     * Retourne un groupe pour un id donné
     *
     * @param id - L'id
     * @return - Le groupe si dispo sinon null
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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

    /**
     * Retourne l'id du créateur d'un ticket
     *
     * @param ticketID - Le ticket en question
     * @return - L'id du créateur, 0 si non trouvé
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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

    /**
     * Mets tous les message d'un ticket à LU pour un utilisateur donné
     *
     * @param ticketID - L'id du ticket
     * @param userID   - L'id de l'utilisateur
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
    public int setMessagesFromTicketRead(Long ticketID, Long userID) throws SQLException {

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

        ResultSet set = statement.executeQuery(query);
        Statement other = databaseConnection.createStatement();
        int result = 0;
        while (set.next()) {
            Long messageID = set.getLong(VU_MESSAGE_ID);
            final String update = String.format(
                    "DELETE FROM %s WHERE %s.%s = '%s' AND %s.%s = '%s'",
                    TABLE_NAME_VU, TABLE_NAME_VU, VU_MESSAGE_ID, messageID,
                    TABLE_NAME_VU, VU_UTILISATEUR_ID, userID
            );

            result += other.executeUpdate(update);
        }

        return result;

    }

    /**
     * Retourne tous les utilisateurs
     *
     * @return Tous les utilisateurs
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
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

    /**
     * Le ticket lié à un message
     *
     * @param id - L'id du message
     * @return - Le ticket lié à ce message
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
    public Ticket relatedMessageTicket(Long id) throws SQLException {

        final String query = String.format(
                "SELECT DISTINCT %s.* " +
                        "FROM %s, %s " +
                        "WHERE %s.%s = '%s' " +
                        "AND %s.%s = %s.%s",
                TABLE_NAME_TICKET,
                TABLE_NAME_TICKET, TABLE_NAME_MESSAGE,
                TABLE_NAME_MESSAGE, MESSAGE_ID, id,
                TABLE_NAME_TICKET, TICKET_ID, TABLE_NAME_MESSAGE, MESSAGE_TICKET_ID
        );

        ResultSet set = databaseConnection.createStatement().executeQuery(query);
        if (set.next()) {
            final long ticketid = set.getLong(TICKET_ID);
            final String title = set.getString(TICKET_TITRE);

            return new Ticket(ticketid, title, getAllMessagesForGivenTicket(ticketid));
        }

        return null;

    }

    /**
     * Marque un message comme reçu
     *
     * @param message - Le message en question
     * @param user    - L'utilisateur en question
     * @throws SQLException - Peut être lancée en cas d'erreur sur la requête
     */
    public int setMessageReceived(Message message, Utilisateur user) throws SQLException {

        String request = String.format(
                "DELETE FROM %s " +
                        "WHERE %s.%s = '%s' " +
                        "AND %s.%s = '%s'",
                TABLE_NAME_RECU,
                TABLE_NAME_RECU, RECU_MESSAGE_ID, message.getID(),
                TABLE_NAME_RECU, RECU_UTILISATEUR_ID, user.getID()
        );

        Statement statement = databaseConnection.createStatement();
        return statement.executeUpdate(request);

    }

    public Message getMessage(Long id) throws SQLException {

        final String query = String.format(
                "SELECT * FROM %s WHERE %s.%s = '%s'",
                TABLE_NAME_MESSAGE, TABLE_NAME_MESSAGE, MESSAGE_ID, id
        );

        Statement statement = databaseConnection.createStatement();
        ResultSet set = statement.executeQuery(query);

        if (set.next()) {
            return new Message(set, getRemainingReadUsernames(id), getRemainingReceiveUsernames(id));
        }

        return null;

    }
}
