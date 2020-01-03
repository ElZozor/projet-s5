package backend.server.client;


import backend.server.Server;
import backend.server.communication.CommunicationMessage;
import debug.Debugger;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.Date;

public class Client implements Server {

    private final static String DBG_COLOR = Debugger.YELLOW;
    private final static int SOCKET_TIMEOUT = 5000;

    private final SSLSocket mSocket;
    boolean isRunning = true;
    private BufferedWriter mWriteStream;

    private BufferedReader mReadStream;

    /**
     * This class is used on the client side.
     * It's used to communicate with the host.
     *
     * @param socket The connexion socket
     * @throws ServerInitializationFailedException When the server can't be init
     */
    public Client(SSLSocket socket) throws ServerInitializationFailedException {
        try {
            mSocket = socket;

            mSocket.setSoTimeout(SOCKET_TIMEOUT);

            mWriteStream = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
            mReadStream = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));


        } catch (IOException e) {
            e.printStackTrace();
            throw new ServerInitializationFailedException("Something went wrong while initializing connexion");
        }
    }


    /**
     * A synchronized function that send a message and wait for the return value.
     *
     * @param communicationMessage The message you want to send
     * @return The data send by the host
     * @throws IOException Can be thrown while writing/reading into the fd
     */
    public CommunicationMessage sendAndWaitForReturn(CommunicationMessage communicationMessage) throws IOException {
        sendData(mWriteStream, communicationMessage);

        try {
            return readData(mReadStream);
        } catch (IOException | CommunicationMessage.InvalidMessageException e) {
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
     * @param INE      The user INE
     * @param password The user password
     * @return
     */
    public CommunicationMessage sendConnectionMessage(String INE, String password) {

        CommunicationMessage returnedData = null;

        try {
            returnedData = sendAndWaitForReturn(
                    CommunicationMessage.createConnection(INE, password)
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
    public CommunicationMessage createANewTicket(String title, String messageContent, String group) {

        CommunicationMessage returnedData = null;

        try {

            returnedData = sendAndWaitForReturn(
                    CommunicationMessage.createTicket(title, group, messageContent)
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
    public CommunicationMessage postAMessage(String ticketid, String contents) {

        CommunicationMessage returnedData = null;

        try {

            returnedData = sendAndWaitForReturn(
                    CommunicationMessage.createMessage(
                            ticketid,
                            contents
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
    public CommunicationMessage updateLocalDatabase() {

        CommunicationMessage returnedData = null;

        try {

            returnedData = sendAndWaitForReturn(
                    CommunicationMessage.createLocalUpdate(new Date(0))
            );

        } catch (IOException e) {
            e.printStackTrace();
        }

        return returnedData;

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




}