package backend.server.client;


import backend.data.Groupe;
import backend.data.Message;
import backend.data.Ticket;
import backend.data.Utilisateur;
import backend.modele.UserModel;
import backend.server.Server;
import backend.server.communication.CommunicationMessage;
import backend.server.communication.classic.ClassicMessage;
import debug.Debugger;
import org.json.JSONArray;
import org.json.JSONException;
import ui.InteractiveUI;
import ui.Server.ServerUI;
import utils.Utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.swing.*;
import java.io.*;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static backend.database.Keys.*;
import static utils.Utils.HOST;
import static utils.Utils.PORT;

public class Client extends Thread implements Server {

    private final static String GROUPS_FILE = "groups";
    private final static String RELATED_GROUPS_FILE = "related_groups";
    private final static String USERS_FILE = "users";
    private final static String PENDING_MESSAGES_FILE = "pending_messages";

    private final static String DBG_COLOR = Debugger.YELLOW;
    private final static int SOCKET_TIMEOUT = 5000;

    private SSLSocket mSocket;

    private BufferedWriter mWriteStream;
    private BufferedReader mReadStream;

    private InteractiveUI ui;
    private Boolean running = false;
    private Boolean connected = true;
    private Boolean requestEverything = false;

    private Utilisateur myUser;

    private Stack<CommunicationMessage> pendingMessages = new Stack<>();

    /**
     * This class is used on the client side.
     * It's used to communicate with the host.
     *
     * @param socket The connexion socket
     * @throws ServerInitializationFailedException When the server can't be init
     */
    public Client(SSLSocket socket) throws ServerInitializationFailedException {
        try {
            mSocket = socket;

            mSocket.setSoTimeout(SOCKET_TIMEOUT);

            mWriteStream = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
            mReadStream = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new ServerInitializationFailedException("Something went wrong while initializing connexion");
        }
    }

    /**
     * Charge les contenus sauvegardés sur le disque
     */
    public void loadContents() {
        loadLocalData();
        loadPendingMessages();
    }


    /**
     * Charge les groupes, utilisateurs, et groupes affiliés
     * sauvegardés sur le disque
     */
    private void loadLocalData() {
        TreeSet<String> groups = loadGroups();
        TreeSet<Utilisateur> users = loadUsers();
        TreeSet<Groupe> relatedGroups = loadRelatedGroups();

        Utilisateur.setInstances(users);

        ui.updateGroupsList(groups);
        ui.updateRelatedGroups(relatedGroups);
    }

    /**
     * Charge les groupes sauvegardés sur le disque
     *
     * @return - Les groupes sauvegardés
     */
    private TreeSet<String> loadGroups() {
        TreeSet<String> groups = new TreeSet<>();

        try {
            JSONArray localData = new JSONArray(Utils.loadFromFile(GROUPS_FILE));

            for (int i = 0; i < localData.length(); ++i) {
                try {
                    groups.add(localData.getString(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            Debugger.logColorMessage(Debugger.RED, "Client", "Unable to load groups");
        }

        return groups;
    }

    /**
     * Charge les utilisateurs sauvegardés sur le disque
     *
     * @return - Les utilisateurs sauvegardés
     */
    private TreeSet<Utilisateur> loadUsers() {
        TreeSet<Utilisateur> users = new TreeSet<>();

        try {
            JSONArray localData = new JSONArray(Utils.loadFromFile(USERS_FILE));

            for (int i = 0; i < localData.length(); ++i) {
                try {
                    users.add(new Utilisateur(localData.getJSONObject(i)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            Debugger.logColorMessage(Debugger.RED, "Client", "Unable to load users");
        }

        return users;
    }

    /**
     * Charge les groupes affiliés sur le disque
     *
     * @return - Les groupes affiliés
     */
    private TreeSet<Groupe> loadRelatedGroups() {
        TreeSet<Groupe> groupes = new TreeSet<>();

        try {
            JSONArray localData = new JSONArray(Utils.loadFromFile(RELATED_GROUPS_FILE));

            for (int i = 0; i < localData.length(); ++i) {
                try {
                    groupes.add(new Groupe(localData.getJSONObject(i)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            Debugger.logColorMessage(Debugger.RED, "Client", "Unable to load related groups");
        }

        return groupes;
    }

    /**
     * Charge les message en attente sauvegardés sur le disque
     *
     * @return - Les messages en attente
     */
    private void loadPendingMessages() {
        try {
            JSONArray savedPendingMessages = new JSONArray(Utils.loadFromFile(PENDING_MESSAGES_FILE));

            for (int i = 0; i < savedPendingMessages.length(); ++i) {
                try {
                    pendingMessages.push(new ClassicMessage(savedPendingMessages.getString(i)));
                } catch (CommunicationMessage.InvalidMessageException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            Debugger.logColorMessage(Debugger.RED, "Client", "Unable to load pending messages");
        }


        sendPendingMessages();
    }

    /**
     * Sauvegarde les données locales sur le disque
     *
     * @param groups        - Les groupes
     * @param relatedGroups - Les groupes affiliés
     */
    public void saveContents(TreeSet<String> groups, TreeSet<Groupe> relatedGroups) {
        saveLocalData(groups, relatedGroups);

        savePendingMessages();
    }

    /**
     * Sauvegarde les groupes et groupes affiliés sur le disque
     *
     * @param groups        - Les groupes
     * @param relatedGroups - Les groupes affiliés
     */
    private void saveLocalData(TreeSet<String> groups, TreeSet<Groupe> relatedGroups) {
        if (groups != null || relatedGroups != null) {
            if (groups != null) {
                saveGroups(groups);
            }

            if (relatedGroups != null) {
                saveRelatedGroups(relatedGroups);
            }

            saveUsers(Utilisateur.getAllInstances());
        }
    }

    /**
     * Sauvegarde les groupes sur le disque
     *
     * @param groups - Les groupes
     */
    private void saveGroups(TreeSet<String> groups) {
        JSONArray array = new JSONArray();
        for (String groupe : groups) {
            array.put(groupe);
        }

        Utils.saveToFile(GROUPS_FILE, array.toString());
    }

    /**
     * Sauvegarde les groupes affiliés sur le disque
     *
     * @param relatedGroups - Les groupes affiliés
     */
    private void saveRelatedGroups(TreeSet<Groupe> relatedGroups) {
        JSONArray array = new JSONArray();
        for (Groupe groupe : relatedGroups) {
            array.put(groupe.toJSON());
        }

        Utils.saveToFile(RELATED_GROUPS_FILE, array.toString());
    }

    /**
     * Sauvegarde les utilisateurs sur le disque
     *
     * @param users - Les utilisateurs
     */
    private void saveUsers(Collection<Utilisateur> users) {
        JSONArray array = new JSONArray();
        for (Utilisateur user : users) {
            array.put(user.toJSON());
        }

        Utils.saveToFile(USERS_FILE, array.toString());
    }

    /**
     * Sauvegarde les messages en attente sur le disque
     */
    private void savePendingMessages() {
        JSONArray array = new JSONArray();
        pendingMessages.toArray();

        CommunicationMessage[] pending = new CommunicationMessage[pendingMessages.size()];
        for (int i = 0; i < pending.length; ++i) {
            String message = pendingMessages.elementAt(i).toString();
            array.put(message.substring(0, message.lastIndexOf("\n")));
        }

        Utils.saveToFile(PENDING_MESSAGES_FILE, array.toString());
    }

    /**
     * Permet d'attribuer une interface graphique au client
     *
     * @param ui
     */
    public void setUI(InteractiveUI ui) {
        this.ui = ui;
        ui.setConnectionStatus(connected);
    }


    /**
     * Envoie des données et attend le retour de l'hôte.
     * S'arrête en cas de timeout.
     *
     * @param classicMessage Le message à envoyer
     * @return Le message retourné par l'hôte
     * @throws IOException -
     */
    public ClassicMessage sendAndWaitForReturn(ClassicMessage classicMessage) throws IOException {
        sendData(classicMessage);

        try {
            return readData();
        } catch (IOException | ClassicMessage.InvalidMessageException | SocketDisconnectedException e) {
            return null;
        }
    }


    /**
     * Fonction utilisée pour se conncter à l'hôte.
     * Bloquante.
     *
     * @param INE - L'ine
     * @param password - Le mot de passe
     * @return Si la connection est un succès ou non
     */
    public ClassicMessage sendConnectionMessage(String INE, String password) {

        ClassicMessage returnedData = null;

        try {
            returnedData = sendAndWaitForReturn(
                    ClassicMessage.createConnection(INE, password)
            );

            if (returnedData != null && returnedData.isAck()) {
                myUser = new Utilisateur(0L, "", "", INE, "");
                myUser.setPassword(password);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return returnedData;

    }


    /**
     * Cette fonction est utilsée pour se déconnecter de l'hôte.
     * @throws IOException -
     */
    public void disconnect(TreeSet<String> groups, TreeSet<Groupe> relatedGroups) throws IOException {

        mSocket.close();
        running = false;

        saveContents(groups, relatedGroups);

    }


    /**
     * methode bouclant à l'infini tant qu'il n'y a pas de fermeture de l'application,
     * cette methode attend la reception d'un message et le transmet à handleMessage pour le traiter.
     * En cas de perte de connexion il y'a tentative de reconnexion jusqu'a reussite ou fermeture de l'application
     */
    @Override
    public void run() {

        try {
            mSocket.setSoTimeout(0);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        running = true;

        while (running) {

            try {
                ClassicMessage message = readData();
                if (message == null || ui == null) {
                    continue;
                }

                handleMessage(message);

            } catch (SocketDisconnectedException e) {
                reconnect();
            } catch (IOException | ClassicMessage.InvalidMessageException e) {
                e.printStackTrace();
            }

        }

    }


    /**
     * Reconnecte l'utilisateur au serveur
     */
    private void reconnect() {
        connected = false;
        if (ui != null) {
            ui.setConnectionStatus(false);
        }

        while (running && !connected) {
            try {
                mSocket = (SSLSocket) SSLContext.getDefault().getSocketFactory().createSocket(HOST, PORT);
                mSocket.setSoTimeout(SOCKET_TIMEOUT);

                mWriteStream = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
                mReadStream = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

                ClassicMessage message = sendConnectionMessage(myUser.getINE(), myUser.getPassword());
                connected = (message != null && message.isAck());

                mSocket.setSoTimeout(0);
            } catch (IOException | NoSuchAlgorithmException ex) {
                try {
                    sleep(1000);
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }
        }

        if (running && connected) {
            setRequestEverything(requestEverything);
            sendPendingMessages();

            if (ui != null) {
                ui.setConnectionStatus(true);
            }
        }
    }

    /**
     * Renvoie tous les messages en attente stockés en mémoire
     */
    private void sendPendingMessages() {
        CommunicationMessage[] pending = new CommunicationMessage[pendingMessages.size()];
        for (int i = 0; i < pending.length; ++i) {
            pending[i] = pendingMessages.pop();
        }

        for (CommunicationMessage message : pending) {
            sendData(message);
        }
    }


    /**
     * Methode recevant un message et le redirigeant vers les fonction de traitement adaptées au type du message
     * @param message - objet ClassiqueMessage étant le message reçu
    **/
    private void handleMessage(ClassicMessage message) {

        switch (message.getType()) {
            case LOCAL_UPDATE_RESPONSE:
                handleLocalUpdate(message);
                break;

            case ENTRY_ADDED:
                handleEntryAdded(message);
                break;

            case ENTRY_DELETED:
                handleEntryDeleted(message);
                break;

            case ENTRY_UPDATED:
                handleEntryUpdated(message);
                break;

            case TABLE_MODEL:
                handleTableModelMessage(message);
                break;
        }

    }

    /**
     * Traite un message de type "tablemodel", utilisée par l'UI serveur.
     *
     * @param message - Le message envoyé par l'hôte
     */
    private void handleTableModelMessage(ClassicMessage message) {

        if (ui instanceof ServerUI) {
            Debugger.logMessage("Client", "Table model received, sending to the ui");
            ServerUI serverUI = (ServerUI) ui;

            final UserModel userModel = message.getTableModelUserModel();
            final String password = myUser.getPassword();
            myUser = userModel.getReferenceTo(myUser.getINE());
            myUser.setPassword(password);

            ui.setTitle("Administration | Connecté en tant que : " + myUser.getNom() + " " + myUser.getPrenom());

            serverUI.setAllModels(
                    message.getTableModelUserModel(),
                    message.getTableModelGroupModel(),
                    message.getTableModelTicketModel(),
                    message.getTableModelMessageModel()
            );
        }

    }


    /**
     * Traite un message de type local update.
     *
     * @param message - Le message envoyé par l'hôte.
     */
    private void handleLocalUpdate(ClassicMessage message) {
        TreeSet<Groupe> relatedGroups = message.getLocalUpdateResponseRelatedGroups();
        TreeSet<String> allGroups = message.getLocalUpdateResponseAllGroups();
        TreeSet<Utilisateur> users = message.getLocalUpdateResponseUsers();

        for (Utilisateur user : users) {
            if (user.getINE().equals(myUser.getINE())) {
                final String password = myUser.getPassword();
                myUser = user;
                myUser.setPassword(password);

                ui.setTitle("Connecté en tant que : " + myUser.getNom() + " " + myUser.getPrenom());
                break;
            }
        }

        Utilisateur.setInstances(users);

        ui.updateRelatedGroups(relatedGroups);
        ui.updateGroupsList(allGroups);

        ArrayList<Message> received = new ArrayList<>();
        for (Groupe groupe : relatedGroups) {
            for (Ticket ticket : groupe.getTickets()) {
                for (Message msg : ticket.getMessages()) {
                    if (msg.state() < 3) {
                        received.add(msg);
                    }
                }
            }
        }

        sendData(ClassicMessage.createMessageReceived(received));
    }


    /**
     * Traite un message quand un entrée est ajoutée.
     *
     * @param message - Le message envoyé par l'hôte.
     */
    private void handleEntryAdded(ClassicMessage message) {
        ArrayList<Message> received = new ArrayList<>();

        switch (message.getTable()) {
            case TABLE_NAME_UTILISATEUR:
                Utilisateur user = message.getEntryAsUtilisateur();
                Utilisateur.addInstance(user);

                if (ui instanceof ServerUI) {
                    ((ServerUI) ui).addUser(user);
                }

                break;

            case TABLE_NAME_GROUPE:
                ui.addGroupe(message.getEntryAsGroupe());
                break;

            case TABLE_NAME_TICKET:
                ui.addTicket(message.getEntryRelatedGroup(), message.getEntryAsTicket());
                TreeSet<Message> messages = message.getEntryAsTicket().getMessages();
                if (messages != null) {
                    received.addAll(messages);
                }

                break;

            case TABLE_NAME_MESSAGE:
                ui.addMessage(message.getEntryRelatedGroup(), message.getEntryRelatedTicket(), message.getEntryAsMessage());
                received.add(message.getEntryAsMessage());
                break;
        }


        if (!received.isEmpty()) {
            sendData(ClassicMessage.createMessageReceived(received));
        }
    }

    /**
     * Traite une entrée supprimée.
     *
     * @param message - Le message envoyé par l'hôte.
     */
    private void handleEntryDeleted(ClassicMessage message) {
        switch (message.getTable()) {
            case TABLE_NAME_UTILISATEUR:
                Utilisateur user = message.getEntryAsUtilisateur();
                Utilisateur.removeInstance(user.getID());

                if (user.equals(myUser)) {
                    JOptionPane.showMessageDialog(ui,
                            "Votre utilisateur a été supprimé !",
                            "Déconnexion",
                            JOptionPane.ERROR_MESSAGE
                    );
                    ui.dispose();

                } else {
                    ui.deleteUser(user);
                }
                break;

            case TABLE_NAME_GROUPE:
                ui.deleteGroupe(message.getEntryAsGroupe());
                break;

            case TABLE_NAME_TICKET:
                ui.deleteTicket(message.getEntryRelatedGroup(), message.getEntryAsTicket());
                break;

            case TABLE_NAME_MESSAGE:
                ui.deleteMessage(message.getEntryRelatedGroup(), message.getEntryRelatedTicket(), message.getEntryAsMessage());
                break;
        }
    }


    /**
     * Traite une entrée maj.
     *
     * @param message - Le message envoyé par l'hôte.
     */
    private void handleEntryUpdated(ClassicMessage message) {
        ArrayList<Message> received = new ArrayList<>();

        switch (message.getTable()) {
            case TABLE_NAME_UTILISATEUR:
                Utilisateur user = message.getEntryAsUtilisateur();
                Utilisateur.updateInstance(user);
                if (user.getID().equals(myUser.getID())) {
                    myUser = user;
                }

                if (ui instanceof ServerUI) {
                    ((ServerUI) ui).updateUser(user);
                }

                break;

            case TABLE_NAME_GROUPE:
                ui.updateGroupe(message.getEntryAsGroupe());
                break;

            case TABLE_NAME_TICKET:
                Ticket ticket = message.getEntryAsTicket();
                ui.updateTicket(message.getEntryRelatedGroup(), ticket);

                if (ticket.containsUnreceivedMessages()) {
                    TreeSet<Message> messages = ticket.getMessages();
                    if (messages != null) {
                        received.addAll(messages);
                    }
                }
                break;

            case TABLE_NAME_MESSAGE:
                System.out.println("blblbl");
                ui.updateMessage(message.getEntryRelatedGroup(), message.getEntryRelatedTicket(), message.getEntryAsMessage());
                if (message.getEntryAsMessage().state() < 3) {
                    received.add(message.getEntryAsMessage());
                }

                break;
        }

        if (!received.isEmpty()) {
            sendData(ClassicMessage.createMessageReceived(received));
        }
    }


    /**
     * Utilisée pour poster un nouveau message.
     * Non bloquante.
     *
     * @param ticketid - Le ticket en question
     * @param contents - Le contenu du message.
     */
    public void postAMessage(Long ticketid, String contents) {
        sendData(ClassicMessage.createMessage(ticketid, contents));
    }


    /**
     * Envoie un message pour demander la maj des données
     * locale au serveur.
     * Non bloquante.
     */
    public void updateLocalDatabase() {
        sendData(ClassicMessage.createLocalUpdate(new Date(0)));
    }

    /**
     * Envoie une nofitication pour dire que l'on a
     * cliqué sur un ticket.
     *
     * @param ticket - Le ticket en question
     */
    public void sendNotificationTicketClicked(Ticket ticket) {
        sendData(ClassicMessage.createTicketClicked(ticket));
    }


    /**
     * Utilisée par l'ui serveur pour demander
     * toutes les tables au serveur.
     */
    public void retrieveAllModels() {
        sendData(ClassicMessage.createTableModelRequest());
    }

    /**
     * Utilisée par l'UI serveur pour demander au
     * serveur de nous envoyer toutes les mises à jour
     * disponibles.
     *
     * @param b - Si on veut tout récuprérer
     */
    public void setRequestEverything(boolean b) {
        requestEverything = b;

        if (requestEverything) {
            sendData(ClassicMessage.createRequestEverything());
        }
    }

    @Override
    public BufferedWriter getSocketWriter() {
        return mWriteStream;
    }

    @Override
    public BufferedReader getSocketReader() {
        return mReadStream;
    }

    @Override
    public void addPendingMessage(CommunicationMessage message) {
        pendingMessages.push(message);
    }

    public Utilisateur getMyUser() {
        return myUser;
    }
}