package backend.server.host;

import backend.database.DatabaseManager;
import backend.server.Server;
import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.SQLException;
import java.util.Date;

public class ClientManager extends Thread implements Server {



    private final Socket mSocket;
    private OutputStreamWriter mWriteStream;
    private InputStreamReader mReadStream;

    private KeyPair mRSAKey;
    private PublicKey mOtherPublicKey;


    public ClientManager(final Socket socket) throws IOException, NoSuchAlgorithmException {
        mSocket = socket;

        mWriteStream = new OutputStreamWriter(mSocket.getOutputStream(), StandardCharsets.UTF_8);
        mReadStream  = new InputStreamReader(mSocket.getInputStream(), StandardCharsets.UTF_8);

        mRSAKey = Server.generateRSAKey();
        sendData(mWriteStream, Server.createKeyExchangeMessage(getPublicKey()));
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

        StringBuilder jsonConstruct = new StringBuilder();

        while (Host.isRunning) {

            try {
                String message = readData(mReadStream);
                JSONObject messageAsJSON = null;

                try {
                    messageAsJSON = new JSONObject(message);
                } catch (JSONException e) {
                    try {
                        messageAsJSON = new JSONObject(Server.decryptMessage(message, getPrivateKey()));
                    } catch (JSONException f) {
                        f.printStackTrace();
                    }
                }

                if (messageAsJSON != null) {
                    if (messageAsJSON.has("type") && messageAsJSON.has("data")) {
                        handleMessage(messageAsJSON);
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

        switch (message.getString("type")) {
            case Server.TYPE_KEY_XCHANGE :
                handleKeyExchange(message.getJSONObject("data"));

            case Server.TYPE_CONNECTION :
                handleConnection(message.getJSONObject("data"));
                break;

            case Server.TYPE_REGISTRATION :
                handleRegistration(message);
                break;

            case Server.TYPE_TICKET :
                handleTicket(message);
                break;

            case Server.TYPE_MESSAGE :
                handleClassicMessage(message);
                break;

            default:
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

            String key = messageData.getString("key");

            KeyFactory factory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(key.getBytes());
            mOtherPublicKey =  factory.generatePublic(encodedKeySpec);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
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

        Boolean queryResult = false;
        PublicKey pk = getOtherPublicKey();

        if (pk == null) {
            return ;
        }

        try {

            if (messageData.has("id") && messageData.has("password")) {

                String id = messageData.getString("id");
                String password = messageData.getString("password");

                DatabaseManager database = DatabaseManager.getInstance();
                queryResult = database.checkUserPresence(id, password, true);

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }


        try {
            Server.sendResponseMessage(pk, mWriteStream, queryResult);
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

        if (pk == null) {
            return ;
        }

        try {

            if (messageData.has("id") && messageData.has("password")
                    && messageData.has("name") && messageData.has("surname")) {

                String id = messageData.getString("id");
                String password = messageData.getString("password");
                String name = messageData.getString("name");
                String surname = messageData.getString("surname");


                DatabaseManager database = DatabaseManager.getInstance();
                Boolean presentInDatabase = database.checkUserPresence(id, password, false);

                if (!presentInDatabase) {
                    queryResult = database.registerNewUser(id, password, name, surname);
                }

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }



        try {
            Server.sendResponseMessage(pk, mWriteStream, queryResult);
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
    private void handleTicket(JSONObject messageData) {

        Boolean queryResult = false;
        PublicKey pk = getOtherPublicKey();

        if (pk == null) {
            return ;
        }


        try {

            if (messageData.has("id") && messageData.has("title")
                    && messageData.has("message") && messageData.has("groups")) {

                String id       = messageData.getString("id");
                String title    = messageData.getString("title");
                String message  = messageData.getString("message");
                String groups   = messageData.getString("groups");

                DatabaseManager databaseManager = DatabaseManager.getInstance();
                databaseManager.createNewTicket(id, title, message, groups, new Date().getTime());

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


        try {
            Server.sendResponseMessage(pk, mWriteStream, queryResult);
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
