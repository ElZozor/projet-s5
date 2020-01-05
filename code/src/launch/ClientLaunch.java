package launch;

import backend.server.Server;
import backend.server.client.Client;
import debug.Debugger;
import ui.Client.ConnexionScreen;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.swing.*;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class ClientLaunch {

    public static Client client;

    public static void main(String[] args) {
        final String keypass = "https://www.youtube.com/watch?v=l4_JIIrMhIQ";

        System.setProperty("javax.net.ssl.keyStore", "res/keystore");
        System.setProperty("javax.net.ssl.trustStore", "res/keystore");
        System.setProperty("javax.net.ssl.keyStorePassword", "passphrase");
        System.setProperty("javax.net.ssl.trustStorePassword", "passphrase");

        Debugger.isDebugging = true;

        try {
            client = new Client((SSLSocket) SSLContext.getDefault().getSocketFactory().createSocket("localhost", 6666));
            SwingUtilities.invokeLater(() -> new ConnexionScreen(client, false));
        } catch (IOException | Server.ServerInitializationFailedException | NoSuchAlgorithmException e) {
            // Do something on client connection refused
            JOptionPane.showMessageDialog(null, "Connexion au serveur impossible !", "Erreur", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

}
