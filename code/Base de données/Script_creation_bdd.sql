DROP TABLE APPARTENIR;
DROP TABLE VU;
DROP TABLE MESSAGE;
DROP TABLE TICKET;
DROP TABLE UTILISATEUR; 
DROP TABLE GROUPE;



CREATE TABLE GROUPE(
    id_groupe INT NOT NULL AUTO_INCREMENT,
    label_grp VARCHAR(50) NOT NULL,

    PRIMARY KEY(id_groupe),
    
    CONSTRAINT CK_id_groupe CHECK  (id_groupe <> ''),
    CONSTRAINT CK_label_grp CHECK  (label_grp <> '')

);

CREATE TABLE UTILISATEUR(
    id_util INT NOT NULL AUTO_INCREMENT,
    mot_de_passe VARCHAR(50) NOT NULL,
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    ine INT NOT NULL,
    type_util VARCHAR(50) NOT NULL,
    
    PRIMARY KEY(id_util),
    
    CONSTRAINT CK_id_util CHECK  (id_util <> ''),
    CONSTRAINT CK_mot_de_passe CHECK  (mot_de_passe <> ''),
    CONSTRAINT CK_nom CHECK  (nom <> ''),
    CONSTRAINT CK_prenom CHECK  (prenom <> ''),
    CONSTRAINT CK_ine CHECK  (ine <> ''),
    CONSTRAINT CK_type_util CHECK  (type_util <> '')
    

);
    
CREATE TABLE TICKET(
    id_ticket INT NOT NULL AUTO_INCREMENT,
    titre VARCHAR(50) NOT NULL,
    premier_message LONGTEXT NOT NULL,
    dernier_message LONGTEXT NOT NULL,
    id_util INT NOT NULL,
    id_groupe INT NOT NULL,
    
    PRIMARY KEY(id_ticket),
    FOREIGN KEY(id_util) REFERENCES UTILISATEUR ON DELETE CASCADE, -- si l'utilisateur qui a créé le ticket est supprimé alors on supprime le ticket
    FOREIGN KEY(id_groupe)REFERENCES GROUPE ON DELETE CASCADE, -- pareil avec le groupe sur lequel se trouve le ticket
    
    CONSTRAINT CK_id_ticket CHECK  (id_ticket <> ''),
    CONSTRAINT CK_titre CHECK  (titre <> ''),
    CONSTRAINT CK_premier_message CHECK  (premier_message <> ''),
    CONSTRAINT CK_dernier_message CHECK  (dernier_message <> ''),
    CONSTRAINT CK_id_util CHECK  (id_util <> ''),
    CONSTRAINT CK_id_groupe CHECK  (id_groupe <> '')
    
);
    
    

    
CREATE TABLE MESSAGE(
    id_message INT NOT NULL AUTO_INCREMENT,
    contenu LONGTEXT NOT NULL,
    heure_envoi DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_ticket INT NOT NULL,
    id_util INT NOT NULL,
    
    PRIMARY KEY(id_message),
    FOREIGN KEY(id_ticket) REFERENCES TICKET ON DELETE CASCADE,
    FOREIGN KEY(id_util) REFERENCES UTILISATEUR ON DELETE CASCADE,
    
    
    CONSTRAINT CK_id_message CHECK  (id_message <> ''),
    CONSTRAINT CK_contenu CHECK  (contenu <> ''),
    CONSTRAINT CK_heure_envoi CHECK  (heure_envoi <> ''),
    CONSTRAINT CK_id_ticket CHECK  (id_ticket <> ''),
    CONSTRAINT CK_id_util CHECK  (id_util <> '')
);

CREATE TABLE VU(
    id_message INT NOT NULL,
    id_util INT NOT NULL,
    
    PRIMARY KEY(id_message,id_util),
    FOREIGN KEY(id_message) REFERENCES MESSAGE ON DELETE CASCADE,
    FOREIGN KEY(id_util) REFERENCES UTILISATEUR ON DELETE CASCADE

);

CREATE TABLE APPARTENIR(
    id_groupe INT NOT NULL,
    id_util INT NOT NULL, 
    
    PRIMARY KEY(id_groupe,id_util),
    FOREIGN KEY(id_groupe) REFERENCES GROUPE ON DELETE CASCADE,
    FOREIGN KEY(id_util) REFERENCES UTILISATEUR ON DELETE CASCADE
    
);
