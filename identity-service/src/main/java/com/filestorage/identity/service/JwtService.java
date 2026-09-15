package com.filestorage.identity.service;


import com.filestorage.identity.config.*;
import com.filestorage.identity.controller.*;
import com.filestorage.identity.domain.*;
import com.filestorage.identity.dto.*;
import com.filestorage.identity.messaging.*;
import com.filestorage.identity.repository.*;
import com.filestorage.identity.service.*;
import com.filestorage.identity.web.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class JwtService {
    private final JwtEncoder encoder;
    private final String issuer;
    private final long accessTokenTtlSeconds;

    public JwtService(JwtEncoder encoder,
               @Value("${app.jwt.issuer:file-storage-identity}") String issuer,
               @Value("${app.jwt.access-token-ttl-seconds:900}") long accessTokenTtlSeconds) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

   public  String accessToken(UserAccount user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTokenTtlSeconds))
                .subject(user.id.toString())
                .claim("role", user.role.name())
                .claim("tokenVersion", user.tokenVersion)
                .build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
