package backend.server.communication.classic;

import backend.data.*;
import backend.modele.GroupModel;
import backend.modele.MessageModel;
import backend.modele.TicketModel;
import backend.modele.UserModel;
import backend.server.communication.CommunicationMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import static backend.database.Keys.*;

public class ClassicMessage extends CommunicationMessage {

    public static final String TYPE_KEY_XCHANGE = "keyxchange";
    public static final String TYPE_CONNECTION = "connection";
    public static final String TYPE_TICKET = "ticket";
    public static final String TYPE_MESSAGE = "message";
    public static final String TYPE_RESPONSE = "response";
    public static final String TYPE_LOCAL_UPDATE = "local_update";
    public static final String TYPE_LOCAL_UPDATE_RESPONSE = "local_update_response";
    public static final String TYPE_ENTRY_ADDED = "added";
    public static final String TYPE_ENTRY_DELETED = "deleted";
    public static final String TYPE_ENTRY_UPDATED = "updated";
    public final static String TYPE_DELETE = "deletion";
    public final static String TYPE_ADD = "add";
    public final static String TYPE_UPDATE = "update";
    public final static String TYPE_MESSAGE_RECEIVED = "message_received";
    private static final String TYPE_TICKET_CLICKED = "ticket_clicked";
    private static final String TYPE_TABLE_MODEL = "table_model";
    private static final String TYPE_TABLE_MODEL_REQUEST = "model_request";
    private static final String TYPE_REQUEST_EVERYTHING = "request_everything";


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

    public static final String LOCAL_UPDATE_DATE = "contents";

    private static final String TICKET_CLICKED_ID = "id";

    private static final String TABLE = "table";
    private static final String ENTRY = "entry";

    private static final String RELATED_TICKETS = "related_tickets";
    private static final String RELATED_GROUPS = "related_groups";
    private static final String ALL_GROUPS = "all_groups";
    private static final String USERS = "users";
    private static final String MESSAGE_RECEIVED = "message_received";


    private final CLASSIC_MESSAGE_TYPE CLASSICMESSAGE_type;
    
    /**
     * Constructeur de l'objet Message à partir d'un type de message et 
    **/
    private ClassicMessage(CLASSIC_MESSAGE_TYPE msg_type, final String type) {
        this.CLASSICMESSAGE_type = msg_type;
        setTypeString(type);
    }

    public ClassicMessage(String data) throws InvalidMessageException {

        if (data == null || !isValidJSON(data)) {
            throw new InvalidMessageException("Data cannot be decoded or JSON is invalid");
        }

        JSONObject jsonData = new JSONObject(data);

        if (!isValid(jsonData)) {
            throw new InvalidMessageException("Trying to create a Message Object with invalid data");
        }

        setTypeString(jsonData.getString(TYPE));

        setData(jsonData.getJSONObject(DATA));

        CLASSICMESSAGE_type = guessType();
    }

    public static ClassicMessage createNack(final String reason) {
        ClassicMessage result = new ClassicMessage(CLASSIC_MESSAGE_TYPE.RESPONSE, TYPE_RESPONSE);

        result.addData(RESPONSE_VALUE, RESPONSE_ERROR);
        result.addData(RESPONSE_REASON, reason);

        return result;
    }

    public static ClassicMessage createAck() {
        ClassicMessage result = new ClassicMessage(CLASSIC_MESSAGE_TYPE.RESPONSE, TYPE_RESPONSE);

        result.addData(RESPONSE_VALUE, RESPONSE_SUCCESS);

        return result;
    }

    public static ClassicMessage createConnection(final String ine, final String password) {
        ClassicMessage classicMessage = new ClassicMessage(CLASSIC_MESSAGE_TYPE.CONNECTION, TYPE_CONNECTION);

        classicMessage.addData(CONNECTION_INE, ine);
        classicMessage.addData(CONNECTION_PASSWORD, password);

        return classicMessage;
    }

    public static ClassicMessage createTicket(final String ticketTitle, final String ticketGroup,
                                              final String contents) {

        ClassicMessage classicMessage = new ClassicMessage(CLASSIC_MESSAGE_TYPE.TICKET, TYPE_TICKET);

        classicMessage.addData(TICKET_TITLE, ticketTitle);
        classicMessage.addData(TICKET_GROUP, ticketGroup);
        classicMessage.addData(TICKET_MESSAGE, contents);

        return classicMessage;
    }

    public static ClassicMessage createMessage(final Long ticketID, final String contents) {
        ClassicMessage classicMessage = new ClassicMessage(CLASSIC_MESSAGE_TYPE.MESSAGE, TYPE_MESSAGE);

        classicMessage.addData(MESSAGE_TICKET_ID, ticketID.toString());
        classicMessage.addData(MESSAGE_CONTENTS, contents);

        return classicMessage;
    }


    public static ClassicMessage createLocalUpdate(final Date from) {
        ClassicMessage classicMessage = new ClassicMessage(CLASSIC_MESSAGE_TYPE.LOCAL_UPDATE, TYPE_LOCAL_UPDATE);

        classicMessage.addData(LOCAL_UPDATE_DATE, Long.toString(from.getTime()));

        return classicMessage;
    }

    public static ClassicMessage createLocalUpdateResponse(
            TreeSet<Groupe> relatedGroups, TreeSet<String> allGroups, TreeSet<Utilisateur> users) {
        ClassicMessage classicMessage = new ClassicMessage(CLASSIC_MESSAGE_TYPE.LOCAL_UPDATE_RESPONSE, TYPE_LOCAL_UPDATE_RESPONSE);

        JSONArray relatedGroupsArray = new JSONArray();
        for (Groupe group : relatedGroups) {
            relatedGroupsArray.put(group.toJSON());
        }

        JSONArray usersArray = new JSONArray();
        for (Utilisateur u : users) {
            usersArray.put(u.toJSON());
        }

        JSONArray allGroupsArray = new JSONArray();
        for (String s : allGroups) {
            allGroupsArray.put(s);
        }


        classicMessage.addData(RELATED_GROUPS, relatedGroupsArray);
        classicMessage.addData(ALL_GROUPS, allGroupsArray);
        classicMessage.addData(USERS, usersArray);

        return classicMessage;
    }


    public static ClassicMessage createTicketClicked(Ticket ticket) {

        ClassicMessage classicMessage = new ClassicMessage(CLASSIC_MESSAGE_TYPE.TICKET_CLICKED, TYPE_TICKET_CLICKED);

        classicMessage.addData(TICKET_CLICKED_ID, ticket.getID().toString());

        return classicMessage;

    }


    public static ClassicMessage createEntryDeletedMessage(final String table, ProjectTable entry) {

        ClassicMessage classicMessage = new ClassicMessage(CLASSIC_MESSAGE_TYPE.ENTRY_DELETED, TYPE_ENTRY_DELETED);

        classicMessage.addData(TABLE, table);
        classicMessage.addData(ENTRY, entry.toJSON().toString());

        return classicMessage;

    }

    public static ClassicMessage createTicketDeletedMessage(final String table, Ticket entry, Groupe relatedGroup) {

        ClassicMessage classicMessage = new ClassicMessage(CLASSIC_MESSAGE_TYPE.ENTRY_DELETED, TYPE_ENTRY_DELETED);

        classicMessage.addData(TABLE, table);
        classicMessage.addData(ENTRY, entry.toJSON().toString());
        classicMessage.addData(RELATED_GROUPS, relatedGroup.toJSON().toString());

        return classicMessage;

    }

    public static ClassicMessage createMessageDeletedMessage
            (final String table, Message entry, Groupe relatedGroup, Ticket relatedTicket) {

        ClassicMessage classicMessage = new ClassicMessage(CLASSIC_MESSAGE_TYPE.ENTRY_DELETED, TYPE_ENTRY_DELETED);

        classicMessage.addData(TABLE, table);
        classicMessage.addData(ENTRY, entry.toJSON().toString());
        classicMessage.addData(RELATED_GROUPS, relatedGroup.toJSON().toString());
        classicMessage.addData(RELATED_TICKETS, relatedTicket.toJSON().toString());

        return classicMessage;

    }

    public static ClassicMessage createEntryAddedMessage(final String table, ProjectTable entry) {

        ClassicMessage classicMessage = new ClassicMessage(CLASSIC_MESSAGE_TYPE.ENTRY_ADDED, TYPE_ENTRY_ADDED);

        classicMessage.addData(TABLE, table);
        classicMessage.addData(ENTRY, entry.toJSON().toString());

        return classicMessage;

    }

    public static ClassicMessage createTicketAddedMessage(final String table, Ticket entry, Groupe relatedGroup) {

        ClassicMessage classicMessage = new ClassicMessage(CLASSIC_MESSAGE_TYPE.ENTRY_ADDED, TYPE_ENTRY_ADDED);

        classicMessage.addData(TABLE, table);
        classicMessage.addData(ENTRY, entry.toJSON().toString());
        classicMessage.addData(RELATED_GROUPS, relatedGroup.toJSON().toString());

        return classicMessage;

    }

    public static ClassicMessage createMessageAddedMessage
            (final String table, Message entry, Groupe relatedGroup, Ticket relatedTicket) {

        ClassicMessage classicMessage = new ClassicMessage(CLASSIC_MESSAGE_TYPE.ENTRY_ADDED, TYPE_ENTRY_ADDED);

        classicMessage.addData(TABLE, table);
        classicMessage.addData(ENTRY, entry.toJSON().toString());
        classicMessage.addData(RELATED_GROUPS, relatedGroup.toJSON().toString());
        classicMessage.addData(RELATED_TICKETS, relatedTicket.toJSON().toString());

        return classicMessage;

    }


    public static ClassicMessage createEntryUpdatedMessage(final String table, ProjectTable entry) {

        ClassicMessage classicMessage = new ClassicMessage(CLASSIC_MESSAGE_TYPE.ENTRY_UPDATED, TYPE_ENTRY_UPDATED);

        classicMessage.addData(TABLE, table);
        classicMessage.addData(ENTRY, entry.toJSON().toString());

        return classicMessage;

    }

    public static ClassicMessage createTicketUpdatedMessage(final String table, Ticket entry, Groupe relatedGroup) {

        ClassicMessage classicMessage = new ClassicMessage(CLASSIC_MESSAGE_TYPE.ENTRY_UPDATED, TYPE_ENTRY_UPDATED);

        classicMessage.addData(TABLE, table);
        classicMessage.addData(ENTRY, entry.toJSON().toString());
        classicMessage.addData(RELATED_GROUPS, relatedGroup.toJSON().toString());

        return classicMessage;

    }

    public static ClassicMessage createMessageUpdatedMessage(final String table, Message entry, Groupe relatedGroup, Ticket ticket) {

        ClassicMessage classicMessage = new ClassicMessage(CLASSIC_MESSAGE_TYPE.ENTRY_UPDATED, TYPE_ENTRY_UPDATED);

        classicMessage.addData(TABLE, table);
        classicMessage.addData(ENTRY, entry.toJSON().toString());
        classicMessage.addData(RELATED_GROUPS, relatedGroup.toJSON().toString());
        classicMessage.addData(RELATED_TICKETS, ticket.toJSON().toString());

        return classicMessage;

    }

    public static ClassicMessage createDeleteMessage(final String table, ProjectTable entry) {

        ClassicMessage message = new ClassicMessage(CLASSIC_MESSAGE_TYPE.DELETE, TYPE_DELETE);

        message.addData(TABLE, table);
        message.addData(ENTRY, entry.toJSON().toString());

        return message;

    }

    public static ClassicMessage createAddMessage(final String table, ProjectTable entry) {

        ClassicMessage message = new ClassicMessage(CLASSIC_MESSAGE_TYPE.ADD, TYPE_ADD);

        message.addData(TABLE, table);
        message.addData(ENTRY, entry.toJSON().toString());

        return message;

    }

    public static ClassicMessage createUpdateMessage(final String table, ProjectTable entry) {

        ClassicMessage message = new ClassicMessage(CLASSIC_MESSAGE_TYPE.UPDATE, TYPE_UPDATE);

        message.addData(TABLE, table);
        message.addData(ENTRY, entry.toJSON().toString());

        return message;

    }

    public static ClassicMessage createTableModelRequest() {

        ClassicMessage message = new ClassicMessage(CLASSIC_MESSAGE_TYPE.TABLE_MODEL_REQUEST, TYPE_TABLE_MODEL_REQUEST);

        return message;

    }

    public static ClassicMessage createRequestEverything() {

        ClassicMessage message = new ClassicMessage(CLASSIC_MESSAGE_TYPE.REQUEST_EVERYTHING, TYPE_REQUEST_EVERYTHING);

        return message;

    }

    public static ClassicMessage createMessageReceived(ArrayList<Message> received) {

        ClassicMessage message = new ClassicMessage(CLASSIC_MESSAGE_TYPE.MESSAGE_RECEIVED, TYPE_MESSAGE_RECEIVED);

        JSONArray array = new JSONArray();
        for (Message s : received) {
            array.put(s.toJSON());
        }

        message.addData(MESSAGE_RECEIVED, array);

        return message;

    }

    public static ClassicMessage createTableModel
            (List<Utilisateur> users, List<Groupe> groups, List<Ticket> tickets, List<Message> messages) {

        ClassicMessage message = new ClassicMessage(CLASSIC_MESSAGE_TYPE.TABLE_MODEL, TYPE_TABLE_MODEL);

        JSONArray array = new JSONArray();
        for (Utilisateur u : users) {
            array.put(u.toJSON());
        }
        message.addData(TABLE_NAME_UTILISATEUR, array);

        array = new JSONArray();
        for (Groupe g : groups) {
            array.put(g.toJSON());
        }
        message.addData(TABLE_NAME_GROUPE, array);

        array = new JSONArray();
        for (Ticket t : tickets) {
            array.put(t.toJSON());
        }
        message.addData(TABLE_NAME_TICKET, array);

        array = new JSONArray();
        for (Message m : messages) {
            array.put(m.toJSON());
        }
        message.addData(TABLE_NAME_MESSAGE, array);

        return message;

    }


    private CLASSIC_MESSAGE_TYPE guessType() throws InvalidMessageException {
        switch (getTypeToString()) {
            case TYPE_KEY_XCHANGE:
                return CLASSIC_MESSAGE_TYPE.KEYXCHANGE;

            case TYPE_CONNECTION:
                checkForConnectionValidity();
                return CLASSIC_MESSAGE_TYPE.CONNECTION;

            case TYPE_MESSAGE:
                checkForMessageValidity();
                return CLASSIC_MESSAGE_TYPE.MESSAGE;

            case TYPE_RESPONSE:
                checkForResponseValidity();
                return CLASSIC_MESSAGE_TYPE.RESPONSE;

            case TYPE_TICKET:
                checkForTicketValidity();
                return CLASSIC_MESSAGE_TYPE.TICKET;

            case TYPE_LOCAL_UPDATE:
                checkForLocalUpdateValidity();
                return CLASSIC_MESSAGE_TYPE.LOCAL_UPDATE;

            case TYPE_LOCAL_UPDATE_RESPONSE:
                checkForLocalUpdateResponseValidity();
                return CLASSIC_MESSAGE_TYPE.LOCAL_UPDATE_RESPONSE;

            case TYPE_TICKET_CLICKED:
                checkForTicketClickedValidity();
                return CLASSIC_MESSAGE_TYPE.TICKET_CLICKED;

            case TYPE_ENTRY_ADDED:
                checkForEntryAddedValidity();
                return CLASSIC_MESSAGE_TYPE.ENTRY_ADDED;

            case TYPE_ENTRY_DELETED:
                checkForEntryDeletedValidity();
                return CLASSIC_MESSAGE_TYPE.ENTRY_DELETED;

            case TYPE_ENTRY_UPDATED:
                checkForEntryUpdatedValidity();
                return CLASSIC_MESSAGE_TYPE.ENTRY_UPDATED;

            case TYPE_DELETE:
                checkForDeleteValidity();
                return CLASSIC_MESSAGE_TYPE.DELETE;

            case TYPE_ADD:
                checkForAddValidity();
                return CLASSIC_MESSAGE_TYPE.ADD;

            case TYPE_UPDATE:
                checkForUpdateValidity();
                return CLASSIC_MESSAGE_TYPE.UPDATE;

            case TYPE_TABLE_MODEL:
                checkForTableModelValidity();
                return CLASSIC_MESSAGE_TYPE.TABLE_MODEL;

            case TYPE_TABLE_MODEL_REQUEST:
                return CLASSIC_MESSAGE_TYPE.TABLE_MODEL_REQUEST;

            case TYPE_REQUEST_EVERYTHING:
                return CLASSIC_MESSAGE_TYPE.REQUEST_EVERYTHING;

            case TYPE_MESSAGE_RECEIVED:
                return CLASSIC_MESSAGE_TYPE.MESSAGE_RECEIVED;


            default:
                throw new InvalidMessageException("Message with invalid type: " + getTypeToString());
        }
    }

    private void checkForTableModelValidity() throws InvalidMessageException {
        if (!getData().has(TABLE_NAME_GROUPE) || !getData().has(TABLE_NAME_MESSAGE)
                || !getData().has(TABLE_NAME_TICKET) || !getData().has(TABLE_NAME_UTILISATEUR)) {
            throw new InvalidMessageException("Missing field in table model request message");
        }
    }

    private void checkForUpdateValidity() throws InvalidMessageException {
        if (!getData().has(TABLE) || !getData().has(ENTRY)) {
            throw new InvalidMessageException("Missing field in update message");
        }
    }

    private void checkForAddValidity() throws InvalidMessageException {
        if (!getData().has(TABLE) || !getData().has(ENTRY)) {
            throw new InvalidMessageException("Missing field in add message");
        }
    }

    private void checkForDeleteValidity() throws InvalidMessageException {
        if (!getData().has(TABLE) || !getData().has(ENTRY)) {
            throw new InvalidMessageException("Missing field in delete message");
        }
    }

    private void checkForEntryAddedValidity() throws InvalidMessageException {
        if (!getData().has(TABLE) || !getData().has(ENTRY)) {
            throw new InvalidMessageException("Missing field in added entry message");
        }
    }

    private void checkForEntryDeletedValidity() throws InvalidMessageException {
        if (!getData().has(TABLE) || !getData().has(ENTRY)) {
            throw new InvalidMessageException("Missing field in deleted entry message");
        }
    }

    private void checkForEntryUpdatedValidity() throws InvalidMessageException {
        if (!getData().has(TABLE) || !getData().has(ENTRY)) {
            throw new InvalidMessageException("Missing field in updated entry message");
        }
    }


    private void checkForConnectionValidity() throws InvalidMessageException {
        if (!getData().has(CONNECTION_INE) || !getData().has(CONNECTION_PASSWORD)) {
            throw new InvalidMessageException("Missing field in connection Message : \n" + getData().toString() + "\n" + toFormattedString());
        }
    }

    private void checkForMessageValidity() throws InvalidMessageException {
        if (!getData().has(MESSAGE_TICKET_ID) || !getData().has(MESSAGE_CONTENTS)) {
            throw new InvalidMessageException("Missing field in message type Message");
        }
    }

    private void checkForResponseValidity() throws InvalidMessageException {
        if (!getData().has(RESPONSE_VALUE)) {
            throw new InvalidMessageException("Missing field in response Message");
        }

        if (getData().getString(RESPONSE_VALUE).equals(RESPONSE_ERROR) && !getData().has(RESPONSE_REASON)) {
            throw new InvalidMessageException("Missing reason in response Message");
        }
    }

    private void checkForTicketValidity() throws InvalidMessageException {
        if (!getData().has(TICKET_TITLE) || !getData().has(TICKET_GROUP) || !getData().has(TICKET_MESSAGE)) {
            throw new InvalidMessageException("Missing field in ticket Message");
        }
    }

    private void checkForLocalUpdateValidity() throws InvalidMessageException {
        if (!getData().has(LOCAL_UPDATE_DATE)) {
            throw new InvalidMessageException("Missing field in update message");
        }
    }

    private void checkForLocalUpdateResponseValidity() throws InvalidMessageException {
        if (!getData().has(ALL_GROUPS) || !getData().has(RELATED_GROUPS)) {
            throw new InvalidMessageException("Missing field in update message");
        }
    }

    private void checkForTicketClickedValidity() throws InvalidMessageException {
        if (!getData().has(TICKET_CLICKED_ID)) {
            throw new InvalidMessageException("Missing field in ticket clicked message");
        }
    }

    public Boolean isAck() {
        return getType().equals(CLASSIC_MESSAGE_TYPE.RESPONSE) && getData().getString(RESPONSE_VALUE).equals(RESPONSE_SUCCESS);
    }

    public Boolean isNack() {
        return getType().equals(CLASSIC_MESSAGE_TYPE.RESPONSE) && getData().getString(RESPONSE_VALUE).equals(RESPONSE_ERROR);
    }


    public Boolean isConnection() {
        return getType().equals(CLASSIC_MESSAGE_TYPE.CONNECTION);
    }

    public Boolean isTicket() {
        return getType().equals(CLASSIC_MESSAGE_TYPE.TICKET);
    }

    public Boolean isMessage() {
        return getType().equals(CLASSIC_MESSAGE_TYPE.MESSAGE);
    }

    public Boolean isLocalUpdateResponse() {
        return getType().equals(CLASSIC_MESSAGE_TYPE.LOCAL_UPDATE_RESPONSE);
    }

    public CLASSIC_MESSAGE_TYPE getType() {
        return CLASSICMESSAGE_type;
    }


    public String getNackReason() throws WrongMessageTypeException {
        if (!isNack()) {
            throw new WrongMessageTypeException("Requiring Nack reason on a non-nack message");
        }

        return getData().getString(RESPONSE_REASON);
    }


    public String getConnectionINE() {
        return getData().getString(CONNECTION_INE);
    }

    public String getConnectionPassword() {
        return getData().getString(CONNECTION_PASSWORD);
    }

    public String getTicketTitle() {
        return getData().getString(TICKET_TITLE);
    }

    public String getTicketGroup() {
        return getData().getString(TICKET_GROUP);
    }

    public String getTicketMessage() {
        return getData().getString(TICKET_MESSAGE);
    }

    public String getMessageContents() {
        return getData().getString(MESSAGE_CONTENTS);
    }

    public Long getMessageTicketID() {
        return getData().getLong(MESSAGE_TICKET_ID);
    }

    public Long getTicketClickedID() {
        return getData().getLong(TICKET_CLICKED_ID);
    }

    public TreeSet<Groupe> getLocalUpdateResponseRelatedGroups() {
        TreeSet<Groupe> groups = new TreeSet<>();
        JSONArray array = getData().getJSONArray(RELATED_GROUPS);
        for (int i = 0; i < array.length(); ++i) {
            groups.add(new Groupe(array.getJSONObject(i)));
        }

        return groups;
    }


    public TreeSet<String> getLocalUpdateResponseAllGroups() {
        TreeSet<String> groups = new TreeSet<>();
        JSONArray array = getData().getJSONArray(ALL_GROUPS);
        for (int i = 0; i < array.length(); ++i) {
            groups.add(array.getString(i));
        }

        return groups;
    }

    public TreeSet<Utilisateur> getLocalUpdateResponseUsers() {
        TreeSet<Utilisateur> users = new TreeSet<>();
        JSONArray array = getData().getJSONArray(USERS);
        for (int i = 0; i < array.length(); ++i) {
            users.add(new Utilisateur(array.getJSONObject(i)));
        }

        return users;
    }

    public Groupe getEntryRelatedGroup() {
        return new Groupe(new JSONObject(getData().getString(RELATED_GROUPS)));
    }

    public Ticket getEntryRelatedTicket() {
        System.out.println(getData().getString(RELATED_TICKETS));
        return new Ticket(new JSONObject(getData().getString(RELATED_TICKETS)));
    }

    public UserModel getTableModelUserModel() {
        JSONArray array = getData().getJSONArray(TABLE_NAME_UTILISATEUR);
        List<Utilisateur> users = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            users.add(new Utilisateur(array.getJSONObject(i)));
        }

        return new UserModel(users);
    }

    public GroupModel getTableModelGroupModel() {
        JSONArray array = getData().getJSONArray(TABLE_NAME_GROUPE);
        List<Groupe> groups = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            groups.add(new Groupe(array.getJSONObject(i)));
        }

        return new GroupModel(groups);
    }

    public TicketModel getTableModelTicketModel() {
        JSONArray array = getData().getJSONArray(TABLE_NAME_TICKET);
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            tickets.add(new Ticket(array.getJSONObject(i)));
        }

        return new TicketModel(tickets);
    }

    public MessageModel getTableModelMessageModel() {
        JSONArray array = getData().getJSONArray(TABLE_NAME_MESSAGE);
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            messages.add(new Message(array.getJSONObject(i)));
        }

        return new MessageModel(messages);
    }

    public ArrayList<Message> getMessagesReceived() {
        JSONArray array = getData().getJSONArray(MESSAGE_RECEIVED);
        ArrayList<Message> received = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            received.add(new Message(array.getJSONObject(i)));
        }

        return received;
    }
}
