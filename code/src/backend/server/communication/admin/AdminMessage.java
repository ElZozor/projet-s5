package backend.server.communication.admin;

import backend.server.communication.CommunicationMessage;
import org.json.JSONObject;

import static backend.server.communication.classic.ClassicMessage.*;

public class AdminMessage extends CommunicationMessage {


    private ADMIN_MESSAGE_TYPE ADMIN_MESSAGE_type;

    private AdminMessage(ADMIN_MESSAGE_TYPE msg_type, final String type) {
        this.ADMIN_MESSAGE_type = msg_type;
        setTypeString(type);
    }


    public AdminMessage(String data) throws InvalidMessageException {
        if (data == null || !isValidJSON(data)) {
            throw new InvalidMessageException("Data cannot be decoded or JSON is invalid");
        }

        JSONObject jsonData = new JSONObject(data);

        if (!isValid(jsonData)) {
            throw new InvalidMessageException("Trying to create a Message Object with invalid data");
        }

        setTypeString(jsonData.getString(TYPE));
        ;
        setData(jsonData.getJSONObject(DATA));

        ADMIN_MESSAGE_type = guessType();
    }

    private ADMIN_MESSAGE_TYPE guessType() throws InvalidMessageException {

        checkValidity();

        switch (getTypeToString()) {
            case TYPE_DELETE:
                return ADMIN_MESSAGE_TYPE.DELETE;

            case TYPE_ADD:
                return ADMIN_MESSAGE_TYPE.ADD;

            case TYPE_UPDATE:
                return ADMIN_MESSAGE_TYPE.EDIT;

            default:
                throw new InvalidMessageException("Message with invalid type: " + getTypeToString());
        }

    }

    public ADMIN_MESSAGE_TYPE getType() {
        return ADMIN_MESSAGE_type;
    }

    private void checkValidity() throws InvalidMessageException {
        if (!getData().has(ENTRY) || !getData().has(TABLE)) {
            throw new InvalidMessageException("Missing field in deletion message");
        }
    }

}
