package backend.data;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeSet;

import static backend.database.Keys.GROUPE_ID;
import static backend.database.Keys.GROUPE_LABEL;

public class Groupe extends ProjectTable implements Comparable<Groupe> {

    private Long mID;
    private String mLabel;

    private TreeSet<Ticket> mTickets = new TreeSet<>();

    /**
     * Constructeur de l'objet Groupe à partir d'un identifiant et d'un nom de groupe
     *
     * @param id - identifiant unique du groupe 
     * @param label - nom du groupe
     **/
    public Groupe(final Long id, final String label) {
        mID = id;
        mLabel = label;
    }
    
    /**
     * Constructeur de l'objet Groupe à partir d'un identifiant, d'un nom de groupe et d'un ensemble trié de tickets
     *
     * @param id - identifiant unique du groupe 
     * @param label - nom du groupe
     * @param tickets - ensemble de tickets liés à ce groupe
    **/
    public Groupe(final Long id, final String label, final TreeSet<Ticket> tickets) {
        mID = id;
        mLabel = label;
        mTickets = tickets;
    }
    
    /**
     * Constructeur de l'objet Groupe à partir des informations extraites de la base de données
     *
     * @param set - ensemble d'informations recupérées dans la base de donnée
     * @throws SQLException - peut être renvoyée si une entrée n'est pas présente dans le ResultSet
    **/
    public Groupe(ResultSet set) throws SQLException {
        mID = set.getLong(1);
        mLabel = set.getString(2);
    }
    
    /**
     * Constructeur de l'objet Groupe à partir d'un message au format JSON 
     *
     * @param jsonObject - objet json contenant toutes les onformations d'un groupe
    **/
    public Groupe(JSONObject jsonObject) {
        mID = jsonObject.getLong(GROUPE_ID);
        mLabel = jsonObject.getString(GROUPE_LABEL);

        JSONArray array = jsonObject.getJSONArray("tickets");
        for (int i = 0; i < array.length(); ++i) {
            JSONObject o = array.getJSONObject(i);

            mTickets.add(new Ticket(o));
        }
    }
    
    /**
     * Constructeur de l'objet groupe à partir de son nom
     *
     *  @param label - nom du groupe a creer
    **/
    public Groupe(String label) {
        super();

        setLabel(label);
    }
    
    /**
     * Accesseur sur l'identifiant du groupe
     *
     * @return identifiant du groupe
    **/
    public Long getID() {
        return mID;
    }
    
    /**
     * Mutateur sur l'identifiant du groupe
     *
     * @param id - nouvel identifiant
    **/
    public void setID(Long id) {
        mID = id;
    }
    
    /**
     * Accesseur sur le nom du groupe
     *
     * @return nom du groupe
    **/
    public String getLabel() {
        return mLabel;
    }
    
    /**
     * Mutateur sur le nom du groupe
     *
     * @param label - nouveau nom du groupe
    **/
    public void setLabel(String label) {
        mLabel = label;
    }
    /**
     * Methode ajoutant un ticket au groupe
     * @param ticket - Ticket à ajouter à l'nesemble de tickets du groupe
    **/ 
    public void addTicket(Ticket ticket) {
        mTickets.add(ticket);
    }

    /**
     * Accesseur sur l'ensemble de tickets du groupe
     *
     * @return l'ensemble de tickets du groupe
    **/ 
    public TreeSet<Ticket> getTickets() {
        return mTickets;
    }
    
    /**
     * Methode qui encode un groupe en un objet json 
     *
     * @return un objet au format JSON contenant toutes les informations du groupe
    **/
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put(GROUPE_ID, mID);
        jsonObject.put(GROUPE_LABEL, mLabel);

        JSONArray array = new JSONArray();
        for (Ticket ticket : mTickets) {
            array.put(ticket.toJSON());
        }

        jsonObject.put("tickets", array);

        return jsonObject;
    }

    @Override
    public int compareTo(@NotNull Groupe groupe) {
        int idComparison = getID().compareTo(groupe.getID());
        if (idComparison == 0) {
            return 0;
        }

        int labelComparison = this.getLabel().compareTo(groupe.getLabel());
        return (labelComparison == 0 ? idComparison : labelComparison);
    }

    @Override
    public String toString() {
        return getLabel();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Groupe) {
            return getID().equals(((Groupe) obj).getID());
        }

        return false;
    }

    public void updateTickets() {
        mTickets = new TreeSet<>(mTickets);
    }
}
