package backend.data;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

import static backend.database.Keys.*;

public class Message extends ProjectTable implements Comparable<Message> {

    private Long mID;
    private Long mUtilisateurID;
    private Long mTicketID;
    private Date mHeureEnvoie;
    private String mContenu;


    public Message(Long id, Long utilisateurID, Long ticketID, Date date, String contenu) {
        mID = id;
        mUtilisateurID = utilisateurID;
        mTicketID = ticketID;
        mHeureEnvoie = date;
        mContenu = contenu;
    }


    public Message(ResultSet set) throws SQLException {
        mID = set.getLong(1);
        mUtilisateurID = set.getLong(5);
        mTicketID = set.getLong(4);
        mHeureEnvoie = set.getDate(3);
        mContenu = set.getString(2);
    }

    public static Message fromJSON(JSONObject json) {
        Long id = json.getLong(MESSAGE_ID);
        Long utilisateurID = json.getLong(MESSAGE_UTILISATEUR_INE);
        Long ticketID = json.getLong(MESSAGE_TICKET_ID);
        Date date = new Date(json.getLong(MESSAGE_HEURE_ENVOIE));
        String contenu = json.getString(MESSAGE_CONTENU);

        return new Message(id, ticketID, utilisateurID, date, contenu);
    }

    public static JSONObject toJSON(Message message) {
        JSONObject json = new JSONObject();

        json.put(MESSAGE_ID, message.getID());
        json.put(MESSAGE_UTILISATEUR_INE, message.getUtilisateurINE());
        json.put(MESSAGE_HEURE_ENVOIE, message.getHeureEnvoie());
        json.put(MESSAGE_CONTENU, message.getContenu());
        json.put(MESSAGE_TICKET_ID, message.getTicketID());

        return json;
    }

    public Long getID() {
        return mID;
    }

    public Long getUtilisateurINE() {
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

    @Override
    public int compareTo(@NotNull Message message) {
        return this.getHeureEnvoie().compareTo(message.getHeureEnvoie());
    }
}
