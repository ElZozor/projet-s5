package backend.server.client;


import backend.server.Server;

public class Client implements Server {

    private String mToken;

    public Client() {
    	
    }

    @Override
    public String getToken() {
        return mToken;
    }
}