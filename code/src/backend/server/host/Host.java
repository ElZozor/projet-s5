package backend.server.host;

import backend.server.Server;
import debug.Debugger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class Host extends Thread {

    public static final String DBG_COLOR = Debugger.RED;

    public static final int PORT = 6666;
    public static Boolean isRunning = false;

    private ServerSocket mServerSocket;

    private static HashMap<String, HashSet<ClientManager>> clientsByGroups = new HashMap<>();

    public Host() throws IOException {
        mServerSocket = new ServerSocket(PORT);
    }

    @Override
    public void run() {
        super.run();
        isRunning = true;
        Debugger.logColorMessage(DBG_COLOR, "Server", "Host is running !");

        while (Host.isRunning) {
            try {
                Socket client = mServerSocket.accept();
                Debugger.logColorMessage(DBG_COLOR, "Server", "Connection detected");

                new ClientManager(client).start();
            } catch (IOException | Server.ServerInitializationFailedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void addClient(Collection<String> groups, ClientManager client) {
        for (String group : groups) {
            HashSet<ClientManager> clientSet = clientsByGroups.computeIfAbsent(group, k -> new HashSet<>());

            clientSet.add(client);
        }
    }

    public static void removeClient(Collection<String> groups, ClientManager client) {
        for (String group : groups) {
            clientsByGroups.get(group).remove(client);
        }
    }
}
