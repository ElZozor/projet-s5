package backend.server.host;

import backend.server.Server;
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

    private SSLServerSocket mServerSocket;

    private static HashMap<String, HashSet<ClientManager>> clientsByGroups = new HashMap<>();

    public Host() throws IOException {
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        mServerSocket = (SSLServerSocket) factory.createServerSocket(PORT);
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
