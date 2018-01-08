package org.bch.c3pro.server.external;

import org.bch.c3pro.server.exception.C3PROException;

import java.security.PublicKey;

/**
 * Interface for queue access
 * @author CHIP-IHL
 */
public interface Queue {
    public void sendMessageEncrypted(String resource, PublicKey key, String UUIDKey, String version)
            throws C3PROException;
}
