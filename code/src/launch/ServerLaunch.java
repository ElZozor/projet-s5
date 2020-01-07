package launch;

import backend.database.DatabaseManager;
import backend.server.host.Host;
import debug.Debugger;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public class ServerLaunch {

    public static void main(String[] args) {
        final String keypass = "https://www.youtube.com/watch?v=l4_JIIrMhIQ";

        System.setProperty("javax.net.ssl.keyStore", "res/keystore");
        System.setProperty("javax.net.ssl.trustStore", "res/keystore");
        System.setProperty("javax.net.ssl.keyStorePassword", "passphrase");
        System.setProperty("javax.net.ssl.trustStorePassword", "passphrase");

        //Enter in debugging mode
        Debugger.isDebugging = true;

        try {
            DatabaseManager.initDatabaseConnection();
            DatabaseManager manager = DatabaseManager.getInstance();
            if (!manager.userExists("admin")) {
                manager.registerNewUser("admin", "admin", "admin", "admin", "admin", "admin");
            }

            try {
                Host host = new Host();
                host.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (SQLException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

}
