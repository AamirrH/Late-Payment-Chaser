package in.chaser.engine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class FinanceEngine {
    private static final MathContext MC = MathContext.DECIMAL128;
    private FinanceEngine() {}
    public record Calculation(String principal, String interest, String total, long daysOverdue,
                              LocalDate dueDate, LocalDate asOf, String bankRate, String annualRate,
                              LocalDate bankRateDate, int fullMonths, long remainingDays, String formula) {}
    public static LocalDate dueDate(LocalDate accepted, Integer agreedDays, int defaultDays) {
        if (accepted == null) throw new IllegalArgumentException("Confirm the acceptance date before calculating.");
        if (agreedDays == null && defaultDays <= 0)
            throw new IllegalArgumentException("No agreed credit period: a reviewed default credit convention must be configured first.");
        int days = agreedDays == null ? defaultDays : agreedDays;
        if (days < 0 || days > 365) throw new IllegalArgumentException("Credit days must be between 0 and 365.");
        return accepted.plusDays(Math.min(days, 45));
    }
    public static Calculation calculate(BigDecimal principal, LocalDate due, LocalDate today,
                                        BigDecimal bankRate, LocalDate rateDate) {
        if (principal.signum() < 0 || bankRate.signum() < 0 || bankRate.compareTo(BigDecimal.ONE) > 0)
            throw new IllegalArgumentException("Invalid principal or bank rate.");
        var annual = bankRate.multiply(new BigDecimal("3"));
        long overdue = Math.max(0, ChronoUnit.DAYS.between(due, today));
        int months = 0;
        while (due.plusMonths(months + 1L).compareTo(today) <= 0) months++;
        long remaining = overdue == 0 ? 0 : ChronoUnit.DAYS.between(due.plusMonths(months), today);
        var amount = principal.multiply(BigDecimal.ONE.add(annual.divide(new BigDecimal("12"), MC)).pow(months, MC), MC);
        amount = amount.add(amount.multiply(annual, MC).multiply(BigDecimal.valueOf(remaining), MC).divide(new BigDecimal("365"), MC), MC);
        var interest = amount.subtract(principal).setScale(2, RoundingMode.HALF_UP);
        return new Calculation(money(principal), money(interest), money(principal.add(interest)), overdue,
            due, today, bankRate.toPlainString(), annual.toPlainString(), rateDate, months, remaining,
            "P × (1 + annual rate / 12)^full months × (1 + annual rate × remaining days / 365) − P");
    }
    public static String stage(Calculation calc) {
        if (calc.daysOverdue() == 0 || new BigDecimal(calc.principal()).signum() == 0) return "invoice_received";
        return stageForDays(calc.daysOverdue());
    }
    public static String stageForDays(long days) {
        if (days <= 7) return "reminded";
        if (days <= 30) return "formal";
        if (days <= 60) return "notice";
        return "filing_ready";
    }
    public static String money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
}
