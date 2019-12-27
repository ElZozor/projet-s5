package backend.server.client;


import backend.server.Server;
import backend.server.message.Message;
import debug.Debugger;

import java.io.*;
import java.net.Socket;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Client implements Server {

    private final static String DBG_COLOR = Debugger.YELLOW;
    private final static int SOCKET_TIMEOUT = 5000;

    private final Socket mSocket;
    boolean isRunning = true;
    private BufferedWriter mWriteStream;

    private KeyPair mRSAKey;
    private PublicKey mOtherPublicKey;
    private BufferedReader mReadStream;

    /**
     * This class is used on the client side.
     * It's used to communicate with the host.
     *
     * @param socket The connexion socket
     * @throws ServerInitializationFailedException When the server can't be init
     */
    public Client(Socket socket) throws ServerInitializationFailedException {
        try {
            mSocket = socket;
            mSocket.setSoTimeout(SOCKET_TIMEOUT);

            mWriteStream = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
            mReadStream = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

            exchangesKeys();

            Debugger.logColorMessage(DBG_COLOR, "Client", "received: " + mOtherPublicKey);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new ServerInitializationFailedException("Something went wrong while initializing connexion");
        }
    }

    private void exchangesKeys() throws NoSuchAlgorithmException, IOException, ServerInitializationFailedException {
        mRSAKey = generateRSAKey();

        // Send the public key and wait for the returned key
        mWriteStream.write(Message.createKeyXChangeMessage(getPublicKey()));
        mWriteStream.flush();

        mOtherPublicKey = Message.getKeyXChangePublicKey(mReadStream.readLine());
        if (mOtherPublicKey == null) {
            throw new ServerInitializationFailedException();
        }
    }


    /**
     * A synchronized function that send a message and wait for the return value.
     *
     * @param message The message you want to send
     * @return The data send by the host
     * @throws IOException Can be thrown while writing/reading into the fd
     */
    public Message sendAndWaitForReturn(Message message) throws IOException {
        sendData(mWriteStream, message);

        try {
            return readData(mReadStream);
        } catch (IOException | Message.InvalidMessageException e) {
            e.printStackTrace();

            return null;
        } catch (SocketDisconnectedException e) {
            isRunning = false;
        }

        return null;
    }



    /**
     * Function used to send a connection message to the host.
     * Will return the message received from the host.
     *
     * @param INE           The user INE
     * @param password      The user password
     * @return
     */
    public Message sendConnectionMessage(String INE, String password) {

        Message returnedData = null;

        try {
            returnedData = sendAndWaitForReturn(
                    Message.createConnectionMessage(INE, password, getOtherPublicKey())
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return returnedData;

    }


    /**
     * This function is used when the user want end the communication.
     * Will return the message received from the host.
     *
     * @throws IOException  Can be thrown while closing the socket.
     */
    public void disconnect() throws IOException {

        mWriteStream.close();
        mReadStream.close();
        mSocket.close();

    }


    /**
     * This function is used to create a new ticket.
     * Will return the message received from the host.
     *
     * @param title                 The ticket title
     * @param messageContent        The message contents
     * @param group                 The concerned group
     * @return The message received from the host
     */
    public Message createANewTicket(String title, String messageContent, String group) {

        Message returnedData = null;

        try {

            returnedData = sendAndWaitForReturn(
                    Message.createTicketMessage(title, group, messageContent, getOtherPublicKey())
            );

        } catch (IOException e) {
            e.printStackTrace();
        }

        return returnedData;

    }


    /**
     * Used to post a message into a ticket.
     * Will return the data retrieved by the host.
     *
     * @param ticketid  The ticket id.
     * @param contents  The contents;
     * @return The data retrieved by the host.
     */
    public Message postAMessage(String ticketid, String contents) {

        Message returnedData = null;

        try {

            returnedData = sendAndWaitForReturn(
                    Message.createMessageMessage(
                            ticketid,
                            contents,
                            getOtherPublicKey()
                    )
            );

        } catch (IOException e) {
            e.printStackTrace();
        }

        return returnedData;

    }


    /**
     * This function is used to update the local database
     * from the host database.
     * It will send an "udpateMessage" to the host with
     * the last update date and the host will retrieve
     * all the messages / tickets that are newer than the
     * given date.
     *
     * @return The data retrieved by the server.
     */
    public String updateLocalDatabase() {

        // TODO

        return null;
    }


    /**
     * This function is used to register a new message
     * into the local database and to add it into the UI.
     *
     * @param message   The data received from the host
     */
    public void receiveNewMessage(String message) {

        // TODO

    }

    /**
     * This function is used to update a message both
     * in the ui and in the local database.
     *
     * @param message   The data received from the host
     */
    public void updateMessageData(String message) {

        // TODO

    }





    @Override
    public PrivateKey getPrivateKey() {
        return mRSAKey.getPrivate();
    }

    @Override
    public PublicKey getPublicKey() {
        return mRSAKey.getPublic();
    }

    @Override
    public PublicKey getOtherPublicKey() {
        return mOtherPublicKey;
    }
}