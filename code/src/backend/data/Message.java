package backend.data;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.TreeSet;

import static backend.database.Keys.*;

public class Message extends ProjectTable implements Comparable<Message> {

    private Long mID;
    private Long mUtilisateurID;
    private Long mTicketID;
    private Date mHeureEnvoie;
    private String mContenu;
    private String mUtilisateur;
    private TreeSet<String> mHaveToRead;


    public Message(Long id, Long utilisateurID, Long ticketID, Date date, String contenu, TreeSet<String> haveToRead) {
        mID = id;
        mUtilisateurID = utilisateurID;
        mTicketID = ticketID;
        mHeureEnvoie = date;
        mContenu = contenu;
        mHaveToRead = haveToRead;
    }


    public Message(ResultSet set, TreeSet<String> haveToRead) throws SQLException {
        mID = set.getLong(1);
        mUtilisateurID = set.getLong(5);
        mTicketID = set.getLong(4);
        mHeureEnvoie = set.getTimestamp(3);
        mContenu = set.getString(2);
        mHaveToRead = haveToRead;
    }

    public Message(JSONObject json) {
        mID = json.getLong(MESSAGE_ID);
        mUtilisateurID = json.getLong(MESSAGE_UTILISATEUR_ID);
        mTicketID = json.getLong(MESSAGE_TICKET_ID);
        mHeureEnvoie = new Date(json.getLong(MESSAGE_HEURE_ENVOIE));
        mContenu = json.getString(MESSAGE_CONTENU);

        mHaveToRead = new TreeSet<>();
        JSONArray array = json.getJSONArray("have_to_read");
        for (int i = 0; i < array.length(); ++i) {
            mHaveToRead.add(array.getString(i));
        }
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        json.put(MESSAGE_ID, getID());
        json.put(MESSAGE_UTILISATEUR_ID, getUtilisateurID());
        json.put(MESSAGE_HEURE_ENVOIE, getHeureEnvoie().getTime());
        json.put(MESSAGE_CONTENU, getContenu());
        json.put(MESSAGE_TICKET_ID, getTicketID());

        JSONArray array = new JSONArray();
        for (String s : mHaveToRead) {
            array.put(s);
        }

        json.put("have_to_read", array);

        return json;
    }

    public Long getID() {
        return mID;
    }

    public Long getUtilisateurID() {
        return mUtilisateurID;
    }

    public Long getTicketID() {
        return mTicketID;
    }

    public Date getHeureEnvoie() {
        return mHeureEnvoie;
    }

    public String getContenu() {
        return mContenu;
    }

    public int state() {
        if (mHaveToRead == null) {
            return 0;
        } else if (mHaveToRead.size() > 0) {
            return 3;
        } else {
            return 4;
        }
    }

    public String getFormattedState() {
        if (mHaveToRead != null) {
            if (mHaveToRead.isEmpty()) {
                return "Tous les utilisateurs ont vus ce message.";
            }

            StringBuilder builder = new StringBuilder();
            builder.append("Doit être lu par:\n");
            for (String s : mHaveToRead) {
                builder.append("-").append(s).append("\n");
            }

            return builder.toString();
        }

        return "Aucune info disponible sur ce message.";
    }

    @Override
    public int compareTo(@NotNull Message message) {
        if (this.getID().compareTo(message.getID()) == 0) {
            return 0;
        }
        return this.getHeureEnvoie().compareTo(message.getHeureEnvoie());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Message) {
            return getID().equals(((Message) obj).getID());
        }

        return false;
    }
}
