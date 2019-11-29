package backend.server.host;

import backend.server.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Host extends Thread implements Server {

    public static final int PORT = 6666;
    public static Boolean isRunning = false;

    private ServerSocket mServerSocket;

    private KeyPair mRSAKey;
    private PublicKey mOtherPublicKey;

    public Host() throws IOException, NoSuchAlgorithmException {
        mServerSocket = new ServerSocket(PORT);

        mRSAKey = Server.generateRSAKey();
    }

    @Override
    public void run() {
        super.run();

        while (Host.isRunning) {
            try {
                Socket client = mServerSocket.accept();
                new ClientManager(client);
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public PrivateKey getPrivateKey() {
        return mRSAKey.getPrivate();
    }

    @Override
    public PublicKey getPublicKey() {
        return mRSAKey.getPublic();
    }

    @Override
    public PublicKey getOtherPublicKey() {
        return mOtherPublicKey;
    }
}
