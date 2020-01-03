package backend.server.communication;

import backend.data.Groupe;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private JSONObject data = new JSONObject();

    private CommunicationMessage(COMMUNICATION_TYPE msg_type, final String type) {
        this.communication_type = msg_type;
        this.type = type;

        put(TYPE, type);
    }

    public CommunicationMessage(String data) throws InvalidMessageException {

        if (data == null || !isValidJSON(data)) {
            throw new InvalidMessageException("Data cannot be decoded or JSON is invalid");
        }

        JSONObject jsonData = new JSONObject(data);

        if (!isValid(jsonData)) {
            throw new InvalidMessageException("Trying to create a Message Object with invalid data");
        }

        this.type = jsonData.getString(TYPE);
        this.data = jsonData.getJSONObject(DATA);

        communication_type = guessType();
    }

    public static CommunicationMessage createNack(final String reason) {
        CommunicationMessage result = new CommunicationMessage(COMMUNICATION_TYPE.RESPONSE, TYPE_RESPONSE);

        result.addData(RESPONSE_VALUE, RESPONSE_ERROR);
        result.addData(RESPONSE_REASON, reason);

        return result;
    }

    public static CommunicationMessage createAck() {
        CommunicationMessage result = new CommunicationMessage(COMMUNICATION_TYPE.RESPONSE, TYPE_RESPONSE);

        result.addData(RESPONSE_VALUE, RESPONSE_SUCCESS);

        return result;
    }

    public static CommunicationMessage createConnection(final String ine, final String password) {
        CommunicationMessage communicationMessage = new CommunicationMessage(COMMUNICATION_TYPE.CONNECTION, TYPE_CONNECTION);

        communicationMessage.addData(CONNECTION_INE, ine);
        communicationMessage.addData(CONNECTION_PASSWORD, password);

        return communicationMessage;
    }

    public static CommunicationMessage createTicket(final String ticketTitle, final String ticketGroup,
                                                    final String contents) {

        CommunicationMessage communicationMessage = new CommunicationMessage(COMMUNICATION_TYPE.TICKET, TYPE_TICKET);

        communicationMessage.addData(TICKET_TITLE, ticketTitle);
        communicationMessage.addData(TICKET_GROUP, ticketGroup);
        communicationMessage.addData(TICKET_MESSAGE, contents);

        return communicationMessage;
    }

    public static CommunicationMessage createMessage(final String ticketID, final String contents) {
        CommunicationMessage communicationMessage = new CommunicationMessage(COMMUNICATION_TYPE.MESSAGE, TYPE_MESSAGE);

        communicationMessage.addData(MESSAGE_TICKET_ID, ticketID);
        communicationMessage.addData(MESSAGE_CONTENTS, contents);

        return communicationMessage;
    }

    public static CommunicationMessage createUpdate(final String contents) {
        CommunicationMessage communicationMessage = new CommunicationMessage(COMMUNICATION_TYPE.UPDATE, TYPE_UPDATE);

        communicationMessage.addData(UPDATE_CONTENTS, contents);

        return communicationMessage;
    }

    public static CommunicationMessage createLocalUpdate(final Date from) {
        CommunicationMessage communicationMessage = new CommunicationMessage(COMMUNICATION_TYPE.LOCAL_UPDATE, TYPE_LOCAL_UPDATE);

        communicationMessage.addData(LOCAL_UPDATE_DATE, Long.toString(from.getTime()));

        return communicationMessage;
    }

    public static CommunicationMessage createLocalUpdateResponse(TreeSet<Groupe> groups) {
        CommunicationMessage communicationMessage = new CommunicationMessage(COMMUNICATION_TYPE.LOCAL_UPDATE_RESPONSE, TYPE_LOCAL_UPDATE_RESPONSE);

        JSONArray array = new JSONArray();
        for (Groupe group : groups) {
            array.put(group.toJSON());
        }

        communicationMessage.addData(LOCAL_UPDATE_RESPONSE, array);

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

    private void addData(String key, String data) {
        this.data.put(key, data);
    }

    private void addData(String key, byte[] data) {
        this.data.put(key, data);
    }

    private void addData(String key, JSONArray data) {
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

        return result.toString() + "\n";
    }

    public String toFormattedString() {
        JSONObject result = new JSONObject();
        result.put(TYPE, type);
        result.put(DATA, data);

        return format("", result, 0);
    }


    private String format(final String key, JSONObject object, int padding) {
        final String pad = new String(new char[padding]).replace("\0", "\t");

        StringBuilder builder = new StringBuilder();

        if (key.isEmpty()) {
            builder.append(pad).append("{").append('\n');
        } else {
            builder.append(pad).append(key).append(": ").append("{").append('\n');
        }

        int position = 0;
        for (String s : object.keySet()) {
            final Object o = object.get(s);
            if (o instanceof JSONObject) {
                builder.append(format(s, (JSONObject) o, padding + 1));
            } else if (o instanceof JSONArray) {
                builder.append(format(s, (JSONArray) o, padding + 1));
            } else {
                builder.append(pad).append("\t").append(s).append(": ").append(o);
            }

            if ((++position) != object.keySet().size()) {
                builder.append(",");
            }

            builder.append("\n");
        }

        builder.append(pad).append("}");

        return builder.toString();
    }

    private String format(final String key, JSONArray array, int padding) {
        final String pad = new String(new char[padding]).replace("\0", "\t");

        StringBuilder builder = new StringBuilder();

        if (key.isEmpty()) {
            builder.append(pad).append("{").append('\n');
        } else {
            builder.append(pad).append(key).append(": ").append("[").append('\n');
        }

        for (int i = 0; i < array.length(); ++i) {
            final Object o = array.get(i);
            if (o instanceof JSONObject) {
                builder.append(format("", (JSONObject) o, padding + 1));
            } else if (o instanceof JSONArray) {
                builder.append(format("", (JSONArray) o, padding + 1));
            } else {
                builder.append(pad).append("\t").append(o);
            }

            if (i + 1 < array.length()) {
                builder.append(",");
            }

            builder.append("\n");
        }
        builder.append(pad).append("]");

        return builder.toString();
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
