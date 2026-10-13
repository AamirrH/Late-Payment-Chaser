package in.chaser.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class FinanceEngineTest {
    private FinanceEngine.Calculation calc(String p,String due,String today) { return FinanceEngine.calculate(new BigDecimal(p),LocalDate.parse(due),LocalDate.parse(today),new BigDecimal("0.06"),LocalDate.parse("2026-10-13")); }
    @ParameterizedTest @CsvSource({"50000,2026-10-13,690.41","50000,2026-11-12,1450.77","50000,2026-12-12,2197.13","200000,2026-10-13,2761.64"})
    void goldenCases(String p,String today,String interest) { assertEquals(interest,calc(p,"2026-09-15",today).interest()); }
    @ParameterizedTest @CsvSource({"2026-09-15","2026-09-14"})
    void noInterestOrNoticeUntilOverdue(String today) { var result=calc("50000","2026-09-15",today); assertEquals("0.00",result.interest()); assertEquals("invoice_received",FinanceEngine.stage(result)); }
    @Test void calendarMonthAnchorsDoNotDrift() { var c=calc("50000","2026-01-31","2026-03-30"); assertEquals(1,c.fullMonths()); assertEquals(30,c.remainingDays()); }
    @Test void leapDay() { var c=calc("50000","2024-02-29","2024-03-29"); assertEquals("750.00",c.interest()); assertEquals(1,c.fullMonths()); }
    @Test void zeroPrincipalDoesNotEscalate() { var c=calc("0","2026-09-15","2026-12-12"); assertEquals("0.00",c.interest()); assertEquals("invoice_received",FinanceEngine.stage(c)); }
    @Test void capsAgreedTermsAtFortyFive() { assertEquals(LocalDate.parse("2026-09-15"),FinanceEngine.dueDate(LocalDate.parse("2026-08-01"),60,0)); }
    @Test void missingAcceptanceBlocked() { assertThrows(IllegalArgumentException.class,()->FinanceEngine.dueDate(null,30,0)); }
    @Test void missingTermsRequiresReviewedDefault() { assertThrows(IllegalArgumentException.class,()->FinanceEngine.dueDate(LocalDate.parse("2026-08-01"),null,0)); assertEquals(LocalDate.parse("2026-08-16"),FinanceEngine.dueDate(LocalDate.parse("2026-08-01"),null,15)); }
    @ParameterizedTest @CsvSource({"7,reminded","8,formal","30,formal","31,notice","60,notice","61,filing_ready"})
    void stageBoundaries(long days,String stage) { assertEquals(stage,FinanceEngine.stageForDays(days)); }
}
