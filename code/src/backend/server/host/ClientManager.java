package backend.server.host;

import backend.database.DatabaseManager;
import backend.server.Server;
import debug.Debugger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public class ClientManager extends Thread implements Server {


    private final static String ERROR_MESSAGE_HANDLE_DEMAND     = "Le serveur ne peut pas traiter cette demande.";
    private final static String ERROR_MESSAGE_DATABASE_ERROR    = "La base de donnée a rencontré une erreur.";
    private final static String ERROR_MESSAGE_SERVER_ERROR      = "Le serveur a recontré une erreur.";
    private final static String ERROR_MESSAGE_EMPTY_FIELD       = "Tous les champs doivent être correctement remplis !";

    private final static String DBG_COLOR = Debugger.YELLOW;

    private final Socket mSocket;
    private OutputStreamWriter mWriteStream;
    private InputStreamReader mReadStream;

    private KeyPair mRSAKey;
    private PublicKey mOtherPublicKey;

    private String userINE;


    public ClientManager(final Socket socket) throws ServerInitializationFailedException {

        try {
            mSocket = socket;
            mWriteStream = new OutputStreamWriter(mSocket.getOutputStream(), StandardCharsets.UTF_8);
            mReadStream  = new InputStreamReader(mSocket.getInputStream(), StandardCharsets.UTF_8);

            mRSAKey = generateRSAKey();
            Debugger.logColorMessage(DBG_COLOR, "ClientManager", "New client manager created");
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();

            throw  new ServerInitializationFailedException();
        }


    }

    /**
     * Main function overridden from the parent class "Thread".
     * It will run in background until the Host.running value became "false"
     * or the main program stop.
     *
     * It handle the client message including connection, disconnection,
     * and all actions that the client can perform.
     */
    @Override
    public void run() {
        super.run();

        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "client manager is listening");


        while (Host.isRunning && mSocket.isConnected()) {

            try {
                String message = readData(mReadStream, "");
                if (message.isEmpty()) {
                    continue;
                }

                JSONObject messageAsJSON = null;

                try {
                    Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Trying to json this message: " + message);
                    messageAsJSON = new JSONObject(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                    try {
                        messageAsJSON = new JSONObject(decryptMessage(message, getPrivateKey()));
                    } catch (JSONException | NoSuchPaddingException
                            | NoSuchAlgorithmException | InvalidKeyException
                            | BadPaddingException | IllegalBlockSizeException f) {
                        f.printStackTrace();
                    }
                } finally {
                    if (messageAsJSON != null) {
                        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Message received:" + messageAsJSON);
                        if (messageAsJSON.has("type") && messageAsJSON.has(TYPE_DATA)) {
                            handleMessage(messageAsJSON);
                        }
                    }
                }

            } catch (IOException e) {
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
    private void handleMessage(JSONObject message) {

        switch (message.optString("type")) {
            case TYPE_KEY_XCHANGE :
                handleKeyExchange(message.getJSONObject(TYPE_DATA));
                break;

            case TYPE_CONNECTION :
                handleConnection(message.getJSONObject(TYPE_DATA));
                break;

            case TYPE_REGISTRATION :
                handleRegistration(message.getJSONObject(TYPE_DATA));
                break;

            case TYPE_TICKET :
                handleTicketCreation(message.getJSONObject(TYPE_DATA));
                break;

            case TYPE_MESSAGE :
                handleClassicMessage(message.getJSONObject(TYPE_DATA));
                break;

            default:
                try {
                    sendNackMessage(getPublicKey(), mWriteStream, ERROR_MESSAGE_HANDLE_DEMAND);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;
        }

    }


    /**
     * Function that handles a key exchange.
     *
     * @param messageData   The key exchange message.
     */
    private void handleKeyExchange(JSONObject messageData) {

        try {

            JSONArray key = messageData.getJSONArray("key");

            byte[] result = new byte[key.length()];
            for (int i = 0; i < key.length(); ++i) {
                result[i] = (byte)(key.getInt(i) & 0xFF);
            }

            KeyFactory factory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(result);
            mOtherPublicKey =  factory.generatePublic(encodedKeySpec);

            sendData(mWriteStream, createKeyExchangeMessage(getPublicKey()));



        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Function that handles a client connection when a client send his
     * credential ( not when the socket connection begins ).
     *
     * @param messageData The connection message
     */
    private void handleConnection(JSONObject messageData) {

        boolean queryResult = false;
        PublicKey pk = getOtherPublicKey();
        String fail_reason = "";

        if (pk == null) {
            return;
        }

        try {

            if (messageData.has(CONNECTION_INE) && messageData.has(CONNECTION_PASSWORD)) {

                String ine      = messageData.getString(CONNECTION_INE);
                String password = messageData.getString(CONNECTION_PASSWORD);

                DatabaseManager database = DatabaseManager.getInstance();
                queryResult = database.credentialsAreValid(ine, password);


                if (queryResult) {

                    userINE = ine;
                    Collection<String> groups = database.retrieveAffiliatedGroups(userINE);
                    Host.addClient(groups, this);

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
                sendAckMessage(pk, mWriteStream);
            } else {
                sendNackMessage(pk, mWriteStream, fail_reason);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Function that handles a registration message.
     * Use to register a new user into the database.
     *
     *
     * @param messageData   The registration message.
     */
    private void handleRegistration(JSONObject messageData) {

        Boolean queryResult = false;
        PublicKey pk = getPublicKey();
        String fail_reason = "";


        if (pk == null) {
            return;
        }

        try {

            if (messageData.has(REGISTRATION_INE) && messageData.has(REGISTRATION_NAME)
                    && messageData.has(REGISTRATION_SURNAME) && messageData.has(REGISTRATION_PASSWORD)) {

                String ine = messageData.getString(REGISTRATION_INE);
                String password = messageData.getString(REGISTRATION_PASSWORD);
                String name = messageData.getString(REGISTRATION_NAME);
                String surname = messageData.getString(REGISTRATION_SURNAME);
                String type = "type";


                DatabaseManager database = DatabaseManager.getInstance();
                ResultSet set = database.registerNewUser(ine, password, name, surname, type);
                queryResult = set != null && set.next();

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
                sendAckMessage(pk, mWriteStream);
            } else {
                sendNackMessage(pk, mWriteStream, fail_reason);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Function that handles a ticket message.
     * Used to create a new ticket.
     *
     *
     * @param messageData   The message that contains the ticket informations
     */
    private void handleTicketCreation(JSONObject messageData) {

        Boolean queryResult = false;
        PublicKey pk = getOtherPublicKey();
        String fail_reason = "";

        if (pk == null) {
            return ;
        }


        try {

            if (messageData.has(TICKET_TITLE) && messageData.has(TICKET_MESSAGE) && messageData.has(TICKET_GROUP)) {

                String title    = messageData.getString(TICKET_TITLE);
                String message  = messageData.getString(TICKET_MESSAGE);
                String group    = messageData.getString(TICKET_GROUP);

                DatabaseManager databaseManager = DatabaseManager.getInstance();
                queryResult = databaseManager.createNewTicket(userINE, title, message, group);

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
                sendAckMessage(pk, mWriteStream);
            } else {
                sendNackMessage(pk, mWriteStream, fail_reason);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Used to handle a classic message.
     *
     * @param messageData   The message data.
     */
    private void handleClassicMessage(JSONObject messageData) {
        Debugger.logColorMessage(DBG_COLOR, "ClientManager", "Classic message: \n" + messageData.toString());
        Boolean queryResult = false;
        PublicKey pk = getOtherPublicKey();
        String fail_reason = "";

        if (pk == null) {
            return ;
        }


        try {

            if (messageData.has(MESSAGE_TICKET_ID) && messageData.has(MESSAGE_TICKET_ID)) {

                String ticketid = messageData.getString(MESSAGE_TICKET_ID);
                String contents = messageData.getString(MESSAGE_CONTENTS);

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
                sendAckMessage(pk, mWriteStream);
            } else {
                sendNackMessage(pk, mWriteStream, fail_reason);
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
