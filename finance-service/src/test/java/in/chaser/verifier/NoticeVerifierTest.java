package in.chaser.verifier;
import in.chaser.api.Models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
class NoticeVerifierTest {
    Numbers n=new Numbers("50000.00","690.41","50690.41",28,LocalDate.parse("2026-09-15"));
    Draft good=NoticeVerifier.fallback(new DraftRequest("formal","formal","Synthetic supplier","Synthetic buyer",n));
    @Test void acceptsCodeTemplate() { assertTrue(NoticeVerifier.verify(good,n)); }
    @Test void blocksWrongInterestEvenWithCorrectCitations() { assertFalse(NoticeVerifier.verify(new Draft(good.subject(),good.body().replace("690.41","700.00"),n),n)); }
    @Test void blocksSwappedAmounts() { assertFalse(NoticeVerifier.verify(new Draft(good.subject(),good.body().replace("Principal: INR 50000.00","Principal: INR 50690.41"),n),n)); }
    @ParameterizedTest @ValueSource(strings={"Pay INR 999.00", "Pay 999 rupees", "Pay seven hundred rupees", "Pay on 12 November", "Pay tomorrow", "Rate 18%", "₹７００", "2026-11-20"})
    void blocksExtraClaims(String claim) { assertFalse(NoticeVerifier.verify(new Draft(good.subject(),good.body()+"\n"+claim,n),n)); }
    @Test void verifiesSubjectToo() { assertFalse(NoticeVerifier.verify(new Draft("Pay INR 700",good.body(),n),n)); }
    @Test void requiresDisclaimer() { assertFalse(NoticeVerifier.verify(new Draft(good.subject(),good.body().replace(NoticeVerifier.DISCLAIMER,""),n),n)); }
    @Test void requiresCitations() { assertFalse(NoticeVerifier.verify(new Draft(good.subject(),good.body(),null),n)); }
}
