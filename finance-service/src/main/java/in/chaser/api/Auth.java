package in.chaser.api;

import in.chaser.Config;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Auth {
    private record Session(String uid, Instant expires) {}
    private final Config config;
    private final Map<String,Session> sessions = new ConcurrentHashMap<>();
    public Auth(Config config) { this.config = config; }
    public Map<String,String> session() {
        if (!config.demo()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        sessions.entrySet().removeIf(e -> e.getValue().expires().isBefore(Instant.now()));
        if(sessions.size() >= 1000) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        String token = UUID.randomUUID() + "-" + UUID.randomUUID();
        String uid = "demo-" + UUID.randomUUID();
        sessions.put(token, new Session(uid, Instant.now().plusSeconds(86400)));
        return Map.of("token",token,"uid",uid,"mode","demo");
    }
    public String owner(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) throw unauthorized();
        String token = authorization.substring(7);
        if (config.demo()) {
            Session s = sessions.get(token);
            if (s == null || s.expires().isBefore(Instant.now())) throw unauthorized();
            return s.uid();
        }
        try { return FirebaseAuth.getInstance().verifyIdToken(token).getUid(); }
        catch (Exception ignored) { throw unauthorized(); }
    }
    private ResponseStatusException unauthorized() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sign in again to continue."); }
}
