package in.chaser.eligibility;
import in.chaser.api.Models.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EligibilityTest {
    @Test void acceptsOnlyRegisteredMicroOrSmall() {
        assertNull(Eligibility.reason(new Supplier("Synthetic supplier","UDYAM-ZZ-00-0000000","small")));
        assertNull(Eligibility.reason(new Supplier("Synthetic supplier","UDYAM-ZZ-00-0000000","micro")));
        assertNotNull(Eligibility.reason(new Supplier("Synthetic supplier","UDYAM-ZZ-00-0000000","medium")));
        assertNotNull(Eligibility.reason(new Supplier("Synthetic supplier","","small")));
    }
}
