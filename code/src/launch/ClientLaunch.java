package launch;

import backend.server.client.Client;
import debug.Debugger;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class ClientLaunch {

    public static void main(String[] args) {
        Debugger.isDebugging = true;

        try {
            Client client = new Client(new Socket("localhost", 6666));
            client.sendRegistrationMessage("monnumeroetudiant", "monmotdepasse", "monnom", "monprenom");
        } catch (IOException e) {
            // Do something on client connection refused
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // Do something on RSA algorithm refused
            e.printStackTrace();
        }
    }

}
