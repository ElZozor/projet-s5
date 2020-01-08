Le projet utilise une base de données mysql.
Il faut donc avoir un serveur mysql installé 
sur la machine exécutant le programme du serveur
ainsi que le driver jdbc correspondant.

La base de données "projets5" doit être créée.
Normalement, le programme initialise les tables de lui-même au premier lancement.
En cas d'erreur vérifier que le driver jdbc soit bien installé de même que mysql.

Si le problème persiste, dans l'arborescence du programme se trouve un dossier "res".
Dans ce même dossier, vous trouverez un fichier nommé "database.sql" qui est le fichier
utilisé pour la création des tables.
Il suffit ensuite de se connecter à un compte mysql, de sélectionner la base de donnée "projets5"
puis de coller les commandes présentent dans ce fichier.

Le seul utilisateur existant au premier démarrage est "admin" avec comme mot de passe "admin".
Pour en créer de nouveau, il faut lancer l'UI serveur, se connecter puis éditer les tables comme souhaitées.
Pour cela le serveur doit tourner en fond.

En effet, le serveur et l'UI serveur sont deux programmes disjoints, 
ils communiquent via des sockets et il est nécessaire d'être connecté 
à un compte administrateur pour pouvoir utiliser l'interface serveur.

D'autres comptes administrateurs peuvent être créés avec comme type d'utilisateur "admin" ou "staff".
Nous considérons ici que les membres du personnel peuvent éditer les entrées à leur guise.
