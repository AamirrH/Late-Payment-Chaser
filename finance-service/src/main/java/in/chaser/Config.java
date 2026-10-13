package in.chaser;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class Config implements WebMvcConfigurer {
    @Value("${chaser.mode}") public String mode;
    @Value("${chaser.data-dir}") public String dataDir;
    @Value("${chaser.origin}") public String origin;
    @Value("${chaser.bank-rate}") public BigDecimal bankRate;
    @Value("${chaser.bank-rate-date}") public LocalDate bankRateDate;
    @Value("${chaser.default-credit-days}") public int defaultCreditDays;
    @Value("${chaser.demo-date}") public LocalDate demoDate;
    @Value("${chaser.ai-url}") public String aiUrl;
    @Value("${chaser.ai-secret}") public String aiSecret;
    @Value("${chaser.project-id}") public String projectId;
    @Value("${chaser.bucket}") public String bucket;
    public boolean demo() { return mode.equals("demo"); }
    @PostConstruct void init() throws Exception {
        if (!mode.equals("demo") && !mode.equals("cloud")) throw new IllegalStateException("APP_MODE must be demo or cloud");
        if (bankRate.signum() < 0 || bankRate.compareTo(BigDecimal.ONE) > 0 || defaultCreditDays < 0 || defaultCreditDays > 45)
            throw new IllegalStateException("Invalid finance configuration");
        if (!demo()) {
            if (projectId.isBlank() || bucket.isBlank() || !aiUrl.startsWith("https://"))
                throw new IllegalStateException("Cloud mode requires FIREBASE_PROJECT_ID, GCS_BUCKET, HTTPS AI_SERVICE_URL");
            if (FirebaseApp.getApps().isEmpty()) FirebaseApp.initializeApp(FirebaseOptions.builder()
                .setProjectId(projectId).setCredentials(GoogleCredentials.getApplicationDefault()).build());
        }
    }
    @Override public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins(origin).allowedMethods("GET","POST","PATCH","PUT","DELETE","OPTIONS")
            .allowedHeaders("Authorization","Content-Type");
    }
}
