package backend.server.host;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class Host extends Thread {

    public static final String DBG_COLOR = Debugger.RED;

    public static final int PORT = 6666;
    public static Boolean isRunning = false;
    private static HashMap<String, HashSet<Server>> clientsByGroups = new HashMap<>();
    private static HashMap<Long, Server> clientsByID = new HashMap<>();
    private SSLServerSocket mServerSocket;

    public Host() throws IOException {
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        mServerSocket = (SSLServerSocket) factory.createServerSocket(PORT);
    }

    public static void addClient(Collection<String> groups, Long clientID, ClientManager client) {
        for (String group : groups) {
            HashSet<Server> clientSet = clientsByGroups.computeIfAbsent(group, k -> new HashSet<>());

            clientSet.add(client);
        }

        clientsByID.put(clientID, client);
    }

    public static void removeClient(Collection<String> groups, Long clientID, Server client) {
        for (String group : groups) {
            clientsByGroups.get(group).remove(client);
        }

        clientsByID.remove(clientID);
    }

    public static void broadcastToGroup(final ClassicMessage message, final String group) {
        HashSet<Server> clients = clientsByGroups.get(group);

        if (clients != null) {
            for (Server cm : clients) {
                try {
                    cm.sendData(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void broadcast(final ClassicMessage message) {
        Collection<Server> clients = clientsByID.values();
        for (Server s : clients) {
            try {
                s.sendData(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void changeGroupName(String relatedGroup, String label) {
        HashSet<Server> servers = clientsByGroups.get(relatedGroup);
        if (servers != null) {
            clientsByGroups.remove(relatedGroup);
            clientsByGroups.put(label, servers);
        }
    }

    public static void sendToClient(Long userID, ClassicMessage message) {
        Server client = clientsByID.get(userID);
        if (client != null) {
            try {
                client.sendData(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
    }
}
