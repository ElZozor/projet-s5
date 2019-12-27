package backend.server;

import backend.server.message.Message;
import debug.Debugger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.*;


public interface Server {

    // Theses are all the variables that will be used by both client
    // and server to create and receive messages


    int BUFFER_SIZE = 256;


    PrivateKey getPrivateKey();

    PublicKey getPublicKey();

    PublicKey getOtherPublicKey();


    /**
     * Generate a random token which has a length of TOKEN_SIZE
     *
     * @return The random token
     */
    default KeyPair generateRSAKey() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());

        return generator.generateKeyPair();
    }


    /**
     * Used to send data through a socket.
     *
     * @param socketWriter The socket output stream
     * @param message      The message to send
     * @throws IOException Exception if write has failed
     */
    default void sendData(BufferedWriter socketWriter, Message message) throws IOException {
        Debugger.logMessage("Server sendData", "Sending following data: " + message.toString());
        System.out.println(message.encode());
        socketWriter.write(message.encode());
        socketWriter.flush();
    }


    /**
     * Used to receive data from a socket.
     *
     * @param socketReader The socket input reader
     * @return The data
     * @throws IOException Exception if read has failed
     */
    default Message readData(BufferedReader socketReader)
            throws IOException, Message.InvalidMessageException, SocketDisconnectedException {
        String line;

        try {
            line = socketReader.readLine();
            if (line == null) {
                throw new SocketDisconnectedException();
            }

            return new Message(line, getOtherPublicKey(), getPrivateKey());
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
