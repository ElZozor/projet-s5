package backend.server.client;


import backend.server.Server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Client implements Server {

    private final Socket mSocket;
    private OutputStreamWriter mWriteStream;
    private InputStreamReader mReadStream;

    private KeyPair mRSAKey;
    private PublicKey mOtherPublicKey;


    public Client(Socket socket) throws NoSuchAlgorithmException, IOException {
        mSocket = socket;
        mWriteStream = new OutputStreamWriter(mSocket.getOutputStream());
        mReadStream = new InputStreamReader(mSocket.getInputStream());

        mRSAKey = Server.generateRSAKey();

        sendData(mWriteStream, Server.createKeyExchangeMessage(getPublicKey()));
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
        return null;
    }
}