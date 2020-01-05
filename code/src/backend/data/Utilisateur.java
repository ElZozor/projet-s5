package backend.data;

import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import static backend.database.Keys.*;

public class Utilisateur extends ProjectTable {

    private static HashMap<Long, Utilisateur> instances;

    private String mType;
    private String mNom;
    private String mPrenom;
    private String mINE;
    private Long mID;
    private String mPassword;
    private String[] mGroups;

    public Utilisateur(long id, String nom, String prenom, String INE, String type) {
        mID = id;
        mNom = nom;
        mPrenom = prenom;
        mINE = INE;
        mType = type;
    }

    public Utilisateur(ResultSet set) throws SQLException {
        mID = set.getLong(1);
        mNom = set.getString(3);
        mPrenom = set.getString(4);
        mINE = set.getString(5);
        mType = set.getString(6);
    }

    public Utilisateur(JSONObject object) {
        mID = object.getLong(UTILISATEUR_ID);
        mINE = object.getString(UTILISATEUR_INE);
        mNom = object.getString(UTILISATEUR_NOM);
        mPrenom = object.getString(UTILISATEUR_PRENOM);
        mType = object.getString(UTILISATEUR_TYPE);
        mPassword = object.optString(UTILISATEUR_MDP);
        mGroups = (String[]) object.opt("groups");
    }

    public static Utilisateur getInstance(long id) {
        return instances.get(id);
    }

    public static Boolean addInstance(long id, String nom, String prenom, String INE, String type) {
        if (instances.containsKey(id)) {
            return false;
        }

        instances.put(id, new Utilisateur(id, nom, prenom, INE, type));

        return true;
    }

    public String getNom() {
        return mNom;
    }

    public void setNom(final String nom) {
        mNom = nom;
    }

    public String getPrenom() {
        return mPrenom;
    }

    public void setPrenom(final String prenom) {
        mPrenom = prenom;
    }

    public String getINE() {
        return mINE;
    }

    public void setINE(final String INE) {
        mINE = INE;
    }

    public String getType() {
        return mType;
    }

    public void setType(final String type) {
        mType = type;
    }

    public Long getID() {
        return mID;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    public String[] getGroups() {
        return mGroups;
    }

    public void setGroups(String[] groups) {
        mGroups = new String[groups.length];
        int i = 0;
        for (String s : groups) {
            mGroups[i++] = s;
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject result = new JSONObject();

        result.put(UTILISATEUR_ID, getID());
        result.put(UTILISATEUR_INE, getINE());
        result.put(UTILISATEUR_NOM, getNom());
        result.put(UTILISATEUR_PRENOM, getPrenom());
        result.put(UTILISATEUR_TYPE, getType());
        result.putOpt(UTILISATEUR_MDP, getPassword());
        result.putOpt("groups", getGroups());

        return result;
    }

    public void setID(final Long ID) {
        mID = ID;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Utilisateur) {
            return getID().equals(((Utilisateur) obj).getID());
        }

        return false;
    }
}
