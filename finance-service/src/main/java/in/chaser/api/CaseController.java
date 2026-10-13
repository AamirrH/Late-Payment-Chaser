package in.chaser.api;

import in.chaser.Config;
import in.chaser.api.Models.*;
import in.chaser.pdf.PdfService;
import in.chaser.state.CaseService;
import in.chaser.store.CaseStore;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
public class CaseController {
    private final CaseService service; private final Auth auth; private final CaseStore store; private final PdfService pdf; private final Config config;
    public CaseController(CaseService service,Auth auth,CaseStore store,PdfService pdf,Config config) { this.service=service; this.auth=auth; this.store=store; this.pdf=pdf; this.config=config; }
    @GetMapping("/health") public Map<String,String> health() { return Map.of("status","ok","mode",config.mode); }
    @PostMapping("/demo/session") public Map<String,String> session() { return auth.session(); }
    @GetMapping("/profile") public Object profile(@RequestHeader(value="Authorization",required=false) String token) throws Exception { var p=store.profile(auth.owner(token)); return p==null?Map.of():p; }
    @PutMapping("/profile") public Supplier profile(@RequestHeader(value="Authorization",required=false) String token,@Valid @RequestBody Supplier s) throws Exception { store.profile(auth.owner(token),s); return s; }
    @GetMapping("/cases") public Object list(@RequestHeader(value="Authorization",required=false) String token) throws Exception { return service.list(auth.owner(token)); }
    @PostMapping("/cases") public CaseRecord upload(@RequestHeader(value="Authorization",required=false) String token,@RequestParam MultipartFile file) throws Exception { return service.upload(auth.owner(token),file); }
    @PostMapping("/cases/demo/{kind}") public CaseRecord demo(@RequestHeader(value="Authorization",required=false) String token,@PathVariable String kind) throws Exception { return service.demo(auth.owner(token),kind); }
    @GetMapping("/cases/{id}") public CaseRecord get(@RequestHeader(value="Authorization",required=false) String token,@PathVariable String id) throws Exception { return service.detail(auth.owner(token),id); }
    @PostMapping("/cases/{id}/extract") public CaseRecord extract(@RequestHeader(value="Authorization",required=false) String token,@PathVariable String id) throws Exception { return service.extract(auth.owner(token),id); }
    @PatchMapping("/cases/{id}/confirm") public CaseRecord confirm(@RequestHeader(value="Authorization",required=false) String token,@PathVariable String id,@Valid @RequestBody Confirm data) throws Exception { return service.confirm(auth.owner(token),id,data); }
    @PostMapping("/cases/{id}/draft") public CaseRecord draft(@RequestHeader(value="Authorization",required=false) String token,@PathVariable String id,@Valid @RequestBody Tone data) throws Exception { return service.draft(auth.owner(token),id,data.tone()); }
    @PostMapping("/cases/{id}/simulate") public CaseRecord simulate(@RequestHeader(value="Authorization",required=false) String token,@PathVariable String id,@Valid @RequestBody Simulation data) throws Exception { return service.simulate(auth.owner(token),id,data.days()); }
    @PostMapping("/cases/{id}/paid") public CaseRecord paid(@RequestHeader(value="Authorization",required=false) String token,@PathVariable String id) throws Exception { return service.paid(auth.owner(token),id); }
    @DeleteMapping("/cases/{id}") public Map<String,Boolean> delete(@RequestHeader(value="Authorization",required=false) String token,@PathVariable String id) throws Exception { service.delete(auth.owner(token),id); return Map.of("deleted",true); }
    @GetMapping("/cases/{id}/notices/{noticeId}.{format}") public ResponseEntity<byte[]> export(@RequestHeader(value="Authorization",required=false) String token,@PathVariable String id,@PathVariable String noticeId,@PathVariable String format) throws Exception {
        var c=service.get(auth.owner(token),id); var n=c.notices.stream().filter(x->x.id().equals(noticeId)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        String text=pdf.noticeText(c,n);
        if(format.equals("pdf")) return attachment(pdf.render(text),"application/pdf","notice.pdf");
        if(format.equals("txt")) return attachment(text.getBytes(java.nio.charset.StandardCharsets.UTF_8),"text/plain;charset=UTF-8","notice.txt");
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    @PostMapping("/cases/{id}/filing-pack") public ResponseEntity<byte[]> filing(@RequestHeader(value="Authorization",required=false) String token,@PathVariable String id) throws Exception {
        var c=service.detail(auth.owner(token),id);
        if(!c.confirmed || !c.eligible || !c.stage.equals("filing_ready") || c.status.equals("paid")) throw new IllegalArgumentException("Filing preparation unlocks after sixty days overdue on an open, eligible case.");
        return attachment(pdf.render(pdf.filingText(c)),"application/pdf","filing-pack.pdf");
    }
    private ResponseEntity<byte[]> attachment(byte[] b,String type,String filename) { return ResponseEntity.ok().header("Content-Disposition","attachment; filename=\""+filename+"\"").header("Cache-Control","no-store").contentType(MediaType.parseMediaType(type)).body(b); }
}
