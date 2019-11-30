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


    public Client(Socket socket) throws NoSuchAlgorithmException, IOException {
        mSocket = socket;
        mWriteStream = new OutputStreamWriter(mSocket.getOutputStream());
        mReadStream = new InputStreamReader(mSocket.getInputStream());

        mRSAKey = generateRSAKey();

        // Send the public key
        sendData(mWriteStream, createKeyExchangeMessage(getPublicKey()));

        //Receive and parse the host public key
        String key = readDataBlocking(mReadStream);
        Debugger.logColorMessage(DBG_COLOR, "Client", "received: " + key.substring(0, Math.min(100, key.length())));

        try {
            JSONObject keyMessage = new JSONObject(key);
            handleKeyExchange(keyMessage.getJSONObject("data"));
        } catch (JSONException e) {
            JSONObject keyMessage = new JSONObject(decryptMessage(key, getPrivateKey()));
            handleKeyExchange(keyMessage);
        }
    }

    public void sendClassicMessage(String id, String ticketid, String contents) throws IOException {
        String message = createClassicMessage(getOtherPublicKey(), id, ticketid, contents);
        Debugger.logColorMessage(DBG_COLOR, "Client", "Send following msg: " + message);

        sendData(mWriteStream, message);
    }


    public void sendRegistrationMessage(String id, String password, String name, String surname) throws IOException {
        String message = createRegistrationMessage(getOtherPublicKey(), id, password, name, surname);
        sendData(mWriteStream, message);
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