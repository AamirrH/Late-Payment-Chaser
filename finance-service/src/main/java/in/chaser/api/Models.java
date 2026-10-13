package in.chaser.api;

import in.chaser.engine.FinanceEngine.Calculation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;

public final class Models {
    public record Supplier(@NotBlank @Size(max=160) String businessName,
                           @NotNull @Size(max=30) String udyamNumber,
                           @NotNull @Pattern(regexp="micro|small|medium|none") String category) {}
    public record Confirm(@NotBlank @Size(max=80) String invoiceNo,
                          @NotNull @Pattern(regexp="[0-9]{1,10}\\.[0-9]{2}") String amount,
                          @NotBlank @Size(max=160) String buyer, @NotNull LocalDate invoiceDate,
                          @NotNull LocalDate acceptedDate, @Min(0) @Max(365) Integer agreedCreditDays,
                          @NotNull @Valid Supplier supplier) {}
    public record Extracted(String invoiceNo, String amount, String buyer, LocalDate invoiceDate,
                            LocalDate acceptedDate, Integer agreedCreditDays, List<String> fieldNotes) {}
    public record Numbers(String principal, String interest, String total, long daysOverdue, LocalDate dueDate) {
        public static Numbers from(Calculation c) { return new Numbers(c.principal(),c.interest(),c.total(),c.daysOverdue(),c.dueDate()); }
    }
    public record DraftRequest(String stage, String tone, String supplier, String buyer, Numbers numbers) {}
    public record Draft(String subject, String body, Numbers citedNumbers) {}
    public record Tone(@NotNull @Pattern(regexp="friendly|firm|formal") String tone) {}
    public record Simulation(@Min(1) @Max(365) int days) {}
    public record Event(String id, String type, String detail, String ts) {}
    public record Notice(String id, String stage, String tone, String subject, String body, Numbers numbers,
                         boolean verified, int attempts, String source, String createdAt, LocalDate asOf, boolean simulated) {}
    public static class CaseRecord {
        public String id, ownerUid, filename, mime, createdAt, updatedAt;
        public String state = "invoice_received", status = "open", stage = "invoice_received";
        public String eligibilityReason;
        public boolean confirmed, eligible, synthetic, simulated;
        public int simulatedOffsetDays;
        public String demoKind;
        public LocalDate baseDate;
        public Extracted extracted;
        public Confirm invoice;
        public Calculation lastCalc;
        public List<Event> events = new ArrayList<>();
        public List<Notice> notices = new ArrayList<>();
    }
}
