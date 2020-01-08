package backend.data;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeSet;

import static backend.database.Keys.*;

public class Utilisateur extends ProjectTable implements Comparable<Utilisateur> {

    private static HashMap<Long, Utilisateur> instances = new HashMap<>();

    private String mType;
    private String mNom;
    private String mPrenom;
    private String mINE;
    private Long mID;
    private String mPassword;
    private String[] mGroups;

    /**
     * Constructeur de l'objet Utilisateur à partir de son identifiant, nom, prenom, INE et de sa catégorie
     * 
     * @param id - indentifiant unique de l'utilisateur
     * @param nom - nom de l'utilisateur
     * @param prenom - prenom de l'utilisateur
     * @param INE - Identifiant National d'etudiant de l'utilisateur
     * @param type - catégorie de l'utilisateur, etudiant, enseignant, personnel ...
    **/
    public Utilisateur(long id, String nom, String prenom, String INE, String type) {
        mID = id;
        mNom = nom;
        mPrenom = prenom;
        mINE = INE;
        mType = type;
    }
    
    /**
     * Constructeur de l'objet Utilisateur à partir d'un ensemble d'information extraits de la base de données 
     * 
     * @param set - ensemble des informations de l'utilisateur obtenus via la base de données
     * @throws SQLException peut etre renvoyé si une entrée n'est pas présente dans l'ensemble
    **/
    public Utilisateur(ResultSet set) throws SQLException {
        mID = set.getLong(1);
        mNom = set.getString(3);
        mPrenom = set.getString(4);
        mINE = set.getString(5);
        mType = set.getString(6);
    }
    
    /**
     * Constructeur de l'objet Utilisateur à partir d'un objet au format JSON
     * 
     * @param object - informations sur l'utilisateur sous forme d'objet json
    **/
    public Utilisateur(JSONObject object) {
        mID = object.getLong(UTILISATEUR_ID);
        mINE = object.getString(UTILISATEUR_INE);
        mNom = object.getString(UTILISATEUR_NOM);
        mPrenom = object.getString(UTILISATEUR_PRENOM);
        mType = object.getString(UTILISATEUR_TYPE);
        mPassword = object.optString(UTILISATEUR_MDP);

        JSONArray array = object.optJSONArray("groups");
        if (array != null) {
            mGroups = new String[array.length()];
            for (int i = 0; i < array.length(); ++i) {
                mGroups[i] = array.getString(i);
            }
        }
    }
    
    /**
     * Accesseur sur un utilisateur de l'ensemble des utilisateurs (instances)
     *
     * @param id - id de l'utilisateur à recupérer dans instances
     * @return un Utilisateur dont l'id correspond à celui cherché
    **/
    public static Utilisateur getInstance(long id) {
        return instances.get(id);
    }
    
    /**
     * Methode ajoutant une instance d'utilisateur à l'ensemble des untilisateurs (instances)
     *
     * @param id - identifiant unique de l'utilisateur à ajouter
     * @param nom - nom de l'utilisateur à ajouter
     * @param prenom - prenom de l'utilisateur à ajouter
     * @param INE - Identifiant National d'etudiant de l'utilisateur à ajouter
     * @param type - catégorie de l'utilisateur à ajouter
     * @return true si l'ajout s'est bien fait. false sinon 
    **/
    public static Boolean addInstance(long id, String nom, String prenom, String INE, String type) {
        return instances.put(id, new Utilisateur(id, nom, prenom, INE, type)) != null;
    }
    
    /**
     * Methode ajoutant une instance d'utilisateur à l'ensemble des utilisateurs (instances)
     *
     * @param entryAsUtilisateur - objet Utilisateur à ajouter
     * @return si l'ajout s'est bien effectué true. false sinon
    **/
    public static boolean addInstance(Utilisateur entryAsUtilisateur) {
        return instances.put(entryAsUtilisateur.getID(), entryAsUtilisateur) != null;
    }
    
    /**
     * Methode effaçant une instance d'utilisateur de l'ensemble des utilisateurs (instances)
     *
     * @param id - clé de l'instance à supprimer et identifiant de l'utilisateur à supprimer
    **/
    public static void removeInstance(Long id) {
        instances.remove(id);
    }
    
    /**
     * methode remplaçant une instance d'utilisateur par une autre plus actuelle ou mise à jour
     *
     * @param entryAsUtilisateur - objet Utilisateur mis à jour et à placer à la place de l'ancienne instance
    **/ 
    public static void updateInstance(Utilisateur entryAsUtilisateur) {
        instances.replace(entryAsUtilisateur.getID(), entryAsUtilisateur);
    }

    /**
     * Mutateur sur l'ensemble des utilisateurs (instances)
     *
     * @param users - un ensemble trié d'utilisateurs
     **/
    public static void setInstances(TreeSet<Utilisateur> users) {
        instances = new HashMap<>();
        for (Utilisateur user : users) {
            instances.put(user.getID(), user);
        }
    }

    /**
     * Accesseur sur l'ensemble des utilisateurs
     *
     * @return - Toutes les instances d'utilisateur sous forme de collection
     */
    public static Collection<Utilisateur> getAllInstances() {
        return instances.values();
    }

    /**
     * Accesseur sur le nom d'un utilisateur
     *
     * @return le nom de l'utilisateur
     **/
    public String getNom() {
        return mNom;
    }
    
    /**
     * Mutateur sur le nom d'un utilisateur
     *
     *@param nom - nom qui va remplacer l'ancien nom
    **/
    public void setNom(final String nom) {
        mNom = nom;
    }
    
    /**
     * Accesseur sur le prénom de l'utilisateur
     *
     * @return prenom de l'utilisateur
    **/
    public String getPrenom() {
        return mPrenom;
    }

    /**
     * Mutateur sur le prénom d'un utilisateur
     *
     * @param prenom remplaçant l'ancien
    **/
    public void setPrenom(final String prenom) {
        mPrenom = prenom;
    }
    
    /**
     * Accesseur sur l'INE d'un utilisateur
     * 
     * @return INE de l'utilisateur
    **/
    public String getINE() {
        return mINE;
    }
    
    /**
     * Mutateur sur l'INE d'un utilisateur
     *
     * @param INE - nouveau INE de l'utilisateur
    **/
    public void setINE(final String INE) {
        mINE = INE;
    }
    
    /**
     * Accesseur sur la catégorie d'un utilisateur
     *
     * @return - catégorie de l'utilisateur
    **/
    public String getType() {
        return mType;
    }
    
    /**
     * Mutateur sur la catégorie de l'utilisateur
     *
     * @param type - nouvelle catégorie de l'utilisateur
    **/
    public void setType(final String type) {
        mType = type;
    }
    
    /**
     * Accesseur sur l'idnetifiant de l'utilisateur
     *
     * @return l'indentifiant unique de l'utilisateur
    **/
    public Long getID() {
        return mID;
    }
    
    /**
     * Accesseur sur le mot de passe d'un utilisateur
     *
     * @return le mot de passe de l'utilisateur
    **/
    public String getPassword() {
        return mPassword;
    }
    
    /**
     * Mutateur sur le mot de passe d'un utilisateur
     *
     * @param nouveau mot de passe de l'utilisateur
    **/
    public void setPassword(String password) {
        mPassword = password;
    }
    
    /**
     * Accesseur sur les groupes d'un utilisateur
     *
     * @return liste de groupes auxquels appartient l'utilisateur
    **/
    public String[] getGroups() {
        return mGroups;
    }
    
    /**
     * Mutateur sur les groupes de l'utilisateur
     *
     * @param  nouvelle liste de groupes auxquel appartient l'utilisateur
    **/
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
        System.out.println("PASSWORD: " + getPassword());
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

    @Override
    public int compareTo(@NotNull Utilisateur utilisateur) {
        return getID().compareTo(utilisateur.getID());
    }
}
