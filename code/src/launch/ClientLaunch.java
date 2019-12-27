package launch;

import backend.server.Server;
import backend.server.client.Client;
import debug.Debugger;
import ui.Client.ConnexionScreen;

import javax.swing.*;
import java.io.IOException;
import java.net.Socket;

public class ClientLaunch {

    public static Client client;

    public static void main(String[] args) {
        Debugger.isDebugging = true;

        try {
            client = new Client(new Socket("localhost", 6666));
            SwingUtilities.invokeLater(ConnexionScreen::new);
        } catch (IOException | Server.ServerInitializationFailedException e) {
            // Do something on client connection refused
            JOptionPane.showMessageDialog(null, "Connexion au serveur impossible !", "Erreur", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

}
