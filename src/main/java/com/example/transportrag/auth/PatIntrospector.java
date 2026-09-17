package com.example.transportrag.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;

@Component
public class PatIntrospector implements OpaqueTokenIntrospector {
    private final PatMapper mapper;

    public PatIntrospector(PatMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        String subject = mapper.findActiveSubject(hash(token));
        if (subject == null) {
            throw new BadOpaqueTokenException("Invalid personal access token");
        }
        return new DefaultOAuth2AuthenticatedPrincipal(subject, Map.of("sub", subject),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
