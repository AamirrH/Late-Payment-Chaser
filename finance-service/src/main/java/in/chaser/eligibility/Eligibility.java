package in.chaser.eligibility;

import in.chaser.api.Models.Supplier;

public final class Eligibility {
    private Eligibility() {}
    public static String reason(Supplier supplier) {
        if (!supplier.category().equals("micro") && !supplier.category().equals("small"))
            return "This demo route covers micro and small enterprises only. Review your registration category before proceeding.";
        if (!supplier.udyamNumber().matches("UDYAM-[A-Z]{2}-[0-9]{2}-[0-9]{7}"))
            return "Enter a Udyam number in UDYAM-XX-00-0000000 format, or register first. This is a format check, not registry verification.";
        return null;
    }
}
