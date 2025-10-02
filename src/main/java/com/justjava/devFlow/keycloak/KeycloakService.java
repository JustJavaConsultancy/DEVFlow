package com.justjava.devFlow.keycloak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakService {
    private final KeycloakFeignClient keycloakFeignClient;

    @Value("${keycloak.client-id}")
    String clientId;

    @Value("${keycloak.client-secret}")
    String clientSecret;

    @Value("${keycloak.grant_type}")
    String grantType;

    public KeycloakService(KeycloakFeignClient keycloakFeignClient) {
        this.keycloakFeignClient = keycloakFeignClient;
    }

    public Map getAccessToken(){
        Map<String,String> parmMaps= new HashMap<>();
        parmMaps.put("client_id",clientId);
        parmMaps.put("client_secret",clientSecret);
        parmMaps.put("grant_type",grantType);
        Map accessToken = keycloakFeignClient.getAccessToken(parmMaps);
        return accessToken ;
    }

    public ResponseEntity<Void> createUser(Map<String, Object> params){
        Map<String, Object> user = new HashMap<>();
        user.put("username", params.get("username"));
        user.put("email", params.get("email"));
        user.put("enabled", params.get("status"));

        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", "1234");
        credential.put("temporary", true);
        user.put("credentials", List.of(credential));

        Map tokens = getAccessToken();
        String accessToken ="Bearer "+ tokens.get("access_token");
        ResponseEntity<Void> response = keycloakFeignClient.createUser(accessToken, user);
        return response;
    }

    public List<Map<String, Object>> getUsersByEmail(String email){
        Map tokens = getAccessToken();
        String accessToken ="Bearer "+ tokens.get("access_token");

        List<Map<String, Object>> userByEmail = keycloakFeignClient.getUserByEmail(accessToken, email);
        return userByEmail;
    }
}
