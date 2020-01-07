package backend.data;

import debug.Debugger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import static backend.database.Keys.*;

public class Message extends ProjectTable implements Comparable<Message> {

    private Long mID;
    private Long mUtilisateurID;
    private Long mTicketID;
    private Date mHeureEnvoie;
    private String mContenu;
    private String mUtilisateur;
    private ArrayList<String> mHaveToRead;
    private ArrayList<String> mHaveToReceive;


    public Message(Long id, Long utilisateurID, Long ticketID, Date date, String contenu, ArrayList<String> haveToRead, ArrayList<String> haveToReceive) {
        mID = id;
        mUtilisateurID = utilisateurID;
        mTicketID = ticketID;
        mHeureEnvoie = date;
        mContenu = contenu;
        mHaveToRead = haveToRead;
        mHaveToReceive = haveToReceive;
    }


    public Message(ResultSet set, ArrayList<String> haveToRead, ArrayList<String> haveToReceive) throws SQLException {
        mID = set.getLong(1);
        mUtilisateurID = set.getLong(5);
        mTicketID = set.getLong(4);
        mHeureEnvoie = set.getTimestamp(3);
        mContenu = set.getString(2);
        mHaveToRead = haveToRead;
        mHaveToReceive = haveToReceive;
    }

    public Message(JSONObject json) {
        mID = json.getLong(MESSAGE_ID);
        mUtilisateurID = json.getLong(MESSAGE_UTILISATEUR_ID);
        mTicketID = json.getLong(MESSAGE_TICKET_ID);
        mHeureEnvoie = new Date(json.getLong(MESSAGE_HEURE_ENVOIE));
        mContenu = json.getString(MESSAGE_CONTENU);

        mHaveToRead = new ArrayList<>();
        JSONArray array = json.getJSONArray("have_to_read");
        for (int i = 0; i < array.length(); ++i) {
            mHaveToRead.add(array.getString(i));
        }

        mHaveToReceive = new ArrayList<>();
        array = json.getJSONArray("have_to_receive");
        for (int i = 0; i < array.length(); ++i) {
            mHaveToReceive.add(array.getString(i));
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


        array = new JSONArray();
        for (String s : mHaveToReceive) {
            array.put(s);
        }
        json.put("have_to_receive", array);

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
        if (mHaveToRead == null || mHaveToReceive == null) {
            return 1;
        } else if (mHaveToReceive.size() > 0) {
            return 2;
        } else if (mHaveToRead.size() > 0) {
            return 3;
        } else {
            return 4;
        }
    }

    public String getFormattedState() {
        Debugger.logMessage("Utilisateur", "Non recus : " + mHaveToReceive + "\n" +
                "Non vus : " + mHaveToRead);
        StringBuilder builder = new StringBuilder();

        if (mHaveToReceive != null) {
            if (mHaveToReceive.isEmpty()) {
                builder.append("Tous les utilisateurs ont reçu ce message.\n");
            } else {
                builder.append("Doit être reçu par:\n");
                for (String s : mHaveToReceive) {
                    builder.append("-> ").append(s).append("\n");
                }
            }
        }

        if (mHaveToRead != null) {
            if (mHaveToRead.isEmpty()) {
                builder.append("Tous les utilisateurs ont vus ce message.\n");
            } else {
                builder.append("Doit être lu par:\n");
                for (String s : mHaveToRead) {
                    builder.append("-> ").append(s).append("\n");
                }
            }
        }

        return builder.toString();
    }

    @Override
    public int compareTo(@NotNull Message message) {
        int idComparison = this.getID().compareTo(message.getID());
        if (idComparison == 0) {
            return 0;
        }

        int dateComparison = this.getHeureEnvoie().compareTo(message.getHeureEnvoie());
        return (dateComparison == 0 ? idComparison : dateComparison);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Message) {
            return getID().equals(((Message) obj).getID());
        }

        return false;
    }
}
