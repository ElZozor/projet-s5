package backend.server.communication;

import backend.data.*;
import backend.modele.GroupModel;
import backend.modele.MessageModel;
import backend.modele.TicketModel;
import backend.modele.UserModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import static backend.database.Keys.*;

public class CommunicationMessage {

    public static final String TYPE = "type";
    public static final String DATA = "data";

    public final static String TABLE = "table";
    public final static String ENTRY = "entry";
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
    private static final String TYPE_TICKET_CLICKED = "ticket_clicked";
    private static final String TYPE_TABLE_MODEL = "table_model";
    private static final String TYPE_TABLE_MODEL_REQUEST = "model_request";
    private static final String TYPE_REQUEST_EVERYTHING = "request_everything";
    private static final String TICKET_CLICKED_ID = "id";
    private static final String RELATED_TICKETS = "related_tickets";
    private static final String RELATED_GROUPS = "related_groups";
    private static final String ALL_GROUPS = "all_groups";
    private static final String USERS = "users";
    private static final String MESSAGE_RECEIVED = "message_received";
    protected final MESSAGE_TYPE CLASSICMESSAGE_type;

    private String type;
    private JSONObject data = new JSONObject();


    /**
     * Constructeur de l'objet Message à partir d'un type de message et d'un type de message de communication
     *
     * @param msg_type - type du message
     * @param type     - type de communication du message
     **/
    private CommunicationMessage(MESSAGE_TYPE msg_type, final String type) {
        this.CLASSICMESSAGE_type = msg_type;
        setTypeString(type);
    }

    /**
     * Constructeur de l'objet ClassicMessage à partie de données
     *
     * @param data - données du message
     * @throws InvalidMessageException peut être renvoyé si le message n'est pas valide au format JSON (voir isValidJSON)
     **/
    public CommunicationMessage(String data) throws InvalidMessageException {

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


    /**
     * methode créant un message NACK
     *
     * @param reason - information sur l'erreur
     * @return message créé
     **/
    public static CommunicationMessage createNack(final String reason) {
        CommunicationMessage result = new CommunicationMessage(MESSAGE_TYPE.RESPONSE, TYPE_RESPONSE);

        result.addData(RESPONSE_VALUE, RESPONSE_ERROR);
        result.addData(RESPONSE_REASON, reason);

        return result;
    }

    /**
     * Methode créant un message d'acquitement
     *
     * @return message créé
     **/
    public static CommunicationMessage createAck() {
        CommunicationMessage result = new CommunicationMessage(MESSAGE_TYPE.RESPONSE, TYPE_RESPONSE);

        result.addData(RESPONSE_VALUE, RESPONSE_SUCCESS);

        return result;
    }

    /**
     * methode créant un message de connexion au client
     *
     * @param ine      - Identifiant national d'étudiant
     * @param password - mot de passe de celui se connectant
     * @return message de connexion créé
     **/
    public static CommunicationMessage createConnection(final String ine, final String password) {
        CommunicationMessage communicationMessage = new CommunicationMessage(MESSAGE_TYPE.CONNECTION, TYPE_CONNECTION);

        communicationMessage.addData(CONNECTION_INE, ine);
        communicationMessage.addData(CONNECTION_PASSWORD, password);

        return communicationMessage;
    }

    /**
     * methode créant un message de création de ticket
     *
     * @param ticketTitle - titre du ticket à créer
     * @param ticketGroup - Groupe auquel est rattaché le ticket
     * @param contents    - contenu du message de création
     * @return message de création de ticket créé
     **/
    public static CommunicationMessage createTicket(final String ticketTitle, final String ticketGroup,
                                                    final String contents) {

        CommunicationMessage communicationMessage = new CommunicationMessage(MESSAGE_TYPE.TICKET, TYPE_TICKET);

        communicationMessage.addData(TICKET_TITLE, ticketTitle);
        communicationMessage.addData(TICKET_GROUP, ticketGroup);
        communicationMessage.addData(TICKET_MESSAGE, contents);

        return communicationMessage;
    }

    /**
     * methode créant un message de création de message
     *
     * @param ticketID - ticket sur lequel sera posté le message
     * @param contents - contenu du message posté
     * @return message de création de message créé
     **/
    public static CommunicationMessage createMessage(final Long ticketID, final String contents) {
        CommunicationMessage communicationMessage = new CommunicationMessage(MESSAGE_TYPE.MESSAGE, TYPE_MESSAGE);

        communicationMessage.addData(MESSAGE_TICKET_ID, ticketID.toString());
        communicationMessage.addData(MESSAGE_CONTENTS, contents);

        return communicationMessage;
    }

    /**
     * methode créant un message de demande de mis à jour par le client
     *
     * @param from - date de l'emission de la demande
     * @return message de demande de mise à jour créé
     **/
    public static CommunicationMessage createLocalUpdate(final Date from) {
        CommunicationMessage communicationMessage = new CommunicationMessage(MESSAGE_TYPE.LOCAL_UPDATE, TYPE_LOCAL_UPDATE);

        communicationMessage.addData(LOCAL_UPDATE_DATE, Long.toString(from.getTime()));

        return communicationMessage;
    }

    /**
     * methode créant un message en réponse à une demande de mise à jour
     *
     * @param relatedGroups - groupes liés au client ayant fait la demande
     * @param allGroups     - tous les groupes de la base de données
     * @return message de réponse crée
     * @users - tous les utilisateurs
     **/
    public static CommunicationMessage createLocalUpdateResponse(
            TreeSet<Groupe> relatedGroups, TreeSet<String> allGroups, TreeSet<Utilisateur> users) {
        CommunicationMessage communicationMessage = new CommunicationMessage(MESSAGE_TYPE.LOCAL_UPDATE_RESPONSE, TYPE_LOCAL_UPDATE_RESPONSE);

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


        communicationMessage.addData(RELATED_GROUPS, relatedGroupsArray);
        communicationMessage.addData(ALL_GROUPS, allGroupsArray);
        communicationMessage.addData(USERS, usersArray);

        return communicationMessage;
    }

    /**
     * methode créant un message d'indication de ticket séléctionné
     *
     * @param ticket selectionné
     * @return message créé
     **/
    public static CommunicationMessage createTicketClicked(Ticket ticket) {

        CommunicationMessage communicationMessage = new CommunicationMessage(MESSAGE_TYPE.TICKET_CLICKED, TYPE_TICKET_CLICKED);

        communicationMessage.addData(TICKET_CLICKED_ID, ticket.getID().toString());

        return communicationMessage;

    }

    /**
     * methode de création de message de signalement de signalement d'entrée supprimée
     *
     * @param table - table concernée
     * @param entry - entree à supprimer
     * @return un message créé
     **/
    public static CommunicationMessage createEntryDeletedMessage(final String table, ProjectTable entry) {

        CommunicationMessage communicationMessage = new CommunicationMessage(MESSAGE_TYPE.ENTRY_DELETED, TYPE_ENTRY_DELETED);

        communicationMessage.addData(TABLE, table);
        communicationMessage.addData(ENTRY, entry.toJSON().toString());

        return communicationMessage;

    }

    /**
     * methode de création de message de signalement de ticket supprimé
     *
     * @param table - table concernée
     * @param entry - entrée à supprimer
     * @return le message créé
     **/
    public static CommunicationMessage createTicketDeletedMessage(final String table, Ticket entry) {

        CommunicationMessage communicationMessage = new CommunicationMessage(MESSAGE_TYPE.ENTRY_DELETED, TYPE_ENTRY_DELETED);

        communicationMessage.addData(TABLE, table);
        communicationMessage.addData(ENTRY, entry.toJSON().toString());

        return communicationMessage;

    }

    /**
     * methode créant un message de signalement de message supprimé
     *
     * @param table
     * @param entry
     * @param relatedGroup  - groupe sur lequel le ticket comportant le message est lié
     * @param relatedTicket - ticket sur lequel le message à été posté
     * @return le message créé
     **/
    public static CommunicationMessage createMessageDeletedMessage
    (final String table, Message entry, Groupe relatedGroup, Ticket relatedTicket) {

        CommunicationMessage communicationMessage = new CommunicationMessage(MESSAGE_TYPE.ENTRY_DELETED, TYPE_ENTRY_DELETED);

        communicationMessage.addData(TABLE, table);
        communicationMessage.addData(ENTRY, entry.toJSON().toString());
        communicationMessage.addData(RELATED_GROUPS, relatedGroup.toJSON().toString());
        communicationMessage.addData(RELATED_TICKETS, relatedTicket.toJSON().toString());

        return communicationMessage;

    }

    /**
     * methode créant un message de signalement d'entrée créée
     *
     * @param table - table concernée
     * @param entry - entrée créée
     * @return message créé
     **/
    public static CommunicationMessage createEntryAddedMessage(final String table, ProjectTable entry) {

        CommunicationMessage communicationMessage = new CommunicationMessage(MESSAGE_TYPE.ENTRY_ADDED, TYPE_ENTRY_ADDED);

        communicationMessage.addData(TABLE, table);
        communicationMessage.addData(ENTRY, entry.toJSON().toString());

        return communicationMessage;

    }

    /**
     * methode créant un message designalement de ticket créé
     *
     * @param table        - table concernée
     * @param entry        - entrée de table
     * @param relatedGroup - groupe lié au ticket
     * @return le message créé
     **/
    public static CommunicationMessage createTicketAddedMessage(final String table, Ticket entry, Groupe relatedGroup) {

        CommunicationMessage communicationMessage = new CommunicationMessage(MESSAGE_TYPE.ENTRY_ADDED, TYPE_ENTRY_ADDED);

        communicationMessage.addData(TABLE, table);
        communicationMessage.addData(ENTRY, entry.toJSON().toString());
        communicationMessage.addData(RELATED_GROUPS, relatedGroup.toJSON().toString());

        return communicationMessage;

    }

    /**
     * methode créant un message de signalement de message créé
     *
     * @param table
     * @param entry
     * @param relatedGroup  - groupe contenant le ticket sur lequel le message sera créé
     * @param relatedTicket - ticket sur lequel le message va être posté
     * @return le message créé
     **/
    public static CommunicationMessage createMessageAddedMessage
    (final String table, Message entry, Groupe relatedGroup, Ticket relatedTicket) {

        CommunicationMessage communicationMessage = new CommunicationMessage(MESSAGE_TYPE.ENTRY_ADDED, TYPE_ENTRY_ADDED);

        communicationMessage.addData(TABLE, table);
        communicationMessage.addData(ENTRY, entry.toJSON().toString());
        communicationMessage.addData(RELATED_GROUPS, relatedGroup.toJSON().toString());
        communicationMessage.addData(RELATED_TICKETS, relatedTicket.toJSON().toString());

        return communicationMessage;

    }

    /**
     * methode créant un message de signalement de mise à jour d'un entrée
     *
     * @param table - table concernée
     * @param entry - entrée modifiée
     * @return le message créé
     **/
    public static CommunicationMessage createEntryUpdatedMessage(final String table, ProjectTable entry) {

        CommunicationMessage communicationMessage = new CommunicationMessage(MESSAGE_TYPE.ENTRY_UPDATED, TYPE_ENTRY_UPDATED);

        communicationMessage.addData(TABLE, table);
        communicationMessage.addData(ENTRY, entry.toJSON().toString());

        return communicationMessage;

    }

    /**
     * methode créant un message de signalement de mise à jour d'un ticket
     *
     * @param table        - table concernée
     * @param entry        - ticket modifiée
     * @param relatedGroup - groupe lié au ticket modifié
     * @return le message créé
     **/
    public static CommunicationMessage createTicketUpdatedMessage(final String table, Ticket entry, Groupe relatedGroup) {

        CommunicationMessage communicationMessage = new CommunicationMessage(MESSAGE_TYPE.ENTRY_UPDATED, TYPE_ENTRY_UPDATED);

        communicationMessage.addData(TABLE, table);
        communicationMessage.addData(ENTRY, entry.toJSON().toString());
        communicationMessage.addData(RELATED_GROUPS, relatedGroup.toJSON().toString());

        return communicationMessage;

    }

    /**
     * methode créant un message de signalement de mise à jour d'un message
     *
     * @param table        - table concernée
     * @param entry        - message modifé
     * @param relatedGroup - groupe lié au ticket sur lequel le message est modifié
     * @param ticket       - ticket sur lequel le message est modifié
     * @return le message créé
     **/
    public static CommunicationMessage createMessageUpdatedMessage(final String table, Message entry, Groupe relatedGroup, Ticket ticket) {

        CommunicationMessage communicationMessage = new CommunicationMessage(MESSAGE_TYPE.ENTRY_UPDATED, TYPE_ENTRY_UPDATED);

        communicationMessage.addData(TABLE, table);
        communicationMessage.addData(ENTRY, entry.toJSON().toString());
        communicationMessage.addData(RELATED_GROUPS, relatedGroup.toJSON().toString());
        communicationMessage.addData(RELATED_TICKETS, ticket.toJSON().toString());

        return communicationMessage;

    }

    /**
     * methode créant un message pour demander au serveur de supprimer une entrée
     *
     * @param table - table concernée
     * @param entry - entrée supprimée
     * @return le message créé
     **/
    public static CommunicationMessage createDeleteMessage(final String table, ProjectTable entry) {

        CommunicationMessage message = new CommunicationMessage(MESSAGE_TYPE.DELETE, TYPE_DELETE);

        message.addData(TABLE, table);
        message.addData(ENTRY, entry.toJSON().toString());

        return message;

    }

    /**
     * methode créant un message pour demander au serveur de créér une entrée
     *
     * @param table - table concernée
     * @param entry - entrée créée
     * @return le message créé
     **/
    public static CommunicationMessage createAddMessage(final String table, ProjectTable entry) {

        CommunicationMessage message = new CommunicationMessage(MESSAGE_TYPE.ADD, TYPE_ADD);

        message.addData(TABLE, table);
        message.addData(ENTRY, entry.toJSON().toString());

        return message;

    }

    /**
     * methode créant un message pour demander au serveur de modifier une entrée
     *
     * @param table - table concernée
     * @param entry - entrée modifiée
     * @return le message créé
     **/
    public static CommunicationMessage createUpdateMessage(final String table, ProjectTable entry) {

        CommunicationMessage message = new CommunicationMessage(MESSAGE_TYPE.UPDATE, TYPE_UPDATE);

        message.addData(TABLE, table);
        message.addData(ENTRY, entry.toJSON().toString());

        return message;

    }

    /**
     * methode créant un message de demande des tables par un administrateur
     *
     * @return le message de demande
     **/
    public static CommunicationMessage createTableModelRequest() {

        CommunicationMessage message = new CommunicationMessage(MESSAGE_TYPE.TABLE_MODEL_REQUEST, TYPE_TABLE_MODEL_REQUEST);

        return message;

    }

    /**
     * methode créant un message de demande au serveur de renvoi de tout ce qu'il reçoit à un administrateur
     *
     * @return le message de requète
     **/
    public static CommunicationMessage createRequestEverything() {

        CommunicationMessage message = new CommunicationMessage(MESSAGE_TYPE.REQUEST_EVERYTHING, TYPE_REQUEST_EVERYTHING);

        return message;

    }

    /**
     * methode créant un message d'attestation de reception de messages
     *
     * @param received - liste de messages reçus
     * @return le message d'acquittement
     **/
    public static CommunicationMessage createMessageReceived(ArrayList<Message> received) {

        CommunicationMessage message = new CommunicationMessage(MESSAGE_TYPE.MESSAGE_RECEIVED, TYPE_MESSAGE_RECEIVED);

        JSONArray array = new JSONArray();
        for (Message s : received) {
            array.put(s.toJSON());
        }

        message.addData(MESSAGE_RECEIVED, array);

        return message;

    }

    /**
     * methode créant un message contenant les tables de la base de donnée (sauf les tables des associations)
     *
     * @param users    - liste d'utilisateurs
     * @param groups   - liste de groupes
     * @param tickets  - liste de tickets
     * @param messages - liste de messages
     * @return le message contenant tout
     **/
    public static CommunicationMessage createTableModel
    (List<Utilisateur> users, List<Groupe> groups, List<Ticket> tickets, List<Message> messages) {

        CommunicationMessage message = new CommunicationMessage(MESSAGE_TYPE.TABLE_MODEL, TYPE_TABLE_MODEL);

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

    /**
     * methode permettant d'ajouter des informations sur un message
     *
     * @param key  - clé qui fera référence aux données ajoutées
     * @param data - données à ajouter
     **/
    protected void addData(String key, JSONArray data) {
        this.data.put(key, data);
    }
    
    /**
     * Accesseur sur les données d'un message
     *
     * @return les données du message
    **/
    protected JSONObject getData() {
        return data;
    }
    
    /**
     * Mutateur sur les données d'un message
     * 
     * @param data - nouvelles données à placer dans le message
    **/
    protected void setData(JSONObject data) {
        this.data = data;
    }
    
    /**
     * Accesseur sur le type du message
     *
     * @return le type du message
     **/
    protected String getTypeToString() {
        return type;
    }

    /**
     * Mutateur sur le type du message 
     *
     * @param type - le nouveau type du message
    **/
    protected void setTypeString(String type) {
        this.type = type;
    }
    
    /**
     * methode ajoutant des données (sous forme de string) au message 
     *
     * @param key - clé liée aux données ajoutées
     * @param data - données à ajouter sous forme de chaine de caractères
    **/
    protected void addData(String key, String data) {
        this.data.put(key, data);
    }
    
   /**
     * methode ajoutant des données (sous forme d'octets) au message 
     *
     * @param key - clé liée aux données ajoutées
     * @param data - données à ajouter sous forme de tableau d'octet 
    **/
    protected void addData(String key, byte[] data) {
        this.data.put(key, data);
    }

    /**
     * methode verifiant si les données sont valides pour devenir un objet JSON
     *
     * @param data - données à évlauer
     * @return true si on peut coder les données au format JSON , false sinon
    **/
    protected Boolean isValidJSON(String data) {
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

    public String toString() {
        JSONObject result = new JSONObject();
        result.put(TYPE, getTypeToString());
        result.put(DATA, data);

        return result.toString() + "\n";
    }
    
    /**
     * traduit le message sous forme JSONObject en un String formaté via la fonction format
     *
     * @return Une chaine de caractères formatée contenant les informations du message 
    **/
    public String toFormattedString() {
        JSONObject result = new JSONObject();
        result.put(TYPE, getTypeToString());
        result.put(DATA, data);

        return format("", result, 0);
    }

    /**
     * formate une clé, un objet au format JSON en une chaine de caractère formatée
     * 
     * @param key - clé 
     * @param object - objet au format JSON à formater
     * @param padding - tabulation
     * @return une chaine de caractères contenant les information 
     **/
    private String format(final String key, JSONObject object, int padding) {
        final String pad = new String(new char[padding]).replace("\0", "  ");

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
                final String oString = o.toString();
                if (oString.length() > 100) {
                    builder.append(pad).append("  ").append(s).append(": ")
                            .append(oString, 0, 50)
                            .append(" [...] ")
                            .append(oString.substring(oString.length() - 50));
                } else {
                    builder.append(pad).append("  ").append(s).append(": ").append(oString);
                }
            }

            if ((++position) != object.keySet().size()) {
                builder.append(",");
            }

            builder.append("\n");
        }

        builder.append(pad).append("}");

        return builder.toString();
    }

    /**
     * formate une clé, un tableau JSON en une chaine de caractère formatée
     *
     * @param key - clé 
     * @param array - tableau JSON à formater
     * @param padding - tabulation
     * @return une chaine de caractères contenant les information 
     **/
    private String format(final String key, JSONArray array, int padding) {
        final String pad = new String(new char[padding]).replace("\0", "  ");

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
                final String oString = o.toString();
                if (oString.length() > 100) {
                    builder.append(pad).append("  ")
                            .append(oString, 0, 50)
                            .append(" [...] ")
                            .append(oString.substring(oString.length() - 50));
                } else {
                    builder.append(pad).append("  ").append(oString);
                }
            }

            if (i + 1 < array.length()) {
                builder.append(",");
            }

            builder.append("\n");
        }
        builder.append(pad).append("]");

        return builder.toString();
    }
    
    /**
     * methode verifiant si un objet JSON contient bien un type et des données
     *
     * @param data - objet au format JSON à vérifier
     * @return true si l'objet possède un type et des données, false sinon
    **/
    protected Boolean isValid(final JSONObject data) {
        return data.has(TYPE) && data.has(DATA);
    }
    
    /**
     * Accesseur sur la table stockée sur les données du message
     *
     * @return une chaine de caractères étant la table 
    **/
    public String getTable() {
        return getData().getString(TABLE);
    }
    
    /**
     * Accesseur sur les entrées stockées sur les données du message
     *
     * @retunr les entrées sosu forme de JSONObject
    **/
    public JSONObject getEntryAsJSON() {
        return new JSONObject(getData().getString(ENTRY));
    }
    
    /**
     * methode de relayage permettant de recupérer les instances de table en fonction du nom de la table
     *
     * @return null si aucun nom n'est connnu, l'instance correspondante sinon
    **/
    public ProjectTable getEntry() {
        switch (getTable()) {
            case TABLE_NAME_UTILISATEUR:
                return getEntryAsUtilisateur();

            case TABLE_NAME_GROUPE:
                return getEntryAsGroupe();

            case TABLE_NAME_TICKET:
                return getEntryAsTicket();

            case TABLE_NAME_MESSAGE:
                return getEntryAsMessage();

            default:
                return null;
        }
    }
    
    /**
     * methode retournant un utilisateur en fonction d'un entrée de table
     *
     * @return utilisateur généré
    **/
    public Utilisateur getEntryAsUtilisateur() {
        return new Utilisateur(getEntryAsJSON());
    }
    
    /**
     * methode retournant un groupe en fonction d'un entrée de table
     *
     * @return groupe généré
    **/
    public Groupe getEntryAsGroupe() {
        return new Groupe(getEntryAsJSON());
    }

    /**
     * methode retournant un ticket en fonction d'un entrée de table
     *
     * @return ticket généré
    **/
    public Ticket getEntryAsTicket() {
        return new Ticket(getEntryAsJSON());
    }

    /**
     * methode retournant un message en fonction d'un entrée de table
     *
     * @return message généré
     **/
    public Message getEntryAsMessage() {
        return new Message(getEntryAsJSON());
    }

    /**
     * methode appelant getTypeToString() et renvoyant le type d'un message
     *
     * @return le type du message
     * @throws InvalidMessageException - peut être renvoyé si le message n'a pas de type connu
     **/
    protected MESSAGE_TYPE guessType() throws InvalidMessageException {
        switch (getTypeToString()) {
            case TYPE_KEY_XCHANGE:
                return MESSAGE_TYPE.KEYXCHANGE;

            case TYPE_CONNECTION:
                checkForConnectionValidity();
                return MESSAGE_TYPE.CONNECTION;

            case TYPE_MESSAGE:
                checkForMessageValidity();
                return MESSAGE_TYPE.MESSAGE;

            case TYPE_RESPONSE:
                checkForResponseValidity();
                return MESSAGE_TYPE.RESPONSE;

            case TYPE_TICKET:
                checkForTicketValidity();
                return MESSAGE_TYPE.TICKET;

            case TYPE_LOCAL_UPDATE:
                checkForLocalUpdateValidity();
                return MESSAGE_TYPE.LOCAL_UPDATE;

            case TYPE_LOCAL_UPDATE_RESPONSE:
                checkForLocalUpdateResponseValidity();
                return MESSAGE_TYPE.LOCAL_UPDATE_RESPONSE;

            case TYPE_TICKET_CLICKED:
                checkForTicketClickedValidity();
                return MESSAGE_TYPE.TICKET_CLICKED;

            case TYPE_ENTRY_ADDED:
                checkForEntryAddedValidity();
                return MESSAGE_TYPE.ENTRY_ADDED;

            case TYPE_ENTRY_DELETED:
                checkForEntryDeletedValidity();
                return MESSAGE_TYPE.ENTRY_DELETED;

            case TYPE_ENTRY_UPDATED:
                checkForEntryUpdatedValidity();
                return MESSAGE_TYPE.ENTRY_UPDATED;

            case TYPE_DELETE:
                checkForDeleteValidity();
                return MESSAGE_TYPE.DELETE;

            case TYPE_ADD:
                checkForAddValidity();
                return MESSAGE_TYPE.ADD;

            case TYPE_UPDATE:
                checkForUpdateValidity();
                return MESSAGE_TYPE.UPDATE;

            case TYPE_TABLE_MODEL:
                checkForTableModelValidity();
                return MESSAGE_TYPE.TABLE_MODEL;

            case TYPE_TABLE_MODEL_REQUEST:
                return MESSAGE_TYPE.TABLE_MODEL_REQUEST;

            case TYPE_REQUEST_EVERYTHING:
                return MESSAGE_TYPE.REQUEST_EVERYTHING;

            case TYPE_MESSAGE_RECEIVED:
                return MESSAGE_TYPE.MESSAGE_RECEIVED;


            default:
                throw new InvalidMessageException("Message with invalid type: " + getTypeToString());
        }
    }

    /**
     * methode de verification des tables dans la requête des tables
     *
     * @throws InvalidMessageException si les tables n'ont pas le bon nom (Message, utilisateur, Ticket ou Groupe)
     **/
    private void checkForTableModelValidity() throws InvalidMessageException {
        if (!getData().has(TABLE_NAME_GROUPE) || !getData().has(TABLE_NAME_MESSAGE)
                || !getData().has(TABLE_NAME_TICKET) || !getData().has(TABLE_NAME_UTILISATEUR)) {
            throw new InvalidMessageException("Missing field in table model request message");
        }
    }

    /**
     * methode de verification des message de modification
     *
     * @throws InvalidMessageException si le message ne contient pas les champs Table ou Entry
     **/
    private void checkForUpdateValidity() throws InvalidMessageException {
        if (!getData().has(TABLE) || !getData().has(ENTRY)) {
            throw new InvalidMessageException("Missing field in update message");
        }
    }

    /**
     * methode de verification des messages d'ajout
     *
     * @throws InvalidMessageException si le message ne continent pas les champs Table ou Entry
     **/
    private void checkForAddValidity() throws InvalidMessageException {
        if (!getData().has(TABLE) || !getData().has(ENTRY)) {
            throw new InvalidMessageException("Missing field in add message");
        }
    }

    /**
     * methode de vérification des message de suppression
     *
     * @throws InvalidMessageException si le message ne contient pas les champs Table ou Entry
     **/
    private void checkForDeleteValidity() throws InvalidMessageException {
        if (!getData().has(TABLE) || !getData().has(ENTRY)) {
            throw new InvalidMessageException("Missing field in delete message");
        }
    }

    /**
     * methode de verification des messages de signalement d'ajout
     *
     * @throws InvalidMessageException si le message ne contient pas les champs Table ou Entry
     **/
    private void checkForEntryAddedValidity() throws InvalidMessageException {
        if (!getData().has(TABLE) || !getData().has(ENTRY)) {
            throw new InvalidMessageException("Missing field in added entry message");
        }
    }

    /**
     * methode de vérification des messages de signalement de suppression
     *
     * @throws InvalidMessageException si le message ne contient pas les champs Table ou Entry
     **/
    private void checkForEntryDeletedValidity() throws InvalidMessageException {
        if (!getData().has(TABLE) || !getData().has(ENTRY)) {
            throw new InvalidMessageException("Missing field in deleted entry message");
        }
    }

    /**
     * methode de vérifcation des messages de signalement de modification
     *
     * @throws InvalidMessageException si le message ne contient pas les champs Table ou Entry
     **/
    private void checkForEntryUpdatedValidity() throws InvalidMessageException {
        if (!getData().has(TABLE) || !getData().has(ENTRY)) {
            throw new InvalidMessageException("Missing field in updated entry message");
        }
    }

    /**
     * methode de vérification des messages de connexion
     *
     * @throws InvalidMessageException si le message ne contient pas les champs Ine et Mot de passe
     **/
    private void checkForConnectionValidity() throws InvalidMessageException {
        if (!getData().has(CONNECTION_INE) || !getData().has(CONNECTION_PASSWORD)) {
            throw new InvalidMessageException("Missing field in connection Message : \n" + getData().toString() + "\n" + toFormattedString());
        }
    }

    /**
     * methode de vérification des messages (Message)
     *
     * @throws InvalidMessageException si le message ne contient pas les champs ID_Ticket ou Contenu
     **/
    private void checkForMessageValidity() throws InvalidMessageException {
        if (!getData().has(MESSAGE_TICKET_ID) || !getData().has(MESSAGE_CONTENTS)) {
            throw new InvalidMessageException("Missing field in message type Message");
        }
    }

    /**
     * methode de vérification des messages de réponse
     *
     * @throws InvalidMessageException si le message ne contient pas le champ response_value
     **/
    private void checkForResponseValidity() throws InvalidMessageException {
        if (!getData().has(RESPONSE_VALUE)) {
            throw new InvalidMessageException("Missing field in response Message");
        }

        if (getData().getString(RESPONSE_VALUE).equals(RESPONSE_ERROR) && !getData().has(RESPONSE_REASON)) {
            throw new InvalidMessageException("Missing reason in response Message");
        }
    }

    /**
     * methode de vérification des messages (Ticket)
     *
     * @throws InvalidMessageException si le message ne contient pas les champs titre, groupe ou message
     **/
    private void checkForTicketValidity() throws InvalidMessageException {
        if (!getData().has(TICKET_TITLE) || !getData().has(TICKET_GROUP) || !getData().has(TICKET_MESSAGE)) {
            throw new InvalidMessageException("Missing field in ticket Message");
        }
    }

    /**
     * methode de vérification des message de demande de mise à jour
     *
     * @throws InvalidMessageException si le message ne contient pas le champ date de mise à jour
     **/
    private void checkForLocalUpdateValidity() throws InvalidMessageException {
        if (!getData().has(LOCAL_UPDATE_DATE)) {
            throw new InvalidMessageException("Missing field in update message");
        }
    }

    /**
     * methode de vérification des messages de retour de mise à jour
     *
     * @throws InvalidMessageException si le message ne contient pas les champs all_groups et related_groups
     **/
    private void checkForLocalUpdateResponseValidity() throws InvalidMessageException {
        if (!getData().has(ALL_GROUPS) || !getData().has(RELATED_GROUPS)) {
            throw new InvalidMessageException("Missing field in update message");
        }
    }

    /**
     * methode de vérification du message de signalement de ticket séléctionné
     *
     * @throws InvalidMessageException si le message ne contient pas l'id du ticket selectionné
     **/
    private void checkForTicketClickedValidity() throws InvalidMessageException {
        if (!getData().has(TICKET_CLICKED_ID)) {
            throw new InvalidMessageException("Missing field in ticket clicked message");
        }
    }

    /**
     * methode vérifiant si le message est un ACK
     *
     * @return true si le message est un ACK, false sinon
     **/
    public Boolean isAck() {
        return getType().equals(MESSAGE_TYPE.RESPONSE) && getData().getString(RESPONSE_VALUE).equals(RESPONSE_SUCCESS);
    }

    /**
     * methode vérifiant si le message est un NACK
     *
     * @return true si le message est un NACK, false sinon
     **/
    public Boolean isNack() {
        return getType().equals(MESSAGE_TYPE.RESPONSE) && getData().getString(RESPONSE_VALUE).equals(RESPONSE_ERROR);
    }

    /**
     * methode vérifiant si le message est un message de connection
     *
     * @return true si le message est un message de connction, false sinon
     **/
    public Boolean isConnection() {
        return getType().equals(MESSAGE_TYPE.CONNECTION);
    }

    /**
     * methode vérifiant si le message a un type ticket
     *
     * @return true si le message a un type ticket, false sinon
     **/
    public Boolean isTicket() {
        return getType().equals(MESSAGE_TYPE.TICKET);
    }

    /**
     * methode vérifiant si le message a un type message
     *
     * @return true si le message a un type message , false sinon
     **/
    public Boolean isMessage() {
        return getType().equals(MESSAGE_TYPE.MESSAGE);
    }

    /**
     * methode vérifiant si le message est un message de retour sur mise à jour
     *
     * @return true si le message est un message de retour sur mise à jour, false sinon
     **/
    public Boolean isLocalUpdateResponse() {
        return getType().equals(MESSAGE_TYPE.LOCAL_UPDATE_RESPONSE);
    }

    /**
     * methode renvoyant le type d'un message
     *
     * @return le type du message
     **/
    public MESSAGE_TYPE getType() {
        return CLASSICMESSAGE_type;
    }

    /**
     * methode renvoyant la raison d'un Nack
     *
     * @return la raison du nack
     * @throws WrongMessageTypeException peut être renvoyé si le message sur lequel on recupère le rapport de nack n'est pas un nack
     **/
    public String getNackReason() throws WrongMessageTypeException {
        if (!isNack()) {
            throw new WrongMessageTypeException("Requiring Nack reason on a non-nack message");
        }

        return getData().getString(RESPONSE_REASON);
    }

    /**
     * accesseur sur l'INE d'un message de connection
     *
     * @return l'INE du message
     **/
    public String getConnectionINE() {
        return getData().getString(CONNECTION_INE);
    }

    /**
     * accesseur sur le mot de passe d'un message de connection
     *
     * @return le mot de passe du message
     **/
    public String getConnectionPassword() {
        return getData().getString(CONNECTION_PASSWORD);
    }

    /**
     * accesseur sur le titre du ticket d'un message de type ticket
     *
     * @return le titre du ticket du message
     **/
    public String getTicketTitle() {
        return getData().getString(TICKET_TITLE);
    }

    /**
     * accesseur sur le groupe d'un message de type ticket
     *
     * @return le groupe du message
     **/
    public String getTicketGroup() {
        return getData().getString(TICKET_GROUP);
    }

    /**
     * accesseur sur le ticket d'un message de type message
     *
     * @return le ticket sur le message
     **/
    public String getTicketMessage() {
        return getData().getString(TICKET_MESSAGE);
    }

    /**
     * accesseur sur le contenu d'un message de type message
     *
     * @return le contenu du message sur le message
     **/
    public String getMessageContents() {
        return getData().getString(MESSAGE_CONTENTS);
    }

    /**
     * accesseur sur l'ID du ticket d'un message
     *
     * @return l'ID du ticket sur le message
     **/
    public Long getMessageTicketID() {
        return getData().getLong(MESSAGE_TICKET_ID);
    }

    /**
     * accesseur sur l'id du ticket séléctionné d'un message type selected_ticket
     *
     * @retrun l'id du ticket
     **/
    public Long getTicketClickedID() {
        return getData().getLong(TICKET_CLICKED_ID);
    }

    /**
     * accesseur sur les groupes (liés à un utilisateur) d'un message de compte rendu de mise à jour
     *
     * @return un ensemble trié des groupes
     **/
    public TreeSet<Groupe> getLocalUpdateResponseRelatedGroups() {
        TreeSet<Groupe> groups = new TreeSet<>();
        JSONArray array = getData().getJSONArray(RELATED_GROUPS);
        for (int i = 0; i < array.length(); ++i) {
            groups.add(new Groupe(array.getJSONObject(i)));
        }

        return groups;
    }

    /**
     * accesseur sur les groupes (tous) d'un message de compte rendu de mise à jour
     *
     * @return un ensemble trié des groupes
     **/
    public TreeSet<String> getLocalUpdateResponseAllGroups() {
        TreeSet<String> groups = new TreeSet<>();
        JSONArray array = getData().getJSONArray(ALL_GROUPS);
        for (int i = 0; i < array.length(); ++i) {
            groups.add(array.getString(i));
        }

        return groups;
    }

    /**
     * accesseur sur les utilisateurs (tous) d'un message de compte rendu de mise à jour
     *
     * @return un ensemble trié d'utilisateurs
     **/
    public TreeSet<Utilisateur> getLocalUpdateResponseUsers() {
        TreeSet<Utilisateur> users = new TreeSet<>();
        JSONArray array = getData().getJSONArray(USERS);
        for (int i = 0; i < array.length(); ++i) {
            users.add(new Utilisateur(array.getJSONObject(i)));
        }

        return users;
    }

    /**
     * accesseur sur le groupe lié à une entrée sur un message
     *
     * @return le groupe lié
     **/
    public Groupe getEntryRelatedGroup() {
        return new Groupe(new JSONObject(getData().getString(RELATED_GROUPS)));
    }

    /**
     * accesseur sur le ticket lié à une entrée sur un message
     *
     * @return le ticket lié
     **/
    public Ticket getEntryRelatedTicket() {
        System.out.println(getData().getString(RELATED_TICKETS));
        return new Ticket(new JSONObject(getData().getString(RELATED_TICKETS)));
    }

    /**
     * accesseur sur les utilisateurs d'un message contenant toutes les tables
     *
     * @return tous les utilisateurs
     **/
    public UserModel getTableModelUserModel() {
        JSONArray array = getData().getJSONArray(TABLE_NAME_UTILISATEUR);
        List<Utilisateur> users = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            users.add(new Utilisateur(array.getJSONObject(i)));
        }

        return new UserModel(users);
    }

    /**
     * accesseur sur les groupes d'un message contenant toutes les tables
     *
     * @return tous groupes
     **/
    public GroupModel getTableModelGroupModel() {
        JSONArray array = getData().getJSONArray(TABLE_NAME_GROUPE);
        List<Groupe> groups = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            groups.add(new Groupe(array.getJSONObject(i)));
        }

        return new GroupModel(groups);
    }

    /**
     * accesseur sur tickets d'un message contenant toutes les tables
     *
     * @return tous tickets
     **/
    public TicketModel getTableModelTicketModel() {
        JSONArray array = getData().getJSONArray(TABLE_NAME_TICKET);
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            tickets.add(new Ticket(array.getJSONObject(i)));
        }

        return new TicketModel(tickets);
    }

    /**
     * accesseur sur les messages d'un message contenant toutes les tables
     *
     * @return tous les messages
     **/
    public MessageModel getTableModelMessageModel() {
        JSONArray array = getData().getJSONArray(TABLE_NAME_MESSAGE);
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            messages.add(new Message(array.getJSONObject(i)));
        }

        return new MessageModel(messages);
    }

    /**
     * accesseur sur la liste de messages reçus d'un message d'acquittement de reception
     *
     * @return la  liste de messages reçus
     **/
    public ArrayList<Message> getMessagesReceived() {
        JSONArray array = getData().getJSONArray(MESSAGE_RECEIVED);
        ArrayList<Message> received = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            received.add(new Message(array.getJSONObject(i)));
        }

        return received;
    }

    public boolean containsRelatedGroup() {
        return getData().has(RELATED_GROUPS);
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
