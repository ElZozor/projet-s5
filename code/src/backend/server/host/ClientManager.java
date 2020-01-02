package backend.server.host;

import backend.data.Groupe;
import backend.database.DatabaseManager;
import backend.server.Server;
import backend.server.communication.CommunicationMessage;
import debug.Debugger;
import org.json.JSONArray;

import java.io.*;
import java.net.Socket;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

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
        mRSAKey = generateAESKeys();

        String key = mReadStream.readLine();
        if (key == null) {
            throw new ServerInitializationFailedException("Error while getting rsa key");
        }

        mOtherPublicKey = CommunicationMessage.getKeyXChangePublicKey(key);

        if (mOtherPublicKey == null) {
            throw new ServerInitializationFailedException("Error while getting rsa key");
        }

        mWriteStream.write(CommunicationMessage.createKeyXChange(getPublicKey()));
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
                CommunicationMessage communicationMessage = readData(mReadStream);
                if (communicationMessage == null) {
                    continue;
                }


                handleMessage(communicationMessage);

            } catch (IOException | CommunicationMessage.InvalidMessageException e) {
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
     * <p>
     * Otherwise, the function will crash..
     * <p>
     * It will check the message "type" value and redirect it
     * to the proper function that must handle the action.
     *
     * @param communicationMessage The message to handle
     */
    private void handleMessage(CommunicationMessage communicationMessage) {

        switch (communicationMessage.getType()) {

            case CONNECTION:
                handleConnection(communicationMessage);
                break;

            case TICKET:
                handleTicketCreation(communicationMessage);
                break;

            case MESSAGE:
                handleClassicMessage(communicationMessage);
                break;

            case LOCAL_UPDATE:
                handleLocalUpdateMessage(communicationMessage);
                break;

            default:
                try {
                    sendData(mWriteStream, CommunicationMessage.createNack(ERROR_MESSAGE_HANDLE_DEMAND, getOtherPublicKey()));
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
     * @param communicationMessage The connection message
     */
    private void handleConnection(CommunicationMessage communicationMessage) {

        boolean queryResult = false;
        PublicKey pk = getOtherPublicKey();
        String fail_reason = "";

        if (pk == null) {
            return;
        }

        try {

            if (communicationMessage.isConnection()) {
                DatabaseManager database = DatabaseManager.getInstance();
                queryResult = database.credentialsAreValid(communicationMessage.getConnectionINE(), communicationMessage.getConnectionPassword());


                if (queryResult) {

                    userINE = communicationMessage.getConnectionINE();
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
                sendData(mWriteStream, CommunicationMessage.createAck(getOtherPublicKey()));
            } else {
                sendData(mWriteStream, CommunicationMessage.createNack(fail_reason, getOtherPublicKey()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Function that handles a ticket message.
     * Used to create a new ticket.
     *
     * @param communicationMessage The message that contains the ticket informations
     */
    private void handleTicketCreation(CommunicationMessage communicationMessage) {

        Boolean queryResult = false;
        PublicKey pk = getOtherPublicKey();
        String fail_reason = "";

        if (pk == null) {
            return;
        }


        try {

            if (communicationMessage.isTicket()) {

                final String title = communicationMessage.getTicketTitle();
                final String contents = communicationMessage.getTicketMessage();
                final String group = communicationMessage.getTicketGroup();

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
                sendData(mWriteStream, CommunicationMessage.createAck(getOtherPublicKey()));
            } else {
                sendData(mWriteStream, CommunicationMessage.createNack(fail_reason, getOtherPublicKey()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Used to handle a classic message.
     *
     * @param communicationMessage The message data.
     */
    private void handleClassicMessage(CommunicationMessage communicationMessage) {
        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Classic message: \n" + communicationMessage.toString());
        Boolean queryResult = false;
        PublicKey pk = getOtherPublicKey();
        String fail_reason = "";

        if (pk == null) {
            return;
        }


        try {

            if (communicationMessage.isMessage()) {

                String ticketid = communicationMessage.getMessageTicketID().toString();
                String contents = communicationMessage.getMessageContents();

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
                sendData(mWriteStream, CommunicationMessage.createAck(getOtherPublicKey()));
            } else {
                sendData(mWriteStream, CommunicationMessage.createNack(fail_reason, getOtherPublicKey()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void handleLocalUpdateMessage(CommunicationMessage communicationMessage) {

        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Handling a local update message");

        String fail_reason = "";
        Boolean queryResult = false;

        CommunicationMessage result = null;

        try {
            TreeSet<Groupe> groups = DatabaseManager.getInstance().treatLocalUpdateMessage();
            JSONArray array = new JSONArray();

            for (Groupe group : groups) {
                array.put(group.toJSON());
            }

            result = CommunicationMessage.createLocalUpdateResponse(groups, getOtherPublicKey());

            queryResult = true;

        } catch (SQLException e) {
            e.printStackTrace();
            fail_reason = ERROR_MESSAGE_DATABASE_ERROR;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            fail_reason = ERROR_MESSAGE_SERVER_ERROR;
        }


        try {
            if (queryResult) {
                sendData(mWriteStream, result);
            } else {
                sendData(mWriteStream, CommunicationMessage.createNack(fail_reason, getOtherPublicKey()));
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
