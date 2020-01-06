package backend.server;

import backend.server.communication.CommunicationMessage;
import backend.server.communication.classic.ClassicMessage;
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

    /**
     * Used to send data through a socket.
     *
     * @param classicMessage The message to send
     * @throws IOException Exception if write has failed
     */
    default void sendData(CommunicationMessage classicMessage) throws IOException {
        BufferedWriter socketWriter = getSocketWriter();
        if (socketWriter == null) {
            throw new IOException();
        }

        Debugger.logMessage("Server sendData", "Sending following data: " + classicMessage.toFormattedString());
        socketWriter.write(classicMessage.toString());
        socketWriter.flush();
    }


    /**
     * Used to receive data from a socket.
     *
     * @return The data
     * @throws IOException Exception if read has failed
     */
    default ClassicMessage readData()
            throws IOException, ClassicMessage.InvalidMessageException, SocketDisconnectedException {

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

            ClassicMessage message = new ClassicMessage(line);
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
