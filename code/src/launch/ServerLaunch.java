package launch;

import backend.server.host.Host;
import debug.Debugger;

import java.io.IOException;

public class ServerLaunch {

    public static void main(String[] args) {
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
