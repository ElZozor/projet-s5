package backend.database;

public class Keys {

    public static final String TABLE_NAME_UTILISATEUR = "UTILISATEUR";
    public static final String UTILISATEUR_ID = "id_util";
    public static final String UTILISATEUR_MDP = "mot_de_passe";
    public static final String UTILISATEUR_NOM = "nom";
    public static final String UTILISATEUR_PRENOM = "prenom";
    public static final String UTILISATEUR_INE = "ine";
    public static final String UTILISATEUR_TYPE = "type_util";

    public static final String TABLE_NAME_TICKET = "TICKET";
    public static final String TICKET_ID = "id_ticket";
    public static final String TICKET_UTILISATEUR_ID = "id_util";
    public static final String TICKET_GROUP_ID = "id_groupe";
    public static final String TICKET_TITRE = "titre";

    public static final String TABLE_NAME_MESSAGE = "MESSAGE";
    public static final String MESSAGE_ID = "id_message";
    public static final String MESSAGE_CONTENU = "contenu";
    public static final String MESSAGE_HEURE_ENVOIE = "heure_envoi";
    public static final String MESSAGE_UTILISATEUR_ID = "id_util";
    public static final String MESSAGE_TICKET_ID = "id_ticket";


    public static final String TABLE_NAME_GROUPE = "GROUPE";
    public static final String GROUPE_ID = "id_groupe";
    public static final String GROUPE_LABEL = "label_grp";

    public static final String TABLE_NAME_VU = "VU";
    public static final String VU_UTILISATEUR_ID = "id_util";
    public static final String VU_MESSAGE_ID = "id_message";


    public static final String TABLE_NAME_APPARTENIR = "APPARTENIR";
    public static final String APPARTENIR_GROUPE_ID = "id_groupe";
    public static final String APPARTENIR_UTILISATEUR_ID = "id_util";

    public static final String TABLE_NAME_RECU = "RECU";
    public static final String RECU_MESSAGE_ID = "id_message";
    public static final String RECU_UTILISATEUR_ID = "id_util";
}
