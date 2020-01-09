Projet S5 réalité par
LAFFITE Axel
MORETTO Enzo
FOURCROY Guillaume

-----------------------------------
            COMPILATION
-----------------------------------
Le projet est un projet maven il faut donc le compiler
en tant que tel et non pas comme un projet classique
pour générer des jar valides.
Le développement a été fait sur IntelliJ IDEA donc 
je ne sais pas si la génération d'apk fonctionne sur eclipse.
Le code peut normalement être exécuté.
La compilation sous intellij génère plusieurs fichiers dans
le sous-répertoire target.
Il faut cependant déléguer la compilation à maven.

-----------------------------------
         EDITION DU CODE
-----------------------------------
Si des modifications sont faites sur le code et qu'il est exécuté
depuis un éditeur, il est nécessaire de mettre la valeur
Debugger.isDebugging à true dans le launcher correspondant
sinon les fichiers se chargeront mal, y compris les images.
Ces launchers sont 
    - ClientLaunch
    - ServeurLaunch
    - ServerUILaunch


-----------------------------------
            EXÉCUTION
-----------------------------------
Le projet doit être exécuté dans l'arborescence suivante :
./
-> Client
-> Server
-> ServerUI
    -> res/
        -> base.png
        -> group.png
        -> refresh.png
        -> ticket.png
        -> ticket_seen.png
        -> database.sql
        -> keystore

Tout ces fichiers sont essentiels pour le bon fonctionnement du programme.


-----------------------------------
                BDD
-----------------------------------
Le projet utilise une base de données mysql.
Il faut donc avoir un serveur mysql installé 
sur la machine exécutant le programme du serveur
ainsi que le driver jdbc correspondant.

La base de données "projets5" doit être créée.
Normalement, le programme initialise les tables de 
lui-même au premier lancement.
En cas d'erreur vérifier que le driver 
jdbc soit bien installé de même que mysql.

Si le problème persiste, dans l'arborescence du programme 
se trouve un dossier "res". Dans ce même dossier, vous trouverez 
un fichier nommé "database.sql" qui est le fichier
utilisé pour la création des tables.
Il suffit ensuite de se connecter à un compte mysql, 
de sélectionner la base de donnée "projets5"
puis de coller les commandes présentent dans ce fichier.


L'utilisateur utilisé pour la connection est "root" sans mot de passe.
Si cela ne correspond pas vous pouvez le changer dans la classe
DatabaseManager : 
    - private static final String username = "root";
    - private static final String password = "";


-----------------------------------
            UTILISATION
-----------------------------------
Le seul utilisateur existant au premier démarrage est "admin" 
avec comme mot de passe "admin".
Pour en créer de nouveau, il faut lancer l'UI serveur, 
se connecter puis éditer les tables comme souhaitées.
Pour cela le serveur doit être au préalable 
lancé depuis l'exécutable "Serveur".

En effet l'interface d'édition des tables et l'interface serveur sont
deux programmes distincts.
Il est donc possible d'éditer les tables à distance en disposant de
l'exécutable ServerUI et en ayant accès à un compte administrateur.

D'autres comptes administrateurs peuvent être créés 
avec comme type d'utilisateur "admin" ou "staff".
Nous considérons ici que les membres du personnel 
peuvent éditer les entrées à leur guise.




-----------------------------------
          FONCTIONNALITÉS
-----------------------------------
Serveur UI permet de créer, supprimer et modifier les entrées de la 
base de données.
Il n'est pas possible d'ajouter ou d'éditer des tickets et des messages.
Si le compte admin est supprimé il est ajouté de nouveau au prochain
démarrage du serveur.
L'UI est cependant déconnecté du serveur.

Serveur permet de lancer le serveur et d'avoir accès aux messages de log.
Ce programme montre aussi les stats du serveur, notamment le nombre 
de clients connectés et d'admin (personne sur l'interface Serveur) 
connectés.
Ce programme ne peut avoir qu'une instance de lancée.


Client permet de poster des messages et créer des tickets.
En cas de ticket supprimé, il disparait automatiquement ou 
quand vous cliquez dessus.
En cas de groupe supprimé, il disparait automatiquement ou quand
vous essayez d'accéder à un ticket correspondant.
Si l'utilisateur est supprimé, il vous êtes déconnecté du serveur
et l'interface se ferme après avoir affiché un message d'information.



Tout ceci est censé être résistant au coupures.
Par exemple si le serveur est arrêté, à la prochaine action du client
qui requiert le réseau, le status va passer de "connecté" à "reconnection..".
Là, le client essaiera toutes les secondes de se reconnecter.
Si la reconnexion n'a pas été faite avant de fermer l'ui client,
les messages en attentent seront sauvegardés et renvoyés à la prochaine 
connexion.
Si la reconnexion est effectuée, les messages seront envoyés lors
de celle-ci.