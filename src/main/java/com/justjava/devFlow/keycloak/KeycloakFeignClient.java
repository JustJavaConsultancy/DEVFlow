package com.justjava.devFlow.keycloak;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name="KeycloakFeignClient",url="${keycloak.base-url}")
public interface KeycloakFeignClient {

    @PostMapping(path = "/realms/softwareEngineer/protocol/openid-connect/token",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    Map<String, Object> getAccessToken(Map<String,?> paramMap);

    @PostMapping("/admin/realms/softwareEngineer/users")
    ResponseEntity<Void> createUser(@RequestHeader(value = "Authorization")
                                    String authorizationHeader, Map<String,Object> user);

    @GetMapping("/admin/realms/softwareEngineer/users")
    List<Map<String, Object>> getUserByEmail(
            @RequestHeader("Authorization") String bearerToken,
            @RequestParam("username") String emailOrUsername
    );

    @PutMapping("/admin/realms/softwareEngineer/users/{userId}")
    ResponseEntity<Void> updateUser(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @PathVariable String userId,
            @RequestBody Map<String, Object> body);

    @DeleteMapping("/admin/realms/softwareEngineer/users/{userId}")
    void deleteUser(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @PathVariable String userId
    );
}
