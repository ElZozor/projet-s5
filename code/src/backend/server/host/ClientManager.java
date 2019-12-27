package backend.server.host;

import backend.database.DatabaseManager;
import backend.server.Server;
import backend.server.message.Message;
import debug.Debugger;

import java.io.*;
import java.net.Socket;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

public class ClientManager extends Thread implements Server {


    private final static String ERROR_MESSAGE_HANDLE_DEMAND = "Le serveur ne peut pas traiter cette demande.";
    private final static String ERROR_MESSAGE_DATABASE_ERROR = "La base de donnée a rencontré une erreur.";
    private final static String ERROR_MESSAGE_SERVER_ERROR = "Le serveur a recontré une erreur.";
    private final static String ERROR_MESSAGE_EMPTY_FIELD = "Tous les champs doivent être correctement remplis !";

    private final static String DBG_COLOR = Debugger.YELLOW;

    private final Socket mSocket;
    private BufferedWriter mWriteStream;
    private BufferedReader mReadStream;

    private KeyPair mRSAKey;
    private PublicKey mOtherPublicKey;

    private String userINE;


    public ClientManager(final Socket socket) throws ServerInitializationFailedException {

        try {
            mSocket = socket;
            mWriteStream = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
            mReadStream = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

            exchangeKeys();

            Debugger.logColorMessage(DBG_COLOR, "ClientManager", "New client manager created");
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();

            throw new ServerInitializationFailedException("Cannot initialize client connection");
        }


    }

    private void exchangeKeys() throws NoSuchAlgorithmException, IOException, ServerInitializationFailedException {
        mRSAKey = generateRSAKey();

        String key = mReadStream.readLine();
        if (key == null) {
            throw new ServerInitializationFailedException("Error while getting rsa key");
        }

        mOtherPublicKey = Message.getKeyXChangePublicKey(key);

        if (mOtherPublicKey == null) {
            throw new ServerInitializationFailedException("Error while getting rsa key");
        }

        mWriteStream.write(Message.createKeyXChangeMessage(getPublicKey()));
        mWriteStream.flush();
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

            try {
                Message message = readData(mReadStream);
                if (message == null) {
                    continue;
                }


                handleMessage(message);

            } catch (IOException | Message.InvalidMessageException e) {
                e.printStackTrace();
            } catch (SocketDisconnectedException e) {
                running = false;
            }

        }

        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Client is now disconnected, cleaning things..");

        if (userINE != null) {
            final String groups;
            try {
                groups = DatabaseManager.getInstance().relatedUserGroup(userINE);
                Host.removeClient(new ArrayList<>(Arrays.asList(groups.split(";"))), this);
            } catch (SQLException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * Handle a message sent by the client.
     * You must check that the message contains the key "type"
     * before send it to this function.
     *
     * Otherwise, the function will crash..
     *
     * It will check the message "type" value and redirect it
     * to the proper function that must handle the action.
     *
     * @param message The message to handle
     */
    private void handleMessage(Message message) {

        switch (message.getType()) {

            case CONNECTION:
                handleConnection(message);
                break;

            case TICKET:
                handleTicketCreation(message);
                break;

            case MESSAGE:
                handleClassicMessage(message);
                break;

            default:
                try {
                    sendData(mWriteStream, Message.createNackMessage(ERROR_MESSAGE_HANDLE_DEMAND, getOtherPublicKey()));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;
        }

    }


    /**
     * Function that handles a client connection when a client send his
     * credential ( not when the socket connection begins ).
     *
     * @param message The connection message
     */
    private void handleConnection(Message message) {

        boolean queryResult = false;
        PublicKey pk = getOtherPublicKey();
        String fail_reason = "";

        if (pk == null) {
            return;
        }

        try {

            if (message.isConnection()) {
                DatabaseManager database = DatabaseManager.getInstance();
                queryResult = database.credentialsAreValid(message.getConnectionINE(), message.getConnectionPassword());


                if (queryResult) {

                    userINE = message.getConnectionINE();
                    String groups = database.relatedUserGroup(userINE);
                    Debugger.logColorMessage(DBG_COLOR, "Client Manager", "Affiliated groupe for " + userINE + ": " + groups);
                    Host.addClient(new ArrayList<>(Arrays.asList(groups.split(";"))), this);

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
                sendData(mWriteStream, Message.createAckMessage(getOtherPublicKey()));
            } else {
                sendData(mWriteStream, Message.createNackMessage(fail_reason, getOtherPublicKey()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Function that handles a ticket message.
     * Used to create a new ticket.
     *
     * @param message The message that contains the ticket informations
     */
    private void handleTicketCreation(Message message) {

        Boolean queryResult = false;
        PublicKey pk = getOtherPublicKey();
        String fail_reason = "";

        if (pk == null) {
            return;
        }


        try {

            if (message.isTicket()) {

                final String title = message.getTicketTitle();
                final String contents = message.getTicketMessage();
                final String group = message.getTicketGroup();

                DatabaseManager databaseManager = DatabaseManager.getInstance();
                queryResult = databaseManager.createNewTicket(userINE, title, contents, group);

            } else {

                fail_reason = ERROR_MESSAGE_EMPTY_FIELD;

            }

        } catch (SQLException e) {
            e.printStackTrace();
            fail_reason = ERROR_MESSAGE_DATABASE_ERROR;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            fail_reason = ERROR_MESSAGE_SERVER_ERROR;
        }


        try {
            if (queryResult) {
                sendData(mWriteStream, Message.createAckMessage(getOtherPublicKey()));
            } else {
                sendData(mWriteStream, Message.createNackMessage(fail_reason, getOtherPublicKey()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Used to handle a classic message.
     *
     * @param message   The message data.
     */
    private void handleClassicMessage(Message message) {
        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Classic message: \n" + message.toString());
        Boolean queryResult = false;
        PublicKey pk = getOtherPublicKey();
        String fail_reason = "";

        if (pk == null) {
            return;
        }


        try {

            if (message.isMessage()) {

                String ticketid = message.getMessageTicketID().toString();
                String contents = message.getMessageContents();

                DatabaseManager database = DatabaseManager.getInstance();
                queryResult = database.addNewMessage(userINE, ticketid, contents);

            } else {

                fail_reason = ERROR_MESSAGE_EMPTY_FIELD;

            }

        } catch (SQLException e) {
            e.printStackTrace();
            fail_reason = ERROR_MESSAGE_DATABASE_ERROR;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            fail_reason = ERROR_MESSAGE_SERVER_ERROR;
        }


        try {
            if (queryResult) {
                sendData(mWriteStream, Message.createAckMessage(getOtherPublicKey()));
            } else {
                sendData(mWriteStream, Message.createNackMessage(fail_reason, getOtherPublicKey()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    @Override
    public PrivateKey getPrivateKey() {
        return mRSAKey.getPrivate();
    }

    @Override
    public PublicKey getPublicKey() {
        return mRSAKey.getPublic();
    }

    @Override
    public PublicKey getOtherPublicKey() {
        return mOtherPublicKey;
    }
}
