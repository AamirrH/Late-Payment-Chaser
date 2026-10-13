package in.chaser.state;

import in.chaser.Config;
import in.chaser.ai.AiClient;
import in.chaser.api.Models.*;
import in.chaser.eligibility.Eligibility;
import in.chaser.engine.FinanceEngine;
import in.chaser.store.CaseStore;
import in.chaser.verifier.NoticeVerifier;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class CaseService {
    private final CaseStore store;
    private final Config config;
    private final AiClient ai;
    public CaseService(CaseStore store, Config config, AiClient ai) { this.store=store; this.config=config; this.ai=ai; }
    public CaseRecord get(String owner,String id) throws Exception {
        if(!id.matches("[a-zA-Z0-9-]+")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        CaseRecord c=store.get(id);
        if(c==null || !owner.equals(c.ownerUid)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Case not found.");
        return c;
    }
    public synchronized Map<String,Object> list(String owner) throws Exception {
        var items=store.list(owner);
        BigDecimal principal=BigDecimal.ZERO, interest=BigDecimal.ZERO;
        for(var c:items) {
            if(c.confirmed && c.eligible && c.status.equals("open")) {
                recalculate(c);
                principal=principal.add(new BigDecimal(c.lastCalc.principal()));
                interest=interest.add(new BigDecimal(c.lastCalc.interest()));
            }
        }
        return Map.of("cases",items,"totalPrincipal",FinanceEngine.money(principal),"totalInterest",FinanceEngine.money(interest),"totalOwed",FinanceEngine.money(principal.add(interest)));
    }
    private CaseRecord create(String owner) {
        var c=new CaseRecord(); c.id=UUID.randomUUID().toString(); c.ownerUid=owner;
        c.createdAt=Instant.now().toString(); c.synthetic=config.demo(); c.baseDate=config.demo()?config.demoDate:null;
        c.simulated=config.demo(); event(c,"created","Invoice received. No calculations until you confirm the fields."); return c;
    }
    public synchronized CaseRecord upload(String owner,MultipartFile file) throws Exception {
        if(file.isEmpty() || file.getSize()>10*1024*1024) throw new IllegalArgumentException("Upload a nonempty file up to 10 MB.");
        byte[] bytes=file.getBytes(); String mime=detect(bytes);
        if(!mime.equals(file.getContentType())) throw new IllegalArgumentException("File contents do not match the declared file type.");
        var c=create(owner); c.filename="invoice."+(mime.equals("application/pdf")?"pdf":mime.equals("image/png")?"png":"jpg"); c.mime=mime;
        store.writeFile(c,bytes); store.save(c); return c;
    }
    public static String detect(byte[] b) {
        if(b.length>=5 && b[0]=='%' && b[1]=='P' && b[2]=='D' && b[3]=='F' && b[4]=='-') return "application/pdf";
        if(b.length>=8 && (b[0]&255)==137 && b[1]==80 && b[2]==78 && b[3]==71 && b[4]==13 && b[5]==10 && b[6]==26 && b[7]==10) return "image/png";
        if(b.length>=3 && (b[0]&255)==255 && (b[1]&255)==216 && (b[2]&255)==255) return "image/jpeg";
        throw new IllegalArgumentException("Only PDF, PNG and JPEG invoices are supported.");
    }
    public synchronized CaseRecord demo(String owner,String kind) throws Exception {
        if(!config.demo()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if(!Set.of("overdue","within-terms","ineligible").contains(kind)) throw new IllegalArgumentException("Unknown demo invoice");
        var c=create(owner); c.demoKind=kind; c.filename="synthetic-"+kind+".pdf";
        boolean within=kind.equals("within-terms");
        c.extracted=new Extracted(within?"DEMO-043":"DEMO-042","50000.00",within?"Sample Greenleaf Studio":"Sample Acme Retail",
            LocalDate.parse(within?"2026-10-01":"2026-08-01"),LocalDate.parse(within?"2026-10-01":"2026-08-01"),within?30:60,
            List.of("Synthetic fixture, not a live Gemini extraction. Confirm every field."));
        event(c,"extract","Loaded labelled synthetic extraction fixture."); store.save(c); return c;
    }
    public synchronized CaseRecord extract(String owner,String id) throws Exception {
        var c=get(owner,id); mutable(c);
        if(c.confirmed) throw new IllegalArgumentException("Confirmed invoices cannot be re-extracted. Create a new case for corrections.");
        if(c.demoKind!=null) return c;
        try { c.extracted=ai.extract(store.readFile(c),c.mime); event(c,"extract","AI extraction received. Fields require your review."); }
        catch(Exception e) { c.extracted=new Extracted(null,null,null,null,null,null,List.of("Extraction unavailable. Enter invoice fields manually; no values have been guessed.")); event(c,"extract","Extraction unavailable; manual confirmation required."); }
        store.save(c); return c;
    }
    public synchronized CaseRecord confirm(String owner,String id,Confirm invoice) throws Exception {
        var c=get(owner,id); mutable(c);
        if(c.confirmed) throw new IllegalArgumentException("This invoice is already confirmed. Create a new case to correct its details.");
        if(invoice.invoiceDate().isAfter(invoice.acceptedDate())) throw new IllegalArgumentException("Acceptance date cannot precede the invoice date in this demo.");
        LocalDate today=c.baseDate==null?LocalDate.now(ZoneId.of("Asia/Kolkata")):c.baseDate;
        if(invoice.acceptedDate().isAfter(today)) throw new IllegalArgumentException("Acceptance date cannot be in the future.");
        if(invoice.acceptedDate().isBefore(today.minusYears(20))) throw new IllegalArgumentException("Acceptance date must be within the past twenty years.");
        c.invoice=invoice; c.eligibilityReason=Eligibility.reason(invoice.supplier()); c.eligible=c.eligibilityReason==null;
        event(c,"eligibility",c.eligible?"Udyam format and enterprise category passed. Registry not queried.":c.eligibilityReason);
        if(c.eligible) { recalculate(c); c.confirmed=true; }
        store.save(c); return c;
    }
    private void mutable(CaseRecord c) { if(c.status.equals("paid")) throw new IllegalArgumentException("Paid is terminal; this case can no longer be changed."); }
    private void ready(CaseRecord c) { mutable(c); if(!c.confirmed || !c.eligible) throw new IllegalArgumentException("Confirm an eligible invoice first."); }
    private void recalculate(CaseRecord c) {
        LocalDate today=(c.baseDate==null?LocalDate.now(ZoneId.of("Asia/Kolkata")):c.baseDate).plusDays(c.simulatedOffsetDays);
        var due=FinanceEngine.dueDate(c.invoice.acceptedDate(),c.invoice.agreedCreditDays(),config.defaultCreditDays);
        c.lastCalc=FinanceEngine.calculate(new BigDecimal(c.invoice.amount()),due,today,config.bankRate,config.bankRateDate);
        String stage=FinanceEngine.stage(c.lastCalc);
        if(!stage.equals(c.stage)) { event(c,"state_change",c.stage+" → "+stage); c.stage=stage; c.state=stage; }
    }
    public synchronized CaseRecord detail(String owner,String id) throws Exception {
        var c=get(owner,id);
        if(c.confirmed && c.eligible && c.status.equals("open")) { recalculate(c); store.save(c); }
        return c;
    }
    public synchronized CaseRecord draft(String owner,String id,String tone) throws Exception {
        var c=get(owner,id); ready(c); recalculate(c);
        if(c.stage.equals("invoice_received")) throw new IllegalArgumentException("No overdue balance. No overdue notice is needed.");
        var request=new DraftRequest(c.stage,tone,c.invoice.supplier().businessName(),c.invoice.buyer(),Numbers.from(c.lastCalc));
        Draft result=null; int attempts=0; String source="gemini";
        for(int attempt=1;attempt<=3;attempt++) {
            attempts=attempt; event(c,"draft","Draft attempt "+attempt+" started.");
            try {
                Draft candidate=ai.draft(request);
                if(NoticeVerifier.verify(candidate,request.numbers())) { result=candidate; event(c,"verify","Passed: fixed financial statements and cited numbers match the engine."); break; }
                event(c,"verify","Blocked: draft did not match the permitted financial statements.");
            } catch(Exception e) { event(c,"draft","AI service unavailable. No unverified content saved."); break; }
        }
        if(result==null) {
            source="template"; result=NoticeVerifier.fallback(request);
            if(!NoticeVerifier.verify(result,request.numbers())) throw new IllegalStateException("Template verification failed");
            event(c,"verify","Verified deterministic template fallback.");
        }
        c.notices.add(new Notice(UUID.randomUUID().toString(),c.stage,tone,result.subject(),result.body(),request.numbers(),true,attempts,source,Instant.now().toString(),c.lastCalc.asOf(),c.simulated));
        store.save(c); return c;
    }
    public synchronized CaseRecord simulate(String owner,String id,int days) throws Exception {
        var c=get(owner,id); ready(c);
        if(c.simulatedOffsetDays+days>3650) throw new IllegalArgumentException("Simulation is limited to ten years.");
        c.simulatedOffsetDays+=days; c.simulated=true; recalculate(c);
        event(c,"simulation","Simulated +"+days+" days. As of "+c.lastCalc.asOf()+"."); store.save(c);
        if(!c.stage.equals("invoice_received")) return draft(owner,id,"formal");
        return c;
    }
    public synchronized CaseRecord paid(String owner,String id) throws Exception {
        var c=get(owner,id); ready(c); recalculate(c); c.status="paid"; c.state="paid";
        event(c,"state_change","Marked paid by the supplier. Escalation stopped; calculation frozen."); store.save(c); return c;
    }
    public synchronized void delete(String owner,String id) throws Exception { store.delete(get(owner,id)); }
    public static void event(CaseRecord c,String type,String detail) { c.events.add(new Event(UUID.randomUUID().toString(),type,detail,Instant.now().toString())); }
}
