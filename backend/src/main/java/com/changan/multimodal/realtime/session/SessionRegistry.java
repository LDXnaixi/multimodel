package com.changan.multimodal.realtime.session;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionRegistry {

    private final Map<String, ClientSession> sessions = new ConcurrentHashMap<>();

    public void register(ClientSession clientSession) {
        sessions.put(clientSession.getClientId(), clientSession);
    }

    public void remove(String clientId) {
        sessions.remove(clientId);
    }

    public Optional<ClientSession> find(String clientId) {
        return Optional.ofNullable(sessions.get(clientId));
    }

    public Collection<ClientSession> all() {
        return sessions.values();
    }

    public int count() {
        return sessions.size();
    }
}
