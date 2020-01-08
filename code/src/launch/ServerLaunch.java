package launch;

import backend.database.DatabaseManager;
import backend.server.host.Host;
import utils.Utils;

import javax.swing.*;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public class ServerLaunch {

    public static void main(String[] args) {
        Utils.setSystemProperties();
//        Debugger.isDebugging = true;

        boolean successfulyLaunched = false;
        try {
            DatabaseManager.initDatabaseConnection();
            DatabaseManager manager = DatabaseManager.getInstance();
            if (!manager.userExists("admin")) {
                manager.registerNewUser("admin", "admin", "admin", "admin", "admin", "admin");
            }

            try {
                final Host host = new Host();
                host.start();
                successfulyLaunched = true;
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (SQLException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

        if (!successfulyLaunched) {
            JOptionPane.showMessageDialog(null,
                    "Impossible de lancer le serveur",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

}
