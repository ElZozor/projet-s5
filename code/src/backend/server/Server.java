package backend.server;

import debug.Debugger;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.SocketTimeoutException;
import java.security.*;
import java.util.Base64;


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

    String CONNECTION_INE           = "ine";
    String CONNECTION_PASSWORD      = "password";

    String REGISTRATION_INE         = "ine";
    String REGISTRATION_PASSWORD    = "password";
    String REGISTRATION_NAME        = "name";
    String REGISTRATION_SURNAME     = "surname";

    String TICKET_ID                = "id";
    String TICKET_TITLE             = "title";
    String TICKET_MESSAGE           = "message";
    String TICKET_GROUP             = "group";

    String MESSAGE_ID               = "id";
    String MESSAGE_TICKET_ID        = "ticketid";
    String MESSAGE_CONTENTS         = "contents";

    String RESPONSE_VALUE           = "value";
    String RESPONSE_SUCCESS         = "success";
    String RESPONSE_ERROR           = "error";
    String RESPONSE_REASON          = "reason";

    int BUFFER_SIZE = 256;


    PrivateKey getPrivateKey();

    PublicKey getPublicKey();

    PublicKey getOtherPublicKey();


    /**
     * Encrypt a message via a public key.
     *
     * @param data      The message
     * @param pk        The public key to encrypt data
     * @return          The encrypted message as a String
     */
    default String encryptMessage(String data, PublicKey pk) {
        if (pk == null) {
            Debugger.logMessage("Server encryptMessage", "Public Key is null !");
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
    default String decryptMessage(String data, PrivateKey pk)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        if (pk == null) {
            return null;
        }

        if (data == null || data.isEmpty()) {
            return null;
        }

        byte[] bytes = Base64.getDecoder().decode(data);

        Cipher decryptCypher = Cipher.getInstance("RSA");
        decryptCypher.init(Cipher.DECRYPT_MODE, pk);

        return new String(decryptCypher.doFinal(bytes));
    }


    /**
     * Create a key exchange message that will be used
     * to send public key between the server and the client
     *
     * @param pk    The public key
     * @return      The message
     */
    default String createKeyExchangeMessage(PublicKey pk) {
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
     * @param ine       The user INE
     * @param password  The user password
     * @return          The signed message or null
     */
    default String createConnectionMessage(PublicKey pk, String ine, String password) {
        JSONObject connectionMessage = new JSONObject();
        connectionMessage.put("type", TYPE_CONNECTION);

        JSONObject data = new JSONObject();
        data.put(CONNECTION_INE, ine);
        data.put(CONNECTION_PASSWORD, password);

        connectionMessage.put(TYPE_DATA, data);

        return encryptMessage(connectionMessage.toString(), pk);
    }


    /**
     * Create a standardized registration message that's signed and valid.
     * If the token is not defined, the result of this function will be null.
     *
     * @param pk        The public key that will be used to sign the message
     * @param password  The user password
     * @param name      The user name
     * @param surname   The user surname
     * @param INE       The user INE (i.e student number)
     * @return          The signed message or null
     */
    default String createRegistrationMessage(PublicKey pk, String password, String name, String surname, String INE) {
        JSONObject registrationMessage = new JSONObject();
        registrationMessage.put("type", TYPE_REGISTRATION);

        JSONObject data = new JSONObject();
        data.put(REGISTRATION_PASSWORD, password);
        data.put(REGISTRATION_NAME, name);
        data.put(REGISTRATION_SURNAME, surname);
        data.put(REGISTRATION_INE, INE);

        registrationMessage.put(TYPE_DATA, data);

        return encryptMessage(registrationMessage.toString(), pk);
    }


    /**
     * Create a standardized ticket message that's signed and valid.
     * If the token is not defined, the result of this function will be null.
     *
     * @param pk        The public key that will be used to sign the message
     * @param title     The ticket title
     * @param message   The ticket message
     * @param groups    The ticket groups
     *
     * @return          The signed message or null
     */
    default String createTicketMessage(PublicKey pk, String title, String message, String groups) {
        JSONObject registrationMessage = new JSONObject();
        registrationMessage.put("type", TYPE_REGISTRATION);

        JSONObject data = new JSONObject();
        data.put(TICKET_TITLE, title);
        data.put(TICKET_MESSAGE, message);
        data.put(TICKET_GROUP, groups);

        registrationMessage.put(TYPE_DATA, data);

        return encryptMessage(registrationMessage.toString(), pk);
    }


    /**
     * Create a standardized message that's signed and valid.
     * If the token is not defined, the result of this function will be null.
     *
     * @param pk        The public key that will be used to sign the message
     * @param ticketid  The ticket title
     * @param contents  The message contents
     *
     * @return          The signed message or null
     */
    default String createClassicMessage(PublicKey pk, String ticketid, String contents) {
        Debugger.logMessage("Server", "create classic message called");
        JSONObject registrationMessage = new JSONObject();
        registrationMessage.put("type", TYPE_MESSAGE);

        JSONObject data = new JSONObject();
        data.put(MESSAGE_TICKET_ID, ticketid);
        data.put(MESSAGE_CONTENTS, contents);

        registrationMessage.put(TYPE_DATA, data);

        Debugger.logMessage(
                "Server",
                "Message is: " + registrationMessage
        );

        return encryptMessage(registrationMessage.toString(), pk);
    }




    /**
     * Generate a random token which has a length of TOKEN_SIZE
     *
     * @return The random token
     */
    default KeyPair generateRSAKey() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());

        return generator.generateKeyPair();
    }


    /**
     * Used to send an Ack response ( query has succeed )
     *
     * @param pk                    The public key to encrypt the data
     * @param socketOutputStream    The output stream for the data
     * @throws IOException          Can be thrown while writing into the stream
     */
    default void sendAckMessage(PublicKey pk, OutputStreamWriter socketOutputStream) throws IOException {
        JSONObject response = new JSONObject();
        response.put("type", TYPE_RESPONSE);

        JSONObject data = new JSONObject();
        data.put(RESPONSE_VALUE, RESPONSE_SUCCESS);

        response.put(TYPE_DATA, data);

        socketOutputStream.write(encryptMessage(response.toString(), pk));
        socketOutputStream.flush();
    }


    /**
     * Used to send a Nack response ( query has failed )
     *
     * @param pk                    The public key to encrypt the data
     * @param socketOutputStream    The ouput stream for the data
     * @param reason                The reason why the query has failed
     * @throws IOException          Can be thrown while writing into the stream
     */
    default void sendNackMessage(PublicKey pk, OutputStreamWriter socketOutputStream, String reason) throws IOException {

        JSONObject response = new JSONObject();
        response.put("type", TYPE_RESPONSE);

        JSONObject data = new JSONObject();
        data.put(RESPONSE_VALUE, RESPONSE_ERROR);
        data.put(RESPONSE_REASON, reason);

        response.put(TYPE_DATA, data);

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
        Debugger.logMessage("Server sendData", "Sending following data: " + data);
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
    default String readData(InputStreamReader socketReader, String defaultReturnValue) throws IOException {
        int nChar = 0;
        char[] buffer = new char[BUFFER_SIZE];

        StringBuilder builder = new StringBuilder();

        do {
            try {
                nChar = socketReader.read(buffer, 0, BUFFER_SIZE);
                if (nChar < 0) {
                    nChar = 0;
                }

                for (int i = 0; i < nChar; ++i) {
                    builder.append(buffer[i]);
                }


            } catch (SocketTimeoutException e) {
                System.err.println("Socket timeout detected, aborting...");
            }
        } while (nChar == BUFFER_SIZE && socketReader.ready());


        if (builder.length() == 0) {
            return defaultReturnValue;
        }

        return builder.toString();
    }



    class ServerInitializationFailedException extends Exception {
        public ServerInitializationFailedException() {
            super();
        }

        public ServerInitializationFailedException(String message) {
            super(message);
        }
    }


}
