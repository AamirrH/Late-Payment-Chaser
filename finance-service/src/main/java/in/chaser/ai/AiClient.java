package in.chaser.ai;

import in.chaser.Config;
import in.chaser.api.Models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.*;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;

@Component
public class AiClient {
    private final Config config;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    public AiClient(Config config, ObjectMapper json) { this.config=config; this.json=json; }
    private <T> T post(String endpoint, Object data, Class<T> type) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create(config.aiUrl+endpoint)).timeout(Duration.ofSeconds(25))
            .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data)));
        if (!config.aiSecret.isBlank()) builder.header("X-Service-Secret",config.aiSecret);
        if (!config.demo()) {
            var credentials=GoogleCredentials.getApplicationDefault();
            if (!(credentials instanceof IdTokenProvider provider)) throw new IllegalStateException("Cloud Run identity required");
            String token=IdTokenCredentials.newBuilder().setIdTokenProvider(provider).setTargetAudience(config.aiUrl).build().refreshAccessToken().getTokenValue();
            builder.header("Authorization","Bearer "+token);
        }
        var response=http.send(builder.build(),HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200) throw new IllegalStateException("AI service unavailable");
        return json.readValue(response.body(),type);
    }
    public Draft draft(DraftRequest request) throws Exception { return post("/ai/draft",request,Draft.class); }
    public Extracted extract(byte[] bytes,String mime) throws Exception {
        return post("/ai/extract",Map.of("contentBase64",java.util.Base64.getEncoder().encodeToString(bytes),"mimeType",mime),Extracted.class);
    }
}
