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

    /**
     * methode permettant d'ajouter des informations sur un message
     * 
     * @param key - clé qui fera référence aux données ajoutées
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
     * @param le nouveau type du message
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
    /**
     * formate une clé, un tableau JSON en une chaine de caractère formatée
     * 
     * @param key - clé 
     * @param object - tableau JSON à formater
     * @param padding - tabulation
     * @return une chaine de caractères contenant les information 
    **/
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
