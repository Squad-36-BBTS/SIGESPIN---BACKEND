package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.ia.AnalisarDemandaRequest;
import br.com.bbts.sigespin.dto.ia.AnalisarDemandaResponse;
import br.com.bbts.sigespin.exception.IaIndisponivelException;
import br.com.bbts.sigespin.model.enums.ArenaTipo;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiIntegrationServiceTests {

    private static final String BASE = "http://gemini.test/v1beta";
    private static final AnalisarDemandaRequest REQ = new AnalisarDemandaRequest("Automatizar triagem", "Hoje é manual.");

    private static final String RESPOSTA = """
            {"candidates":[{"content":{"parts":[{"text":"{\\"arenaSugerida\\":\\"NEGOCIOS_DIGITAIS\\",\\"hipotese\\":\\"h\\",\\"trlInicial\\":1,\\"potencialLeiDoBem\\":false,\\"justificativaArena\\":\\"j\\",\\"scoresRadar\\":{\\"negocio\\":1,\\"produto\\":1,\\"time\\":1,\\"receita\\":1}}"}]}}]}
            """;

    private MockRestServiceServer servidor;

    private GeminiIntegrationService criar(String chave) {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        servidor = MockRestServiceServer.bindTo(builder).build();
        return new GeminiIntegrationService(builder.build(), chave, "modelo-x");
    }

    @Test
    void sucessoEnviaChaveNoHeaderENaoNaUrl() {
        GeminiIntegrationService s = criar("chave-fake");
        servidor.expect(requestTo(BASE + "/models/modelo-x:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "chave-fake"))
                .andRespond(withSuccess(RESPOSTA, MediaType.APPLICATION_JSON));
        AnalisarDemandaResponse r = s.analisar(REQ);
        assertEquals(ArenaTipo.NEGOCIOS_DIGITAIS, r.arenaSugerida());
        servidor.verify();
    }

    @Test
    void erroHttpViraIaIndisponivel() {
        GeminiIntegrationService s = criar("chave-fake");
        servidor.expect(requestTo(BASE + "/models/modelo-x:generateContent"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThrows(IaIndisponivelException.class, () -> s.analisar(REQ));
    }

    @Test
    void semChaveNaoChamaARede() {
        GeminiIntegrationService s = criar("");
        assertThrows(IaIndisponivelException.class, () -> s.analisar(REQ));
        servidor.verify();
    }
}
