package launch;

import backend.server.Server;
import backend.server.client.Client;
import debug.Debugger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class ClientLaunch {

    public static void main(String[] args) {
        Debugger.isDebugging = true;

        try {
            Client client = new Client(new Socket("localhost", 6666));
            // client.sendRegistrationMessage("monnumeroetudiant", "monmotdepasse", "monnom", "monprenom");
        } catch (IOException | Server.ServerInitializationFailedException e) {
            // Do something on client connection refused
            e.printStackTrace();
        }
    }

}
