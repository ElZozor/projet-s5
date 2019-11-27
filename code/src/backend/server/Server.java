package backend.server;

import jdk.internal.jline.internal.Nullable;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.Random;


public interface Server {

    // Theses are all the variables that will be used by both client
    // and server to create and receive messages

    String TYPE_CONNECTION          = "connection";
    String TYPE_REGISTRATION        = "registration";
    String TYPE_TICKET              = "ticket";
    String TYPE_MESSAGE             = "message";

    String TYPE_DATA                = "data";

    String CONNECTION_ID            = "id";
    String CONNECTION_PASSWORD      = "password";

    String REGISTRATION_ID          = "id";
    String REGISTRATION_PASSWORD    = "password";
    String REGISTRATION_NAME        = "name";
    String REGISTRATION_SURNAME     = "surname";

    String TICKET_ID                = "id";
    String TICKET_TITLE             = "title";
    String TICKET_MESSAGE           = "name";
    String TICKET_GROUPS            = "surname";

    String MESSAGE_ID               = "id";
    String MESSAGE_TICKET_ID        = "ticketid";
    String MESSAGE_CONTENTS         = "contents";


    String ALPHA_NUM_STRING = "AZERTYUIOPQSDFGHJKLMWXCVBNazertyuiopqsdfghjklmwxcvbn1234567890";

    int TOKEN_SIZE = 256;


    String getToken();


    /**
     * Sign a message with a token.
     *
     * @param data      The message
     * @param token     The token
     * @return          The signed message as a String
     */
    @Nullable
    default String signData(String data, String token) {
        //TODO Complete this
        if (token == null) {
            return null;
        }

        return aosijas;
    }


    /**
     * Create a standardized connection message that's signed and valid.
     * If the token is not defined, the result of this function will be null.
     *
     * @param id        The user id
     * @param password  The user password
     * @return          The signed message or null
     */
    @Nullable
    default String createConnectionMessage(String id, String password) {
        JSONObject connectionMessage = new JSONObject();
        connectionMessage.put("type", TYPE_CONNECTION);

        JSONObject data = new JSONObject();
        data.put(CONNECTION_ID, id);
        data.put(CONNECTION_PASSWORD, password);

        connectionMessage.put(TYPE_DATA, data);

        return signData(connectionMessage.toString(), getToken());
    }


    /**
     * Create a standardized registration message that's signed and valid.
     * If the token is not defined, the result of this function will be null.
     *
     * @param id        The user id
     * @param password  The user password
     * @param name      The user name
     * @param surname   The user surname
     * @return          The signed message or null
     */
    @Nullable
    default String createRegistrationMessage(String id, String password, String name, String surname) {
        JSONObject registrationMessage = new JSONObject();
        registrationMessage.put("type", TYPE_REGISTRATION);

        JSONObject data = new JSONObject();
        data.put(REGISTRATION_ID, id);
        data.put(REGISTRATION_PASSWORD, password);
        data.put(REGISTRATION_NAME, name);
        data.put(REGISTRATION_SURNAME, surname);

        registrationMessage.put(TYPE_DATA, data);

        return signData(registrationMessage.toString(), getToken());
    }


    /**
     * Create a standardized ticket message that's signed and valid.
     * If the token is not defined, the result of this function will be null.
     *
     * @param id        The user id
     * @param title     The ticket title
     * @param message   The ticket message
     * @param groups    The ticket groups
     *
     * @return          The signed message or null
     */
    @Nullable
    default String createTicketMessage(String id, String title, String message, String groups) {
        JSONObject registrationMessage = new JSONObject();
        registrationMessage.put("type", TYPE_REGISTRATION);

        JSONObject data = new JSONObject();
        data.put(TICKET_ID, id);
        data.put(TICKET_TITLE, title);
        data.put(TICKET_MESSAGE, message);
        data.put(TICKET_GROUPS, groups);

        registrationMessage.put(TYPE_DATA, data);

        return signData(registrationMessage.toString(), getToken());
    }


    /**
     * Create a standardized message that's signed and valid.
     * If the token is not defined, the result of this function will be null.
     *
     * @param id        The user id
     * @param ticketid  The ticket title
     * @param contents  The message contents
     *
     * @return          The signed message or null
     */
    @Nullable
    default String createMessage(String id, String ticketid, String contents) {
        JSONObject registrationMessage = new JSONObject();
        registrationMessage.put("type", TYPE_REGISTRATION);

        JSONObject data = new JSONObject();
        data.put(MESSAGE_ID, id);
        data.put(MESSAGE_TICKET_ID, ticketid);
        data.put(MESSAGE_CONTENTS, contents);

        registrationMessage.put(TYPE_DATA, data);

        return signData(registrationMessage.toString(), getToken());
    }




    /**
     * Generate a random token which has a length of TOKEN_SIZE
     *
     * @return The random token
     */
    @NotNull
    default String generateToken() {
        StringBuilder builder = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < TOKEN_SIZE; ++i) {
            builder.append(ALPHA_NUM_STRING.charAt(random.nextInt(ALPHA_NUM_STRING.length())));
        }

        return builder.toString();
    }

}
