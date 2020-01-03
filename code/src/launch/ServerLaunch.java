package launch;

import backend.server.host.Host;
import debug.Debugger;

import java.io.IOException;

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
            Host host = new Host();
            host.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
