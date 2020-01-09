package backend.server;

import backend.server.communication.CommunicationMessage;
import debug.Debugger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.SocketTimeoutException;


public interface Server {

    String ERROR_MESSAGE_HANDLE_DEMAND = "Le serveur ne peut pas traiter cette demande.";
    String ERROR_MESSAGE_DATABASE_ERROR = "La base de donnée a rencontré une erreur.";
    String ERROR_MESSAGE_SERVER_ERROR = "Le serveur a recontré une erreur.";
    String ERROR_MESSAGE_EMPTY_FIELD = "Tous les champs doivent être correctement remplis !";

    BufferedWriter getSocketWriter();

    BufferedReader getSocketReader();

    void addPendingMessage(CommunicationMessage message);

    /**
     * Used to send data through a socket.
     *
     * @param communicationMessage The message to send
     */
    default boolean sendData(CommunicationMessage communicationMessage) {
        try {

            BufferedWriter socketWriter = getSocketWriter();
            if (socketWriter == null) {
                throw new IOException();
            }

            Debugger.logMessage("Server sendData", "Sending following data: " + communicationMessage.toFormattedString());
            socketWriter.write(communicationMessage.toString());
            socketWriter.flush();

            return true;

        } catch (IOException e) {
            addPendingMessage(communicationMessage);
            return false;
        }

    }


    /**
     * Used to receive data from a socket.
     *
     * @return The data
     * @throws IOException Exception if read has failed
     */
    default CommunicationMessage readData()
            throws IOException, CommunicationMessage.InvalidMessageException, SocketDisconnectedException {

        String line;
        BufferedReader socketReader = getSocketReader();

        if (socketReader == null) {
            return null;
        }

        try {
            line = socketReader.readLine();
            if (line == null) {
                throw new SocketDisconnectedException();
            }

            CommunicationMessage message = new CommunicationMessage(line);
            Debugger.logColorMessage(Debugger.GREEN, "Server", "Received data: \n" + message.toFormattedString());
            return message;
        } catch (SocketTimeoutException e) {
            System.out.println("Socket read timeout !");
        }

        return null;
    }


    class ServerInitializationFailedException extends Exception {
        public ServerInitializationFailedException() {
            super();
        }

        public ServerInitializationFailedException(String message) {
            super(message);
        }
    }

    class SocketDisconnectedException extends Exception {
        public SocketDisconnectedException() {
            super();
        }

        public SocketDisconnectedException(String message) {
            super(message);
        }
    }


}
