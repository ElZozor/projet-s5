--drop table APPARTENIR;
--drop table RECU;
--drop table VU;
--drop table MESSAGE;
--drop table TICKET;
--drop table UTILISATEUR;
--drop table GROUPE;



create TABLE GROUPE (

    id_groupe   INT                     NOT NULL AUTO_INCREMENT,
    label_grp   VARCHAR(50)             NOT NULL UNIQUE,

    PRIMARY KEY(id_groupe),

    CONSTRAINT CK_id_groupe CHECK       (id_groupe <> ''),
    CONSTRAINT CK_label_grp CHECK       (label_grp <> '')

);


create TABLE UTILISATEUR (

    id_util         INT                 NOT NULL AUTO_INCREMENT,
    mot_de_passe    VARCHAR(50)         NOT NULL,
    nom             VARCHAR(50)         NOT NULL,
    prenom          VARCHAR(50)         NOT NULL,
    ine             VARCHAR(11)         NOT NULL,
    type_util       VARCHAR(50)         NOT NULL,

    PRIMARY KEY(id_util),

    CONSTRAINT CK_id_util               CHECK (id_util <> ''),
    CONSTRAINT CK_mot_de_passe          CHECK (mot_de_passe <> ''),
    CONSTRAINT CK_nom                   CHECK (nom <> ''),
    CONSTRAINT CK_prenom                CHECK (prenom <> ''),
    CONSTRAINT CK_ine                   CHECK (ine <> ''),
    CONSTRAINT CK_type_util             CHECK (type_util in ("admin", "staff", "other")),
    CONSTRAINT UK_ine                   UNIQUE (ine)

);


create TABLE TICKET (

    id_ticket       INT                 NOT NULL AUTO_INCREMENT,
    titre           VARCHAR(50)         NOT NULL,
    id_util         INT                 NOT NULL,
    id_groupe       INT                 NOT NULL,

    PRIMARY KEY(id_ticket),
    FOREIGN KEY(id_util)                REFERENCES UTILISATEUR(id_util) ON delete CASCADE,
    FOREIGN KEY(id_groupe)              REFERENCES GROUPE(id_groupe)    ON delete CASCADE,

    CONSTRAINT CK_id_ticket             CHECK (id_ticket <> ''),
    CONSTRAINT CK_titre                 CHECK (titre <> ''),
    CONSTRAINT CK_id_util               CHECK (id_util <> ''),
    CONSTRAINT CK_id_groupe             CHECK (id_groupe <> '')

);

create TABLE MESSAGE (

    id_message      INT                 NOT NULL AUTO_INCREMENT,
    contenu         LONGTEXT            NOT NULL,
    heure_envoi     DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_ticket       INT                 NOT NULL,
    id_util         INT                 NOT NULL,

    PRIMARY KEY(id_message),
    FOREIGN KEY(id_ticket)              REFERENCES TICKET(id_ticket)    ON delete CASCADE,
    FOREIGN KEY(id_util)                REFERENCES UTILISATEUR(id_util) ON delete CASCADE,


    CONSTRAINT CK_id_message            CHECK  (id_message <> ''),
    CONSTRAINT CK_contenu               CHECK  (contenu <> ''),
    CONSTRAINT CK_heure_envoi           CHECK  (heure_envoi <> ''),
    CONSTRAINT CK_id_ticket             CHECK  (id_ticket <> ''),
    CONSTRAINT CK_id_util               CHECK  (id_util <> ''),
    CONSTRAINT CK_state                 CHECK  (state in (1, 2, 3))
);


create TABLE VU (

    id_message      INT                 NOT NULL,
    id_util         INT                 NOT NULL,

    FOREIGN KEY(id_message)             REFERENCES MESSAGE(id_message)  ON delete CASCADE,
    FOREIGN KEY(id_util)                REFERENCES UTILISATEUR(id_util) ON delete CASCADE

);

CREATE TABLE RECU (

    id_message      INT                 NOT NULL,
    id_util         INT                 NOT NULL,

    FOREIGN KEY(id_message)             REFERENCES MESSAGE(id_message)  ON delete CASCADE,
    FOREIGN KEY(id_util)                REFERENCES UTILISATEUR(id_util) ON delete CASCADE

);


create TABLE APPARTENIR (
    id_groupe       INT                 NOT NULL,
    id_util         INT                 NOT NULL,

    FOREIGN KEY(id_groupe)              REFERENCES GROUPE(id_groupe)    ON delete CASCADE,
    FOREIGN KEY(id_util)                REFERENCES UTILISATEUR(id_util) ON delete CASCADE

);