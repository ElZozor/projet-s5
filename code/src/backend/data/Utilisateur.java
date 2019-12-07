package backend.data;

import java.util.HashMap;

public class Utilisateur {

    private static HashMap<Long, Utilisateur> instances;

    private String mNom;
    private String mPrenom;
    private String mINE;
    private Long mID;

    public static Boolean addInstance(long id, String nom, String prenom, String INE) {
        if (instances.containsKey(id)) {
            return false;
        }

        instances.put(id, new Utilisateur(id, nom, prenom, INE));

        return true;
    }

    public static Utilisateur getInstance(long id) {
        return instances.get(id);
    }



    public Utilisateur(long id, String nom, String prenom, String INE) {
        mID     = id;
        mNom    = nom;
        mPrenom = prenom;
        mINE    = INE;
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

    public Long getID() {
        return mID;
    }
}
