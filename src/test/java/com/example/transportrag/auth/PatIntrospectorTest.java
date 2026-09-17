package com.example.transportrag.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PatIntrospectorTest {
    @Test
    void rejectsUnknownOrInactiveToken() {
        PatMapper mapper = mock(PatMapper.class);
        assertThatThrownBy(() -> new PatIntrospector(mapper).introspect("unknown"))
                .isInstanceOf(BadOpaqueTokenException.class);
    }

    @Test
    void usesHashAndReturnsDatabaseSubject() {
        PatMapper mapper = mock(PatMapper.class);
        String expectedHash = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        when(mapper.findActiveSubject(expectedHash)).thenReturn("demo-user");
        assertThat(new PatIntrospector(mapper).introspect("abc").getName()).isEqualTo("demo-user");
        verify(mapper).findActiveSubject(expectedHash);
    }
}
