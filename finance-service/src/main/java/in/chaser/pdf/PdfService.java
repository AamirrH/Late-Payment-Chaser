package in.chaser.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import in.chaser.api.Models.*;
import in.chaser.verifier.NoticeVerifier;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;

@Component
public class PdfService {
    public String noticeText(CaseRecord c,Notice n) {
        return "LATE PAYMENT CHASER\n"+(c.synthetic?"SYNTHETIC DEMO DATA\n":"")+(n.simulated()?"SIMULATED DATE\n":"")
            +"Supplier: "+c.invoice.supplier().businessName()+"\nBuyer: "+c.invoice.buyer()+"\nInvoice: "+c.invoice.invoiceNo()
            +"\nAs of: "+n.asOf()+"\nBank rate used: "+c.lastCalc.bankRate()+" (dated "+c.lastCalc.bankRateDate()+")"
            +"\nDraft source: "+n.source()+"\n\n"+n.subject()+"\n\n"+n.body()+"\n\nDraft only. Nothing has been sent.";
    }
    public String filingText(CaseRecord c) {
        StringBuilder text=new StringBuilder("FILING PREPARATION PACK\n");
        if(c.synthetic) text.append("SYNTHETIC DEMO DATA\n");
        if(c.simulated) text.append("SIMULATED DATE\n");
        text.append("Supplier: ").append(c.invoice.supplier().businessName()).append("\nUdyam (format checked only): ").append(c.invoice.supplier().udyamNumber())
            .append("\nBuyer: ").append(c.invoice.buyer()).append("\nInvoice: ").append(c.invoice.invoiceNo())
            .append("\nAccepted: ").append(c.invoice.acceptedDate()).append("\nAs of: ").append(c.lastCalc.asOf())
            .append("\n\n").append(NoticeVerifier.block(Numbers.from(c.lastCalc)))
            .append("\n\nBank rate: ").append(c.lastCalc.bankRate()).append(" (dated ").append(c.lastCalc.bankRateDate()).append(")")
            .append("\nConvention: calendar months from due date; remaining days / 365; final half-up rounding.")
            .append("\n\nAttach the original invoice, acceptance evidence, written payment agreement and actual correspondence. This pack does not file a complaint or establish legal eligibility.")
            .append("\n\nACTIVITY (drafts are not evidence of delivery)\n");
        c.events.forEach(e->text.append(e.ts()).append(" | ").append(e.detail()).append("\n"));
        c.notices.forEach(n->text.append("\n--- REVIEWED DRAFT ---\n").append(noticeText(c,n)).append("\n"));
        return text.append("\n").append(NoticeVerifier.DISCLAIMER).toString();
    }
    public byte[] render(String text) {
        var output=new ByteArrayOutputStream(); var doc=new Document(PageSize.A4,48,48,48,48);
        PdfWriter.getInstance(doc,output); doc.open();
        // Use INR in exports for portable built-in font support.
        Font font=FontFactory.getFont(FontFactory.HELVETICA,10);
        for(String line:text.split("\n",-1)) { Paragraph p=new Paragraph(line.isEmpty()?" ":line.replace("₹","INR ").replace("→"," -> "),font); p.setLeading(15); doc.add(p); }
        doc.close(); return output.toByteArray();
    }
}
