package backend.server.host;

import backend.server.Server;

import java.io.IOException;
import java.net.ServerSocket;

public class Host extends Thread implements Server {

    public static final int PORT = 6666;
    public static Boolean isRunning = false;

    private ServerSocket mServerSocket;
    private String mToken;

    public Host() throws IOException {
        mServerSocket = new ServerSocket(PORT);
    }

    @Override
    public void run() {
        super.run();

        while (Host.isRunning) {
            try {
                mServerSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getToken() {
        return mToken;
    }
}
