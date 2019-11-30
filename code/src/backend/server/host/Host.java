package backend.server.host;

import debug.Debugger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class Host extends Thread {

    public static final String DBG_COLOR = Debugger.RED;

    public static final int PORT = 6666;
    public static Boolean isRunning = false;

    private ServerSocket mServerSocket;

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
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }
}
