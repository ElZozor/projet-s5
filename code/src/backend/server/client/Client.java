package backend.server.client;


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
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class Client implements Server {

    private final static String DBG_COLOR = Debugger.YELLOW;
    private final static int SOCKET_TIMEOUT = 2500;

    private final Socket mSocket;
    private OutputStreamWriter mWriteStream;
    private InputStreamReader mReadStream;

    private KeyPair mRSAKey;
    private PublicKey mOtherPublicKey;




    /**
     * This class is used on the client side.
     * It's used to communicate with the host.
     *
     * @param socket                        The connexion socket
     * @throws ServerInitializationFailedException When the server can't be init
     */
    public Client(Socket socket)
            throws ServerInitializationFailedException {

        try {
            mSocket = socket;
            mSocket.setSoTimeout(SOCKET_TIMEOUT);

            mWriteStream = new OutputStreamWriter(mSocket.getOutputStream());
            mReadStream = new InputStreamReader(mSocket.getInputStream());

            mRSAKey = generateRSAKey();

            // Send the public key and wait for the returned key
            String key = sendAndWaitForReturn(createKeyExchangeMessage(getPublicKey()));
            if (key == null) {
                throw new ServerInitializationFailedException();
            }

            Debugger.logColorMessage(DBG_COLOR, "Client", "received: " + key);

            JSONObject keyMessage = null;
            try {
                keyMessage = new JSONObject(key);
            } catch (JSONException e) {
                try {
                    String decryptedKey = decryptMessage(key, getPrivateKey());
                    if (decryptedKey != null) {
                        keyMessage = new JSONObject(decryptedKey);
                    }
                } catch (JSONException f) {
                    e.printStackTrace();
                }
            }

            if (keyMessage != null) {
                handleKeyExchange(keyMessage.getJSONObject("data"));
            } else {
                throw new ServerInitializationFailedException("The received RSA key is not valid !");
            }
        } catch (NoSuchAlgorithmException | BadPaddingException | InvalidKeyException
                | NoSuchPaddingException | IOException | IllegalBlockSizeException e) {

            e.printStackTrace();
            throw new ServerInitializationFailedException();

        }
    }


    /**
     * Function that handles a key exchange.
     * Used when when the user is created.
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
            Debugger.logColorMessage(Debugger.GREEN, "Client", "Key is " + mOtherPublicKey);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }

    }


    /**
     * A synchronized function that send a message and wait for the return value.
     *
     * @param message       The message you want to send
     * @return              The data send by the host
     * @throws IOException  Can be thrown while writing/reading into the fd
     */
    public String sendAndWaitForReturn(String message) throws IOException {

        sendData(mWriteStream, message);

        String data = "";

        try {
            data = readData(mReadStream, "");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;

    }


    /**
     * Function used to send a registration message to the host.
     * This function is blocking and will block until the returned data
     * is received or the timer end.
     *
     * @param password      The user password
     * @param name          The user name
     * @param surname       The user surname
     * @param INE           The user INE (i.e student number)
     * @return              The message returned by the host
     */
    public String sendRegistrationMessage(String password, String name, String surname, String INE) {

        String returnedData = "";

        try {

            returnedData = sendAndWaitForReturn(
                    createRegistrationMessage(
                            getOtherPublicKey(),
                            password,
                            name,
                            surname,
                            INE
                    )
            );

        } catch (IOException e) {
            e.printStackTrace();
        }

        return returnedData;

    }


    /**
     * Function used to send a connection message to the host.
     * Will return the message received from the host.
     *
     * @param INE           The user INE
     * @param password      The user password
     * @return
     */
    public String sendConnectionMessage(String INE, String password) {

        String returnedData = "";

        try {

            returnedData = sendAndWaitForReturn(
                    createConnectionMessage(
                            getOtherPublicKey(),
                            INE,
                            password
                    )
            );

        } catch (IOException e) {
            e.printStackTrace();
        }

        return returnedData;

    }


    /**
     * This function is used when the user want end the communication.
     * Will return the message received from the host.
     *
     * @throws IOException  Can be thrown while closing the socket.
     */
    public void disconnect() throws IOException {

        mWriteStream.close();
        mReadStream.close();
        mSocket.close();

    }


    /**
     * This function is used to create a new ticket.
     * Will return the message received from the host.
     *
     * @param title                 The ticket title
     * @param messageContent        The message contents
     * @param group                 The concerned group
     * @return                      The message received from the host
     */
    public String createANewTicket(String title, String messageContent, String group) {

        String returnedData = "";

        try {

            returnedData = sendAndWaitForReturn(
                    createTicketMessage(
                            getOtherPublicKey(),
                            title,
                            messageContent,
                            group
                    )
            );

        } catch (IOException e) {
            e.printStackTrace();
        }

        return returnedData;

    }


    /**
     * Used to post a message into a ticket.
     * Will return the data retrieved by the host.
     *
     * @param ticketid  The ticket id.
     * @param contents  The contents;
     * @return          The data retrieved by the host.
     */
    public String postAMessage(String ticketid, String contents) {

        String returnedData = "";

        try {

            returnedData = sendAndWaitForReturn(
                    createClassicMessage(
                            getOtherPublicKey(),
                            ticketid,
                            contents
                    )
            );

        } catch (IOException e) {
            e.printStackTrace();
        }

        return returnedData;

    }


    /**
     * This function is used to update the local database
     * from the host database.
     * It will send an "udpateMessage" to the host with
     * the last update date and the host will retrieve
     * all the messages / tickets that are newer than the
     * given date.
     *
     * @return The data retrieved by the server.
     */
    public String updateLocalDatabase() {

        // TODO

        return null;
    }


    /**
     * This function is used to register a new message
     * into the local database and to add it into the UI.
     *
     * @param message   The data received from the host
     */
    public void receiveNewMessage(String message) {

        // TODO

    }

    /**
     * This function is used to update a message both
     * in the ui and in the local database.
     *
     * @param message   The data received from the host
     */
    public void updateMessageData(String message) {

        // TODO

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