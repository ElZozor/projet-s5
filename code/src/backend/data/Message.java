package backend.data;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.sql.Date;

public class Message implements Comparable<Message> {

    public static final String KEY_ID               = "id";
    public static final String KEY_UTILISATEUR_ID   = "utilisateurID";
    public static final String KEY_POST_DATE        = "post_date";
    public static final String KEY_CONTENU          = "contenu";

    private Long mID;
    private Long mUtilisateurID;
    private Date mPostDate;
    private String mContenu;


    public static Message fromJSON(JSONObject json) {
        Long id             = json.getLong(KEY_ID);
        Long utilisateurID  = json.getLong(KEY_UTILISATEUR_ID);
        Date date           = new Date(json.getLong(KEY_POST_DATE));
        String contenu      = json.getString(KEY_CONTENU);

        return new Message(id, utilisateurID, date, contenu);
    }

    public static JSONObject toJSON(Message message) {
        JSONObject json = new JSONObject();

        json.put(KEY_ID, message.getID());
        json.put(KEY_UTILISATEUR_ID, message.getUtilisateurID());
        json.put(KEY_POST_DATE, message.getPostDate());
        json.put(KEY_CONTENU, message.getContenu());

        return json;
    }

    public Message(Long id, Long utilisateurID, Date date, String contenu) {
        mID             = id;
        mUtilisateurID  = utilisateurID;
        mPostDate       = date;
        mContenu        = contenu;
    }

    public Long getID() {
        return mID;
    }

    public Long getUtilisateurID() {
        return mUtilisateurID;
    }

    public Date getPostDate() {
        return mPostDate;
    }

    public String getContenu() {
        return mContenu;
    }

    @Override
    public int compareTo(@NotNull Message message) {
        return this.getPostDate().compareTo(message.getPostDate());
    }
}
