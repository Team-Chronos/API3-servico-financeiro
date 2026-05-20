package com.api.financeiro.client;

import com.api.financeiro.client.dto.DadosFinanceirosExternos;
import com.api.financeiro.client.dto.ProfissionalExternoDto;
import com.api.financeiro.client.dto.ProjetoExternoDto;
import com.api.financeiro.client.dto.RegistroHoraExternoDto;
import com.api.financeiro.client.dto.TarefaExternaDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class GatewayClient {

    private static final ParameterizedTypeReference<List<ProjetoExternoDto>> LISTA_PROJETOS = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<List<TarefaExternaDto>> LISTA_TAREFAS = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<List<RegistroHoraExternoDto>> LISTA_REGISTROS = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<List<ProfissionalExternoDto>> LISTA_PROFISSIONAIS = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;

    public GatewayClient(@Value("${gateway.base-url:http://auth-gateway:8080}") String gatewayBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(gatewayBaseUrl)
                .build();
    }

    public DadosFinanceirosExternos buscarDadosFinanceiros(String authorization) {
        return new DadosFinanceirosExternos(
                buscarProjetos(authorization),
                buscarTarefas(authorization),
                buscarRegistros(authorization),
                buscarProfissionais(authorization)
        );
    }

    public List<ProjetoExternoDto> buscarProjetos(String authorization) {
        return getLista("/projeto/projetos", authorization, LISTA_PROJETOS);
    }

    public List<TarefaExternaDto> buscarTarefas(String authorization) {
        return getLista("/tarefas/tarefas", authorization, LISTA_TAREFAS);
    }

    public List<RegistroHoraExternoDto> buscarRegistros(String authorization) {
        return getLista("/apontamento/registros", authorization, LISTA_REGISTROS);
    }

    public List<ProfissionalExternoDto> buscarProfissionais(String authorization) {
        return getLista("/profissionais/api/profissionais", authorization, LISTA_PROFISSIONAIS);
    }

    private <T> List<T> getLista(String uri, String authorization, ParameterizedTypeReference<List<T>> typeReference) {
        try {
            List<T> resposta = restClient.get()
                    .uri(uri)
                    .headers(headers -> adicionarAuthorization(headers, authorization))
                    .retrieve()
                    .body(typeReference);

            return resposta == null ? List.of() : resposta;
        } catch (RestClientException ex) {
            throw new IllegalStateException("Erro ao consultar gateway em " + uri + ": " + ex.getMessage(), ex);
        }
    }

    private void adicionarAuthorization(HttpHeaders headers, String authorization) {
        if (StringUtils.hasText(authorization)) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
    }
}
