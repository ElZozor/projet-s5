package backend.server.client;


import backend.server.Server;
import debug.Debugger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class Client implements Server {

    private final static String DBG_COLOR = Debugger.YELLOW;

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
     * @throws NoSuchAlgorithmException     Can be thrown while generating RSA keys
     * @throws IOException                  Can be thrown while creating file descriptors
     */
    public Client(Socket socket) throws NoSuchAlgorithmException, IOException {
        mSocket = socket;
        mWriteStream = new OutputStreamWriter(mSocket.getOutputStream());
        mReadStream = new InputStreamReader(mSocket.getInputStream());

        mRSAKey = generateRSAKey();

        // Send the public key
        sendData(mWriteStream, createKeyExchangeMessage(getPublicKey()));

        //Receive and parse the host public key
        String key = readDataBlocking(mReadStream);
        Debugger.logColorMessage(DBG_COLOR, "Client", "received: " + key);

        try {
            JSONObject keyMessage = new JSONObject(key);
            handleKeyExchange(keyMessage.getJSONObject("data"));
        } catch (JSONException e) {
            JSONObject keyMessage = new JSONObject(decryptMessage(key, getPrivateKey()));
            handleKeyExchange(keyMessage);
        }
    }


    /**
     * Used to generate and send a classic message
     * @param id            The user id
     * @param ticketid      The ticket id
     * @param contents      The message contents
     * @throws IOException  Can be thrown while sending the message
     */
    public void sendClassicMessage(String id, String ticketid, String contents) throws IOException {
        String message = createClassicMessage(getOtherPublicKey(), id, ticketid, contents);
        Debugger.logColorMessage(DBG_COLOR, "Client", "Send following msg: " + message);

        sendData(mWriteStream, message);
    }


    /**
     * Used to send a registration message which is used
     * to register a new user into the host database.
     *
     * @param id            The user id
     * @param password      The user password
     * @param name          The user name
     * @param surname       The user surname
     * @throws IOException  Can be thrown while sending the messsage
     */
    public void sendRegistrationMessage(String id, String password, String name, String surname) throws IOException {
        String message = createRegistrationMessage(getOtherPublicKey(), id, password, name, surname);
        sendData(mWriteStream, message);
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