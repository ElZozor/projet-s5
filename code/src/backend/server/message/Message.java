package backend.server.message;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Message extends JSONObject {

    public static final String TYPE_KEY_XCHANGE = "keyxchange";
    public static final String TYPE_CONNECTION = "connection";
    public static final String TYPE_TICKET = "ticket";
    public static final String TYPE_MESSAGE = "message";
    public static final String TYPE_RESPONSE = "response";
    public static final String TYPE_UPDATE = "update";


    public static final String CONNECTION_INE = "ine";
    public static final String CONNECTION_PASSWORD = "password";

    public static final String TICKET_TITLE = "title";
    public static final String TICKET_MESSAGE = "message";
    public static final String TICKET_GROUP = "group";

    public static final String MESSAGE_TICKET_ID = "ticketid";
    public static final String MESSAGE_CONTENTS = "contents";

    public static final String RESPONSE_VALUE = "value";
    public static final String RESPONSE_SUCCESS = "success";
    public static final String RESPONSE_ERROR = "error";
    public static final String RESPONSE_REASON = "reason";

    public static final String UPDATE_CONTENTS = "contents";

    public static final String TYPE = "type";
    public static final String DATA = "data";


    private final MESSAGE_TYPE message_type;
    private final String type;
    PublicKey encodeKey;
    PrivateKey decodeKey;
    private JSONObject data = new JSONObject();

    private Message(MESSAGE_TYPE msg_type, final String type, PublicKey encodeKey) {
        this.message_type = msg_type;
        this.type = type;
        this.encodeKey = encodeKey;

        put(TYPE, type);
    }

    public Message(String data, PublicKey encodeKey, PrivateKey decodeKey) throws InvalidMessageException {
        this.encodeKey = encodeKey;
        this.decodeKey = decodeKey;

        data = decode(data);

        if (data == null || !isValidJSON(data)) {
            throw new InvalidMessageException("Data cannot be decoded or JSON is invalid");
        }

        JSONObject decoded = new JSONObject(data);

        if (!isValid(decoded)) {
            throw new InvalidMessageException("Trying to create a Message Object with invalid data");
        }

        this.type = decoded.getString(TYPE);
        this.data = decoded.getJSONObject(DATA);

        message_type = guessType();
    }

    public static Message createNackMessage(final String reason, PublicKey encodeKey) {
        Message result = new Message(MESSAGE_TYPE.RESPONSE, TYPE_RESPONSE, encodeKey);

        result.addData(RESPONSE_VALUE, RESPONSE_ERROR);
        result.addData(RESPONSE_REASON, reason);

        return result;
    }

    public static Message createAckMessage(PublicKey encodeKey) {
        Message result = new Message(MESSAGE_TYPE.RESPONSE, TYPE_RESPONSE, encodeKey);

        result.addData(RESPONSE_VALUE, RESPONSE_SUCCESS);

        return result;
    }

    public static String createKeyXChangeMessage(PublicKey encodeKey) {
        return new String(Base64.getEncoder().encode(encodeKey.getEncoded())) + "\n";
    }

    public static PublicKey getKeyXChangePublicKey(String message) {
        PublicKey otherPublicKey = null;

        try {

            byte[] result = Base64.getDecoder().decode(message);

            KeyFactory factory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(result);
            otherPublicKey = factory.generatePublic(encodedKeySpec);


        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return otherPublicKey;
    }

    public static Message createConnectionMessage(final String ine, final String password, PublicKey encodeKey) {
        Message message = new Message(MESSAGE_TYPE.CONNECTION, TYPE_CONNECTION, encodeKey);

        message.addData(CONNECTION_INE, ine);
        message.addData(CONNECTION_PASSWORD, password);

        return message;
    }

    public static Message createTicketMessage(final String ticketTitle, final String ticketGroup,
                                              final String contents, PublicKey encodeKey) {

        Message message = new Message(MESSAGE_TYPE.TICKET, TYPE_TICKET, encodeKey);

        message.addData(TICKET_TITLE, ticketTitle);
        message.addData(TICKET_GROUP, ticketGroup);
        message.addData(TICKET_MESSAGE, contents);

        return message;
    }

    public static Message createMessageMessage(final String ticketID, final String contents, PublicKey encodeKey) {
        Message message = new Message(MESSAGE_TYPE.MESSAGE, TYPE_MESSAGE, encodeKey);

        message.addData(MESSAGE_TICKET_ID, ticketID);
        message.addData(MESSAGE_CONTENTS, contents);

        return message;
    }

    public static Message createUpdateMessage(final String contents, PublicKey encodeKey) {
        Message message = new Message(MESSAGE_TYPE.MESSAGE, TYPE_MESSAGE, encodeKey);

        message.addData(UPDATE_CONTENTS, contents);

        return message;
    }

    private Boolean isValidJSON(String data) {
        try {
            new JSONObject(data);
        } catch (JSONException e) {
            try {
                new JSONArray(data);
            } catch (JSONException f) {
                return false;
            }
        }


        return true;
    }

    private String decode(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(data);

            System.out.println(bytes);

            Cipher decryptCypher = Cipher.getInstance("RSA");
            decryptCypher.init(Cipher.DECRYPT_MODE, decodeKey);

            return new String(decryptCypher.doFinal(bytes), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException
                | BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String encode() {

        try {

            Cipher encryptCypher = Cipher.getInstance("RSA");
            encryptCypher.init(Cipher.ENCRYPT_MODE, encodeKey);

            byte[] cipher = encryptCypher.doFinal(toString().getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(cipher) + '\n';

        } catch (NoSuchPaddingException | NoSuchAlgorithmException
                | InvalidKeyException | BadPaddingException
                | IllegalBlockSizeException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addData(String key, String data) {
        this.data.put(key, data);
    }

    private void addData(String key, byte[] data) {
        this.data.put(key, data);
    }

    public Boolean isValid(final JSONObject data) {
        return data.has(TYPE) && data.has(DATA);
    }

    private MESSAGE_TYPE guessType() throws InvalidMessageException {
        switch (this.type) {
            case TYPE_KEY_XCHANGE:
                return MESSAGE_TYPE.KEYXCHANGE;

            case TYPE_CONNECTION:
                checkForConnectionValidity();
                return MESSAGE_TYPE.CONNECTION;

            case TYPE_MESSAGE:
                return MESSAGE_TYPE.MESSAGE;

            case TYPE_RESPONSE:
                return MESSAGE_TYPE.RESPONSE;

            case TYPE_TICKET:
                return MESSAGE_TYPE.TICKET;

            case TYPE_UPDATE:
                return MESSAGE_TYPE.UPDATE;

            default:
                throw new InvalidMessageException("Message with invalid type: " + this.type);
        }
    }

    private void checkForConnectionValidity() throws InvalidMessageException {
        if (!data.has(CONNECTION_INE) || !data.has(CONNECTION_PASSWORD)) {
            throw new InvalidMessageException("Missing field in connection Message");
        }
    }

    private void checkForMessageValidity() throws InvalidMessageException {
        if (!data.has(MESSAGE_TICKET_ID) || !data.has(MESSAGE_CONTENTS)) {
            throw new InvalidMessageException("Missing field in message type Message");
        }
    }

    private void checkForResponseValidity() throws InvalidMessageException {
        if (!data.has(RESPONSE_VALUE)) {
            throw new InvalidMessageException("Missing field in response Message");
        }

        if (data.getString(RESPONSE_VALUE).equals(RESPONSE_ERROR) && !data.has(RESPONSE_REASON)) {
            throw new InvalidMessageException("Missing reason in response Message");
        }
    }

    private void checkForTicketValidity() throws InvalidMessageException {
        if (!data.has(TICKET_TITLE) || !data.has(TICKET_GROUP) || !data.has(TICKET_MESSAGE)) {
            throw new InvalidMessageException("Missing field in ticket Message");
        }
    }

    private void checkForUpdateValidity() throws InvalidMessageException {
        if (!data.has(UPDATE_CONTENTS)) {
            throw new InvalidMessageException("Missing field in update message");
        }
    }

    public Boolean isAck() {
        return message_type.equals(MESSAGE_TYPE.RESPONSE) && data.getString(RESPONSE_VALUE).equals(RESPONSE_SUCCESS);
    }

    public Boolean isNack() {
        return message_type.equals(MESSAGE_TYPE.RESPONSE) && data.getString(RESPONSE_VALUE).equals(RESPONSE_ERROR);
    }

    public Boolean isUpdate() {
        return message_type.equals(MESSAGE_TYPE.UPDATE);
    }

    public Boolean isKeyXChange() {
        return message_type.equals(MESSAGE_TYPE.KEYXCHANGE);
    }

    public Boolean isConnection() {
        return message_type.equals(MESSAGE_TYPE.CONNECTION);
    }

    public Boolean isTicket() {
        return message_type.equals(MESSAGE_TYPE.TICKET);
    }

    public Boolean isMessage() {
        return message_type.equals(MESSAGE_TYPE.MESSAGE);
    }

    public MESSAGE_TYPE getType() {
        return message_type;
    }


    public String getNackReason() throws WrongMessageTypeException {
        if (!isNack()) {
            throw new WrongMessageTypeException("Requiring Nack reason on a non-nack message");
        }

        return data.getString(RESPONSE_REASON);
    }


    public String getConnectionINE() {
        return data.getString(CONNECTION_INE);
    }

    public String getConnectionPassword() {
        return data.getString(CONNECTION_PASSWORD);
    }

    public String getTicketTitle() {
        return data.getString(TICKET_TITLE);
    }

    public String getTicketGroup() {
        return data.getString(TICKET_GROUP);
    }

    public String getTicketMessage() {
        return data.getString(TICKET_MESSAGE);
    }

    public String getMessageContents() {
        return data.getString(MESSAGE_CONTENTS);
    }

    public Long getMessageTicketID() {
        return data.getLong(MESSAGE_TICKET_ID);
    }

    public String getUpdateContents() {
        return data.getString(UPDATE_CONTENTS);
    }


    public String toString() {
        JSONObject result = new JSONObject();
        result.put(TYPE, type);
        result.put(DATA, data);

        return result.toString();
    }


    public static class InvalidMessageException extends Exception {
        public InvalidMessageException() {
            super();
        }

        public InvalidMessageException(String message) {
            super(message);
        }
    }

    public static class WrongMessageTypeException extends Exception {
        public WrongMessageTypeException() {
            super();
        }

        public WrongMessageTypeException(String message) {
            super(message);
        }
    }

}
