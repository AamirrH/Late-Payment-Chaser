package in.chaser.store;

import in.chaser.Config;
import in.chaser.api.Models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.storage.*;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.util.*;

/** Local disk for explicit demo mode; Firestore and private GCS for cloud mode. */
@Component
public class CaseStore {
    private final Config config;
    private final ObjectMapper json;
    public CaseStore(Config config, ObjectMapper json) { this.config = config; this.json = json; }
    private Path root() throws Exception { return Files.createDirectories(Path.of(config.dataDir).toAbsolutePath()); }
    private Path path(String id, String suffix) throws Exception {
        if (!id.matches("[a-zA-Z0-9-]+")) throw new IllegalArgumentException("Invalid identifier");
        return root().resolve(id + suffix);
    }
    public CaseRecord get(String id) throws Exception {
        if (config.demo()) {
            Path p = path(id,".json");
            return Files.exists(p) ? json.readValue(p.toFile(),CaseRecord.class) : null;
        }
        var doc = FirestoreClient.getFirestore().collection("cases").document(id).get().get();
        return doc.exists() ? json.readValue(doc.getString("payload"),CaseRecord.class) : null;
    }
    public void save(CaseRecord c) throws Exception {
        c.updatedAt = java.time.Instant.now().toString();
        String payload = json.writeValueAsString(c);
        if (payload.length() > 600000) throw new IllegalArgumentException("Demo case history limit reached; export this case before continuing.");
        if (config.demo()) {
            Path p = path(c.id,".json"), tmp = path(c.id,".tmp");
            Files.writeString(tmp,payload);
            Files.move(tmp,p,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        } else FirestoreClient.getFirestore().collection("cases").document(c.id)
            .set(Map.of("owner_uid",c.ownerUid,"payload",payload)).get();
    }
    public List<CaseRecord> list(String owner) throws Exception {
        List<CaseRecord> cases = new ArrayList<>();
        if (config.demo()) {
            try (var files = Files.list(root())) {
                for (Path p : files.filter(p -> p.toString().endsWith(".json") && !p.getFileName().toString().startsWith("profile-")).toList()) {
                    CaseRecord c = json.readValue(p.toFile(),CaseRecord.class);
                    if (owner.equals(c.ownerUid)) cases.add(c);
                }
            }
        } else {
            for (var doc : FirestoreClient.getFirestore().collection("cases").whereEqualTo("owner_uid",owner).get().get().getDocuments())
                cases.add(json.readValue(doc.getString("payload"),CaseRecord.class));
        }
        cases.sort(Comparator.comparing((CaseRecord c) -> c.createdAt).reversed());
        return cases;
    }
    public void writeFile(CaseRecord c, byte[] bytes) throws Exception {
        if (config.demo()) Files.write(path(c.id,".upload"),bytes);
        else StorageOptions.getDefaultInstance().getService().create(
            BlobInfo.newBuilder(config.bucket,c.ownerUid+"/"+c.id).setContentType(c.mime).build(),bytes);
    }
    public byte[] readFile(CaseRecord c) throws Exception {
        if (config.demo()) return Files.readAllBytes(path(c.id,".upload"));
        return StorageOptions.getDefaultInstance().getService().readAllBytes(config.bucket,c.ownerUid+"/"+c.id);
    }
    public void delete(CaseRecord c) throws Exception {
        if (config.demo()) { Files.deleteIfExists(path(c.id,".json")); Files.deleteIfExists(path(c.id,".upload")); }
        else {
            StorageOptions.getDefaultInstance().getService().delete(config.bucket,c.ownerUid+"/"+c.id);
            FirestoreClient.getFirestore().collection("cases").document(c.id).delete().get();
        }
    }
    public Supplier profile(String owner) throws Exception {
        if (config.demo()) { Path p=path("profile-"+owner,".json"); return Files.exists(p)?json.readValue(p.toFile(),Supplier.class):null; }
        var d=FirestoreClient.getFirestore().collection("users").document(owner).get().get();
        return d.exists()?json.readValue(d.getString("payload"),Supplier.class):null;
    }
    public void profile(String owner, Supplier s) throws Exception {
        if(config.demo()) Files.writeString(path("profile-"+owner,".json"),json.writeValueAsString(s));
        else FirestoreClient.getFirestore().collection("users").document(owner).set(Map.of("payload",json.writeValueAsString(s))).get();
    }
}
