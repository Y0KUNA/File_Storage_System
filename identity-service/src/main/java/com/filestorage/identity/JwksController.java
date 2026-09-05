package com.filestorage.identity;

import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
class JwksController {
    private final RSAKey rsaKey;

    JwksController(RSAKey rsaKey) {
        this.rsaKey = rsaKey;
    }

    @GetMapping("/.well-known/jwks.json")
    Map<String, Object> jwks() {
        return Map.of("keys", java.util.List.of(rsaKey.toPublicJWK().toJSONObject()));
    }
}
