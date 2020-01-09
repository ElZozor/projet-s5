package backend.server.host;

import backend.data.Utilisateur;
import backend.database.DatabaseManager;
import backend.server.Server;
import backend.server.communication.CommunicationMessage;
import debug.Debugger;
import ui.Server.ServerStopUI;
import utils.Utils;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.swing.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class Host extends Thread {

    public static final String DBG_COLOR = Debugger.RED;

    private static HashMap<String, HashSet<Server>> clientsByGroups = new HashMap<>();
    private static HashMap<Long, HashSet<Server>> clientsByID = new HashMap<>();
    private static ArrayList<Server> admins = new ArrayList<>();
    private SSLServerSocket mServerSocket;
    public static Boolean isRunning = false;

    private static ServerStopUI ui;

    private static int nbConnectes = 0;
    private static int nbAdmins = 0;

    public Host() throws IOException {
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        mServerSocket = (SSLServerSocket) factory.createServerSocket(Utils.PORT);
    }

    public synchronized static void addClient(Collection<String> groups, Utilisateur user, ClientManager client) {
        for (String group : groups) {
            HashSet<Server> clientSet = clientsByGroups.computeIfAbsent(group, k -> new HashSet<>());
            clientSet.add(client);
        }

        if (!clientsByID.containsKey(user.getID())) {
            clientsByID.put(user.getID(), new HashSet<>());
        }

        clientsByID.get(user.getID()).add(client);

        ui.setConnectionNumber(++nbConnectes);
        postLogMessage(user.getINE() + " s'est connecté !");
    }


    public synchronized static void removeClient(Collection<String> groups, Utilisateur user, Server client) {
        for (String group : groups) {
            HashSet<Server> set = clientsByGroups.get(group);
            if (set != null) {
                set.remove(client);
            }
        }

        HashSet<Server> set = clientsByID.get(user.getID());
        set.remove(client);
        if (admins.remove(client)) {
            ui.setAdminNumber(--nbAdmins);
        }

        ui.setConnectionNumber(--nbConnectes);
        postLogMessage(user.getINE() + " s'est déconnecté !");
    }

    public synchronized static void broadcastToGroup(final CommunicationMessage message, final String group) {
        HashSet<Server> clients = clientsByGroups.get(group);

        if (clients != null) {
            for (Server cm : clients) {
                cm.sendData(message);
            }
        }

        for (Server server : admins) {
            server.sendData(message);
        }

        postLogMessage(String.format("Broadcast du message suivant (%s):\n%s", group, message.toFormattedString()));
    }

    public synchronized static void broadcast(final CommunicationMessage message) {
        Collection<HashSet<Server>> clients = clientsByID.values();
        System.out.println(clientsByID.values());
        for (HashSet<Server> clientList : clients) {
            for (Server s : clientList) {
                s.sendData(message);
            }
        }

        for (Server server : admins) {
            server.sendData(message);
        }

        postLogMessage(String.format("Broadcast du message suivant :\n%s", message.toFormattedString()));
    }

    public synchronized static void changeGroupName(String relatedGroup, String label) {
        HashSet<Server> servers = clientsByGroups.get(relatedGroup);
        if (servers != null) {
            clientsByGroups.remove(relatedGroup);
            clientsByGroups.put(label, servers);
        }

        postLogMessage(String.format("Changement du nom de groupe : %s -> %s", relatedGroup, label));
    }

    public synchronized static void sendToClient(Utilisateur user, CommunicationMessage message) {
        sendToClient(user.getID(), message);
        postLogMessage("Envoi du message suivant à " + user.getINE() + "\n" + message.toFormattedString());
    }

    public synchronized static void sendToClient(Long userID, CommunicationMessage message) {
        HashSet<Server> client = clientsByID.get(userID);
        if (client != null) {
            for (Server s : client) {
                s.sendData(message);
            }
        }
    }

    public synchronized static void addAdmin(Server server) {
        admins.add(server);
        ui.setAdminNumber(++nbAdmins);
    }

    public static synchronized void postLogMessage(String message) {
        ui.addLogMessage(message);
    }

    @Override
    public void run() {
        super.run();

        SwingUtilities.invokeLater(() -> {
            ui = new ServerStopUI(this);
        });

        isRunning = true;
        Debugger.logColorMessage(DBG_COLOR, "Server", "Host is running !");

        while (isRunning) {
            try {
                SSLSocket client = (SSLSocket) mServerSocket.accept();

                Debugger.logColorMessage(DBG_COLOR, "Server", "Connection detected");


                new ClientManager(client).start();
            } catch (IOException | Server.ServerInitializationFailedException e) {
                e.printStackTrace();
            }
        }

        try {
            DatabaseManager.getInstance().closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        System.out.println("CLOSING");
        try {
            mServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        isRunning = false;
    }
}
