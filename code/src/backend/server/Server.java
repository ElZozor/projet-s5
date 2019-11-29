package backend.server;

import jdk.internal.jline.internal.Nullable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import sun.nio.cs.UTF_8;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.*;
import java.util.Base64;
import java.util.Random;


public interface Server {

    // Theses are all the variables that will be used by both client
    // and server to create and receive messages

    String TYPE_KEY_XCHANGE         = "keyxchange";
    String TYPE_CONNECTION          = "connection";
    String TYPE_REGISTRATION        = "registration";
    String TYPE_TICKET              = "ticket";
    String TYPE_MESSAGE             = "message";
    String TYPE_RESPONSE            = "response";

    String TYPE_DATA                = "data";

    String CONNECTION_ID            = "id";
    String CONNECTION_PASSWORD      = "password";

    String REGISTRATION_ID          = "id";
    String REGISTRATION_PASSWORD    = "password";
    String REGISTRATION_NAME        = "name";
    String REGISTRATION_SURNAME     = "surname";

    String TICKET_ID                = "id";
    String TICKET_TITLE             = "title";
    String TICKET_MESSAGE           = "name";
    String TICKET_GROUPS            = "surname";

    String MESSAGE_ID               = "id";
    String MESSAGE_TICKET_ID        = "ticketid";
    String MESSAGE_CONTENTS         = "contents";

    String RESPONSE_VALUE           = "value";
    String RESPONSE_SUCCESS         = "success";
    String RESPONSE_ERROR           = "error";

    int BUFFER_SIZE = 256;



    @Nullable
    PrivateKey getPrivateKey();

    @Nullable
    PublicKey getPublicKey();

    @Nullable
    PublicKey getOtherPublicKey();


    /**
     * Encrypt a message via a public key.
     *
     * @param data      The message
     * @param pk        The public key to encrypt data
     * @return          The encrypted message as a String
     */
    @Nullable
    static String encryptMessage(String data, PublicKey pk) {
        //TODO Complete this
        if (pk == null) {
            return null;
        }

        try {

            Cipher encryptCypher = Cipher.getInstance("RSA");
            encryptCypher.init(Cipher.ENCRYPT_MODE, pk);

            byte[] cipher = encryptCypher.doFinal(data.getBytes());

            return Base64.getEncoder().encodeToString(cipher);

        } catch (NoSuchPaddingException | NoSuchAlgorithmException
                | InvalidKeyException | BadPaddingException
                | IllegalBlockSizeException e) {
            e.printStackTrace();

            return null;
        }
    }


    /**
     * Decrypt a message via a private key.
     *
     * @param data      The message
     * @param pk        The private key to encrypt data
     * @return          The decrypted message as a String
     */
    @Nullable
    static String decryptMessage(String data, PrivateKey pk) {
        //TODO Complete this
        if (pk == null) {
            return null;
        }

        try {

            byte[] bytes = Base64.getDecoder().decode(data);

            Cipher decryptCypher = Cipher.getInstance("RSA");
            decryptCypher.init(Cipher.DECRYPT_MODE, pk);

            String decipher = new String(decryptCypher.doFinal(data.getBytes()));

            return decipher;

        } catch (NoSuchPaddingException | NoSuchAlgorithmException
                | InvalidKeyException | BadPaddingException
                | IllegalBlockSizeException e) {
            e.printStackTrace();

            return null;
        }
    }


    /**
     * Create a key exchange message that will be used
     * to send public key between the server and the client
     *
     * @param pk    The public key
     * @return      The message
     */
    static String createKeyExchangeMessage(PublicKey pk) {
        JSONObject keyExchangeMessage = new JSONObject();
        keyExchangeMessage.put("type", TYPE_KEY_XCHANGE);

        JSONObject key = new JSONObject();
        key.put("key", pk.getEncoded());

        keyExchangeMessage.put("data", key);

        return keyExchangeMessage.toString();
    }


    /**
     * Create a standardized connection message that's signed and valid.
     * If the token is not defined, the result of this function will be null.
     *
     * @param pk        The public key that will be used to sign the message
     * @param id        The user id
     * @param password  The user password
     * @return          The signed message or null
     */
    @Nullable
    static String createConnectionMessage(PublicKey pk, String id, String password) {
        JSONObject connectionMessage = new JSONObject();
        connectionMessage.put("type", TYPE_CONNECTION);

        JSONObject data = new JSONObject();
        data.put(CONNECTION_ID, id);
        data.put(CONNECTION_PASSWORD, password);

        connectionMessage.put(TYPE_DATA, data);

        return encryptMessage(connectionMessage.toString(), pk);
    }


    /**
     * Create a standardized registration message that's signed and valid.
     * If the token is not defined, the result of this function will be null.
     *
     * @param pk        The public key that will be used to sign the message
     * @param id        The user id
     * @param password  The user password
     * @param name      The user name
     * @param surname   The user surname
     * @return          The signed message or null
     */
    @Nullable
    static String createRegistrationMessage(PublicKey pk, String id, String password, String name, String surname) {
        JSONObject registrationMessage = new JSONObject();
        registrationMessage.put("type", TYPE_REGISTRATION);

        JSONObject data = new JSONObject();
        data.put(REGISTRATION_ID, id);
        data.put(REGISTRATION_PASSWORD, password);
        data.put(REGISTRATION_NAME, name);
        data.put(REGISTRATION_SURNAME, surname);

        registrationMessage.put(TYPE_DATA, data);

        return encryptMessage(registrationMessage.toString(), pk);
    }


    /**
     * Create a standardized ticket message that's signed and valid.
     * If the token is not defined, the result of this function will be null.
     *
     * @param pk        The public key that will be used to sign the message
     * @param id        The user id
     * @param title     The ticket title
     * @param message   The ticket message
     * @param groups    The ticket groups
     *
     * @return          The signed message or null
     */
    @Nullable
    static String createTicketMessage(PublicKey pk, String id, String title, String message, String groups) {
        JSONObject registrationMessage = new JSONObject();
        registrationMessage.put("type", TYPE_REGISTRATION);

        JSONObject data = new JSONObject();
        data.put(TICKET_ID, id);
        data.put(TICKET_TITLE, title);
        data.put(TICKET_MESSAGE, message);
        data.put(TICKET_GROUPS, groups);

        registrationMessage.put(TYPE_DATA, data);

        return encryptMessage(registrationMessage.toString(), pk);
    }


    /**
     * Create a standardized message that's signed and valid.
     * If the token is not defined, the result of this function will be null.
     *
     * @param pk        The public key that will be used to sign the message
     * @param id        The user id
     * @param ticketid  The ticket title
     * @param contents  The message contents
     *
     * @return          The signed message or null
     */
    @Nullable
    static String createClassicMessage(PublicKey pk, String id, String ticketid, String contents) {
        JSONObject registrationMessage = new JSONObject();
        registrationMessage.put("type", TYPE_REGISTRATION);

        JSONObject data = new JSONObject();
        data.put(MESSAGE_ID, id);
        data.put(MESSAGE_TICKET_ID, ticketid);
        data.put(MESSAGE_CONTENTS, contents);

        registrationMessage.put(TYPE_DATA, data);

        return encryptMessage(registrationMessage.toString(), pk);
    }




    /**
     * Generate a random token which has a length of TOKEN_SIZE
     *
     * @return The random token
     */
    @NotNull
    static KeyPair generateRSAKey() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());

        return generator.generateKeyPair();
    }


    /**
     * Used to send a response message.
     * It's mostly used by the host.
     *
     * @param pk
     * @param socketOutputStream
     * @param success
     * @throws IOException
     */
    static void sendResponseMessage(PublicKey pk, OutputStreamWriter socketOutputStream, Boolean success) throws IOException {
        JSONObject response = new JSONObject();
        response.put("type", TYPE_RESPONSE);

        JSONObject data = new JSONObject();

        if (success) {
            data.put(RESPONSE_VALUE, RESPONSE_SUCCESS);
        } else {
            data.put(RESPONSE_VALUE, RESPONSE_ERROR);
        }

        response.put("data", data);

        socketOutputStream.write(encryptMessage(response.toString(), pk));
        socketOutputStream.flush();
    }


    /**
     * Used to send data through a socket.
     * @param socketWriter  The socket output stream
     * @param data          The data to send
     * @throws IOException  Exception if write has failed
     */
    default void sendData(OutputStreamWriter socketWriter, String data) throws IOException {
        socketWriter.write(data);
        socketWriter.flush();
    }


    /**
     * Used to receive data from a socket.
     *
     * @param socketReader  The socket input reader
     * @return              The data
     * @throws IOException  Exception if read has failed
     */
    default String readData(InputStreamReader socketReader) throws IOException {
        int nChar = BUFFER_SIZE;
        char[] buffer = new char[BUFFER_SIZE];

        StringBuilder builder = new StringBuilder();

        while (socketReader.ready() && nChar == BUFFER_SIZE) {
            nChar = socketReader.read(buffer, 0, 256);

            builder.append(buffer);
        }

        return builder.toString();
    }



}
