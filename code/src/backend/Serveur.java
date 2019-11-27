package backend;

import com.auth0.jwt.interfaces.JWTVerifier;

public interface Serveur {

    default String signData(String data, String token) {
        String result = "";

        
        return result;
    }

}
