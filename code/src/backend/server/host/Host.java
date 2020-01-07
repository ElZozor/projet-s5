package backend.server.host;

import backend.database.DatabaseManager;
import backend.server.Server;
import backend.server.communication.classic.ClassicMessage;
import debug.Debugger;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class Host extends Thread {

    public static final String DBG_COLOR = Debugger.RED;

    public static final int PORT = 6666;
    public static Boolean isRunning = false;
    private static HashMap<String, HashSet<Server>> clientsByGroups = new HashMap<>();
    private static HashMap<Long, HashSet<Server>> clientsByID = new HashMap<>();
    private static ArrayList<Server> admins = new ArrayList<>();
    private SSLServerSocket mServerSocket;

    public Host() throws IOException {
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        mServerSocket = (SSLServerSocket) factory.createServerSocket(PORT);
    }

    public synchronized static void addClient(Collection<String> groups, Long clientID, ClientManager client) {
        for (String group : groups) {
            HashSet<Server> clientSet = clientsByGroups.computeIfAbsent(group, k -> new HashSet<>());

            clientSet.add(client);
        }

        if (!clientsByID.containsKey(clientID)) {
            clientsByID.put(clientID, new HashSet<>());
        }

        clientsByID.get(clientID).add(client);
    }


    public synchronized static void removeClient(Collection<String> groups, Long clientID, Server client) {
        for (String group : groups) {
            HashSet<Server> set = clientsByGroups.get(group);
            if (set != null) {
                set.remove(client);
            }
        }

        HashSet<Server> set = clientsByID.get(clientID);
        set.remove(client);
        admins.remove(client);
    }

    public synchronized static void broadcastToGroup(final ClassicMessage message, final String group) {
        HashSet<Server> clients = clientsByGroups.get(group);

        if (clients != null) {
            for (Server cm : clients) {
                cm.sendData(message);
            }
        }

        for (Server server : admins) {
            server.sendData(message);
        }
    }

    public synchronized static void broadcast(final ClassicMessage message) {
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
    }

    public synchronized static void changeGroupName(String relatedGroup, String label) {
        HashSet<Server> servers = clientsByGroups.get(relatedGroup);
        if (servers != null) {
            clientsByGroups.remove(relatedGroup);
            clientsByGroups.put(label, servers);
        }
    }

    public synchronized static void sendToClient(Long userID, ClassicMessage message) {
        HashSet<Server> client = clientsByID.get(userID);
        if (client != null) {
            for (Server s : client) {
                s.sendData(message);
            }

        }
    }

    public synchronized static void addAdmin(Server server) {
        admins.add(server);
    }

    public synchronized static void removeAdmin(Server server) {
        admins.remove(server);
    }

    @Override
    public void run() {
        super.run();
        isRunning = true;
        Debugger.logColorMessage(DBG_COLOR, "Server", "Host is running !");

        try {
            SSLContext context = SSLContext.getDefault();

            SocketFactory factory = context.getSocketFactory();

            while (Host.isRunning) {
                try {
                    SSLSocket client = (SSLSocket) mServerSocket.accept();

                    Debugger.logColorMessage(DBG_COLOR, "Server", "Connection detected");


                    new ClientManager(client).start();
                } catch (IOException | Server.ServerInitializationFailedException e) {
                    e.printStackTrace();
                }
            }

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        }

        try {
            DatabaseManager.getInstance().closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
