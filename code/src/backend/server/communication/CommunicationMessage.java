package backend.server.communication;

import backend.data.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static backend.database.Keys.*;

public abstract class CommunicationMessage {

    public static final String TYPE = "type";
    public static final String DATA = "data";

    public final static String TABLE = "table";
    public final static String ENTRY = "entry";

    private String type;
    private JSONObject data = new JSONObject();


    protected void addData(String key, JSONArray data) {
        this.data.put(key, data);
    }

    protected JSONObject getData() {
        return data;
    }

    protected void setData(JSONObject data) {
        this.data = data;
    }

    protected String getTypeToString() {
        return type;
    }

    protected void setTypeString(String type) {
        this.type = type;
    }

    protected void addData(String key, String data) {
        this.data.put(key, data);
    }

    protected void addData(String key, byte[] data) {
        this.data.put(key, data);
    }


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

    public String toFormattedString() {
        JSONObject result = new JSONObject();
        result.put(TYPE, getTypeToString());
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

    protected Boolean isValid(final JSONObject data) {
        return data.has(TYPE) && data.has(DATA);
    }

    public String getTable() {
        return getData().getString(TABLE);
    }

    public JSONObject getEntryAsJSON() {
        return new JSONObject(getData().getString(ENTRY));
    }

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

    public Utilisateur getEntryAsUtilisateur() {
        return new Utilisateur(getEntryAsJSON());
    }

    public Groupe getEntryAsGroupe() {
        return new Groupe(getEntryAsJSON());
    }

    public Ticket getEntryAsTicket() {
        return new Ticket(getEntryAsJSON());
    }

    public Message getEntryAsMessage() {
        return new Message(getEntryAsJSON());
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
