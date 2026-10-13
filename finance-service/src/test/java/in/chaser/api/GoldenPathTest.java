package in.chaser.api;

import in.chaser.ai.AiClient;
import in.chaser.api.Models.*;
import in.chaser.verifier.NoticeVerifier;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

@SpringBootTest(properties={"chaser.mode=demo","chaser.data-dir=target/test-data","chaser.demo-date=2026-10-13"})
@AutoConfigureMockMvc
class GoldenPathTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @MockitoBean AiClient ai;
    String login() throws Exception { return "Bearer "+json.readTree(mvc.perform(post("/demo/session")).andReturn().getResponse().getContentAsString()).get("token").asText(); }
    String fixture(String token,String kind) throws Exception { return json.readTree(mvc.perform(post("/cases/demo/"+kind).header("Authorization",token)).andReturn().getResponse().getContentAsString()).get("id").asText(); }
    Map<String,Object> invoice(boolean within,boolean registered) {
        return Map.of("invoiceNo","DEMO-042","amount","50000.00","buyer","Synthetic Acme","invoiceDate",within?"2026-10-01":"2026-08-01","acceptedDate",within?"2026-10-01":"2026-08-01","agreedCreditDays",within?30:60,"supplier",Map.of("businessName","Synthetic Studio","udyamNumber",registered?"UDYAM-ZZ-00-0000000":"","category","small"));
    }
    @Test void completeGoldenPathAndTerminalPaid() throws Exception {
        when(ai.draft(any())).thenAnswer(call->NoticeVerifier.fallback(call.getArgument(0)));
        String token=login(),id=fixture(token,"overdue");
        mvc.perform(patch("/cases/"+id+"/confirm").header("Authorization",token).contentType("application/json").content(json.writeValueAsString(invoice(false,true))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.lastCalc.interest").value("690.41")).andExpect(jsonPath("$.stage").value("formal"));
        String noticeId=json.readTree(mvc.perform(post("/cases/"+id+"/draft").header("Authorization",token).contentType("application/json").content("{\"tone\":\"formal\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.notices[0].verified").value(true)).andReturn().getResponse().getContentAsString()).get("notices").get(0).get("id").asText();
        mvc.perform(get("/cases/"+id+"/notices/"+noticeId+".txt").header("Authorization",token))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Bank rate used: 0.06 (dated 2026-10-13)")));
        mvc.perform(post("/cases/"+id+"/simulate").header("Authorization",token).contentType("application/json").content("{\"days\":30}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.lastCalc.interest").value("1450.77")).andExpect(jsonPath("$.stage").value("notice")).andExpect(jsonPath("$.simulated").value(true));
        mvc.perform(post("/cases/"+id+"/simulate").header("Authorization",token).contentType("application/json").content("{\"days\":30}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.lastCalc.interest").value("2197.13")).andExpect(jsonPath("$.stage").value("filing_ready"));
        byte[] pdf=mvc.perform(post("/cases/"+id+"/filing-pack").header("Authorization",token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertEquals("%PDF",new String(Arrays.copyOfRange(pdf,0,4)));
        mvc.perform(post("/cases/"+id+"/paid").header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("paid"));
        mvc.perform(post("/cases/"+id+"/simulate").header("Authorization",token).contentType("application/json").content("{\"days\":30}")).andExpect(status().isBadRequest());
        mvc.perform(get("/cases").header("Authorization",token)).andExpect(jsonPath("$.totalPrincipal").value("0.00"));
    }
    @Test void ownershipAndMissingAuthEnforced() throws Exception {
        String token=login(),id=fixture(token,"overdue");
        mvc.perform(get("/cases/"+id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/cases/"+id).header("Authorization",login())).andExpect(status().isNotFound());
        mvc.perform(delete("/cases/"+id).header("Authorization",login())).andExpect(status().isNotFound());
    }
    @Test void withinTermsDoesNothing() throws Exception {
        String token=login(),id=fixture(token,"within-terms");
        mvc.perform(patch("/cases/"+id+"/confirm").header("Authorization",token).contentType("application/json").content(json.writeValueAsString(invoice(true,true))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.lastCalc.interest").value("0.00")).andExpect(jsonPath("$.stage").value("invoice_received"));
        mvc.perform(post("/cases/"+id+"/draft").header("Authorization",token).contentType("application/json").content("{\"tone\":\"formal\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(ai);
    }
    @Test void ineligibleStopsBeforeCalculation() throws Exception {
        String token=login(),id=fixture(token,"ineligible");
        mvc.perform(patch("/cases/"+id+"/confirm").header("Authorization",token).contentType("application/json").content(json.writeValueAsString(invoice(false,false))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.eligible").value(false)).andExpect(jsonPath("$.lastCalc").isEmpty());
    }
    @Test void wrongDraftRetriesTwiceThenUsesVerifiedTemplate() throws Exception {
        when(ai.draft(any())).thenAnswer(call->{ DraftRequest r=call.getArgument(0); return new Draft("Wrong draft","Pay INR 700.00",r.numbers()); });
        String token=login(),id=fixture(token,"overdue");
        mvc.perform(patch("/cases/"+id+"/confirm").header("Authorization",token).contentType("application/json").content(json.writeValueAsString(invoice(false,true)))).andExpect(status().isOk());
        mvc.perform(post("/cases/"+id+"/draft").header("Authorization",token).contentType("application/json").content("{\"tone\":\"friendly\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.notices[0].source").value("template")).andExpect(jsonPath("$.notices[0].attempts").value(3));
        verify(ai,times(3)).draft(any());
    }
}
