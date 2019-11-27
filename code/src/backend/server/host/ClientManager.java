package backend.server.host;

import backend.database.DatabaseManager;
import backend.server.Server;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientManager extends Thread {

    private static final int BUFFER_SIZE = 256;

    private final Socket mSocket;
    private OutputStreamWriter mWriteStream;
    private InputStreamReader mReadStream;

    public ClientManager(final Socket socket) throws IOException {
        mSocket = socket;

        mWriteStream = new OutputStreamWriter(mSocket.getOutputStream(), StandardCharsets.UTF_8);
        mReadStream  = new InputStreamReader(mSocket.getInputStream(), StandardCharsets.UTF_8);
    }

    /**
     * Main function overridden from the parent class "Thread".
     * It will run in background until the Host.running value became "false"
     * or the main program stop.
     *
     * It handle the client message including connection, disconnection,
     * and all actions that the client can perform.
     */
    @Override
    public void run() {
        super.run();

        char[] buffer = new char[BUFFER_SIZE];
        StringBuilder jsonConstruct = new StringBuilder();

        while (Host.isRunning) {
            try {
                int nChar = BUFFER_SIZE;
                while (mReadStream.ready() && nChar == BUFFER_SIZE) {
                    nChar = mReadStream.read(buffer, 0, 256);

                    jsonConstruct.append(buffer);
                }

                JSONObject message = new JSONObject(jsonConstruct.toString());
                if (message.has("type")) {
                    handleMessage(message);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Handle a message sent by the client.
     * You must check that the message contains the key "type"
     * before send it to this function.
     *
     * Otherwise, the function will crash..
     *
     * It will check the message "type" value and redirect it
     * to the proper function that must handle the action.
     *
     * @param message The message to handle
     */
    private void handleMessage(JSONObject message) {

        switch (message.getString("type")) {
            case Server.TYPE_CONNECTION :
                handleConnection(message);
                break;

            case Server.TYPE_REGISTRATION :
                handleRegistration(message);
                break;

            case Server.TYPE_TICKET :
                handleTicket(message);
                break;

            case Server.TYPE_MESSAGE :
                handleClassicMessage(message);
                break;

            default:
                break;
        }

    }

    private void handleConnection(JSONObject messageData) {
        if (messageData.has("username") && messageData.has("password")) {
            DatabaseManager.getInstance().checkUserPresence(
                    messageData.optString(Server.CONNECTION_ID),
                    messageData.optString(Server.CONNECTION_PASSWORD)
            );
        } else {
            sendResponse();
        }
    }
}
