package backend.server.communication;

import backend.data.Groupe;
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
import java.util.Date;
import java.util.TreeSet;

public class CommunicationMessage extends JSONObject {

    public static final String TYPE_KEY_XCHANGE = "keyxchange";
    public static final String TYPE_CONNECTION = "connection";
    public static final String TYPE_TICKET = "ticket";
    public static final String TYPE_MESSAGE = "message";
    public static final String TYPE_RESPONSE = "response";
    public static final String TYPE_UPDATE = "update";
    public static final String TYPE_LOCAL_UPDATE = "localupdate";
    public static final String TYPE_LOCAL_UPDATE_RESPONSE = "localupdateresponse";


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

    public static final String LOCAL_UPDATE_DATE = "contents";
    public static final String LOCAL_UPDATE_RESPONSE = "groups";

    public static final String TYPE = "type";
    public static final String DATA = "data";


    private final COMMUNICATION_TYPE communication_type;
    private final String type;
    PublicKey encodeKey;
    PrivateKey decodeKey;
    private JSONObject data = new JSONObject();

    private CommunicationMessage(COMMUNICATION_TYPE msg_type, final String type, PublicKey encodeKey) {
        this.communication_type = msg_type;
        this.type = type;
        this.encodeKey = encodeKey;

        put(TYPE, type);
    }

    public CommunicationMessage(String data, PublicKey encodeKey, PrivateKey decodeKey) throws InvalidMessageException {
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

        communication_type = guessType();
    }

    public static CommunicationMessage createNack(final String reason, PublicKey encodeKey) {
        CommunicationMessage result = new CommunicationMessage(COMMUNICATION_TYPE.RESPONSE, TYPE_RESPONSE, encodeKey);

        result.addData(RESPONSE_VALUE, RESPONSE_ERROR);
        result.addData(RESPONSE_REASON, reason);

        return result;
    }

    public static CommunicationMessage createAck(PublicKey encodeKey) {
        CommunicationMessage result = new CommunicationMessage(COMMUNICATION_TYPE.RESPONSE, TYPE_RESPONSE, encodeKey);

        result.addData(RESPONSE_VALUE, RESPONSE_SUCCESS);

        return result;
    }

    public static String createKeyXChange(PublicKey encodeKey) {
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

    public static CommunicationMessage createConnection(final String ine, final String password, PublicKey encodeKey) {
        CommunicationMessage communicationMessage = new CommunicationMessage(COMMUNICATION_TYPE.CONNECTION, TYPE_CONNECTION, encodeKey);

        communicationMessage.addData(CONNECTION_INE, ine);
        communicationMessage.addData(CONNECTION_PASSWORD, password);

        return communicationMessage;
    }

    public static CommunicationMessage createTicket(final String ticketTitle, final String ticketGroup,
                                                    final String contents, PublicKey encodeKey) {

        CommunicationMessage communicationMessage = new CommunicationMessage(COMMUNICATION_TYPE.TICKET, TYPE_TICKET, encodeKey);

        communicationMessage.addData(TICKET_TITLE, ticketTitle);
        communicationMessage.addData(TICKET_GROUP, ticketGroup);
        communicationMessage.addData(TICKET_MESSAGE, contents);

        return communicationMessage;
    }

    public static CommunicationMessage createMessage(final String ticketID, final String contents, PublicKey encodeKey) {
        CommunicationMessage communicationMessage = new CommunicationMessage(COMMUNICATION_TYPE.MESSAGE, TYPE_MESSAGE, encodeKey);

        communicationMessage.addData(MESSAGE_TICKET_ID, ticketID);
        communicationMessage.addData(MESSAGE_CONTENTS, contents);

        return communicationMessage;
    }

    public static CommunicationMessage createUpdate(final String contents, PublicKey encodeKey) {
        CommunicationMessage communicationMessage = new CommunicationMessage(COMMUNICATION_TYPE.UPDATE, TYPE_UPDATE, encodeKey);

        communicationMessage.addData(UPDATE_CONTENTS, contents);

        return communicationMessage;
    }

    public static CommunicationMessage createLocalUpdate(final Date from, PublicKey encodeKey) {
        CommunicationMessage communicationMessage = new CommunicationMessage(COMMUNICATION_TYPE.LOCAL_UPDATE, TYPE_LOCAL_UPDATE, encodeKey);

        communicationMessage.addData(LOCAL_UPDATE_DATE, Long.toString(from.getTime()));

        return communicationMessage;
    }

    public static CommunicationMessage createLocalUpdateResponse(TreeSet<Groupe> groups, PublicKey encodeKey) {
        CommunicationMessage communicationMessage = new CommunicationMessage(COMMUNICATION_TYPE.LOCAL_UPDATE_RESPONSE, TYPE_LOCAL_UPDATE_RESPONSE, encodeKey);

        JSONArray array = new JSONArray();
        for (Groupe group : groups) {
            array.put(group.toJSON());
        }

        communicationMessage.addData(LOCAL_UPDATE_RESPONSE, array.toString());

        return communicationMessage;
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

    private COMMUNICATION_TYPE guessType() throws InvalidMessageException {
        switch (this.type) {
            case TYPE_KEY_XCHANGE:
                return COMMUNICATION_TYPE.KEYXCHANGE;

            case TYPE_CONNECTION:
                checkForConnectionValidity();
                return COMMUNICATION_TYPE.CONNECTION;

            case TYPE_MESSAGE:
                checkForMessageValidity();
                return COMMUNICATION_TYPE.MESSAGE;

            case TYPE_RESPONSE:
                checkForResponseValidity();
                return COMMUNICATION_TYPE.RESPONSE;

            case TYPE_TICKET:
                checkForTicketValidity();
                return COMMUNICATION_TYPE.TICKET;

            case TYPE_UPDATE:
                checkForUpdateValidity();
                return COMMUNICATION_TYPE.UPDATE;

            case TYPE_LOCAL_UPDATE:
                checkForUpdateValidity();
                return COMMUNICATION_TYPE.LOCAL_UPDATE;

            case TYPE_LOCAL_UPDATE_RESPONSE:
                checkForLocalUpdateResponseValidity();
                return COMMUNICATION_TYPE.LOCAL_UPDATE_RESPONSE;

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

    private void checkForLocalUpdateValidity() throws InvalidMessageException {
        if (!data.has(LOCAL_UPDATE_DATE)) {
            throw new InvalidMessageException("Missing field in update message");
        }
    }

    private void checkForLocalUpdateResponseValidity() throws InvalidMessageException {
        if (!data.has(LOCAL_UPDATE_RESPONSE)) {
            throw new InvalidMessageException("Missing field in update message");
        }
    }

    public Boolean isAck() {
        return communication_type.equals(COMMUNICATION_TYPE.RESPONSE) && data.getString(RESPONSE_VALUE).equals(RESPONSE_SUCCESS);
    }

    public Boolean isNack() {
        return communication_type.equals(COMMUNICATION_TYPE.RESPONSE) && data.getString(RESPONSE_VALUE).equals(RESPONSE_ERROR);
    }

    public Boolean isUpdate() {
        return communication_type.equals(COMMUNICATION_TYPE.UPDATE);
    }

    public Boolean isKeyXChange() {
        return communication_type.equals(COMMUNICATION_TYPE.KEYXCHANGE);
    }

    public Boolean isConnection() {
        return communication_type.equals(COMMUNICATION_TYPE.CONNECTION);
    }

    public Boolean isTicket() {
        return communication_type.equals(COMMUNICATION_TYPE.TICKET);
    }

    public Boolean isMessage() {
        return communication_type.equals(COMMUNICATION_TYPE.MESSAGE);
    }

    public Boolean isLocalUpdate() {
        return communication_type.equals(COMMUNICATION_TYPE.LOCAL_UPDATE);
    }

    public Boolean isLocalUpdateResponse() {
        return communication_type.equals(COMMUNICATION_TYPE.LOCAL_UPDATE_RESPONSE);
    }

    public COMMUNICATION_TYPE getType() {
        return communication_type;
    }


    public JSONObject getAckData() throws WrongMessageTypeException {
        if (!isAck()) {
            throw new WrongMessageTypeException("Requiring ack data on a non-ack message");
        }

        return new JSONObject(data);
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

    public Date getLocalUpdateDate() {
        return new Date(data.getLong(LOCAL_UPDATE_DATE));
    }

    public JSONArray getLocalUpdateResponseGroups() {
        return data.getJSONArray(LOCAL_UPDATE_RESPONSE);
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
