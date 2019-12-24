package backend.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class Utilisateur {

    private static HashMap<Long, Utilisateur> instances;

    private String mType;
    private String mNom;
    private String mPrenom;
    private String mINE;
    private Long mID;

    public Utilisateur(long id, String nom, String prenom, String INE, String type) {
        mID = id;
        mNom = nom;
        mPrenom = prenom;
        mINE = INE;
        mType = type;
    }

    public static Utilisateur getInstance(long id) {
        return instances.get(id);
    }


    public Utilisateur(ResultSet set) throws SQLException {
        mID = set.getLong(1);
        mNom = set.getString(3);
        mPrenom = set.getString(4);
        mINE = set.getString(5);
        mType = set.getString(6);
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

    public String getPrenom() {
        return mPrenom;
    }

    public String getINE() {
        return mINE;
    }

    public String getType() {
        return mType;
    }

    public Long getID() {
        return mID;
    }

    public void setType(final String type) {
        mType = type;
    }

    public void setNom(final String nom) {
        mNom = nom;
    }

    public void setPrenom(final String prenom) {
        mPrenom = prenom;
    }

    public void setINE(final String INE) {
        mINE = INE;
    }

    public void setID(final Long ID) {
        mID = ID;
    }
}
