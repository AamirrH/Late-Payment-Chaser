package in.chaser.verifier;

import in.chaser.api.Models.*;
import java.util.regex.Pattern;

/** A deliberately conservative grammar: code fixes every financial statement. */
public final class NoticeVerifier {
    public static final String DISCLAIMER = "Statutory-interest estimate. Review before sending. This is not legal advice.";
    public static final String LEGAL = "Review applicability of MSMED Act Sections 15 and 16 and Income-tax Section 43B(h) with your adviser.";
    private static final Pattern UNSUPPORTED = Pattern.compile("[\\p{N}₹$€£%]|\\b(?:INR|Rs|rupees|zero|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety|hundred|thousand|lakh|crore|million|billion|January|February|March|April|May|June|July|August|September|October|November|December|tomorrow|yesterday|today)\\b", Pattern.CASE_INSENSITIVE);
    private NoticeVerifier() {}
    public static String block(Numbers n) {
        return "Principal: INR " + n.principal() + "\nEstimated interest: INR " + n.interest()
            + "\nTotal: INR " + n.total() + "\nDays overdue: " + n.daysOverdue() + "\nDue date: " + n.dueDate();
    }
    public static boolean verify(Draft d, Numbers n) {
        if (d == null || !n.equals(d.citedNumbers()) || d.subject() == null || d.body() == null
            || d.subject().isBlank() || d.subject().length() > 200 || d.body().length() > 12000) return false;
        String fixed = block(n);
        if (!d.body().contains(fixed) || !d.body().contains(DISCLAIMER)) return false;
        // Remove only one mandatory block. Duplicate numbers elsewhere are not accepted.
        String prose = d.body().replaceFirst(Pattern.quote(fixed), "").replace(LEGAL, "");
        return !UNSUPPORTED.matcher(d.subject() + "\n" + prose).find();
    }
    public static Draft fallback(DraftRequest r) {
        String opening = switch(r.tone()) {
            case "friendly" -> "A gentle reminder that payment remains outstanding. Please let us know when we can expect settlement.";
            case "firm" -> "Payment remains outstanding. Please arrange settlement and confirm your payment plan in writing.";
            default -> "We request settlement of the outstanding invoice. Please respond in writing regarding payment.";
        };
        String body = "Dear customer,\n\n" + opening + "\n\n" + block(r.numbers()) + "\n\n"
            + (r.stage().equals("reminded") ? "" : LEGAL + "\n\n")
            + "Please review these details against your records.\n\n" + DISCLAIMER;
        return new Draft("Payment follow-up", body, r.numbers());
    }
}
