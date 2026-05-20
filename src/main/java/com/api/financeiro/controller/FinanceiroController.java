package com.api.financeiro.controller;

import com.api.financeiro.dto.response.DashboardFinanceiroResponse;
import com.api.financeiro.dto.response.ProfissionalGanhosResponse;
import com.api.financeiro.dto.response.ProjetoDetalheResponse;
import com.api.financeiro.dto.response.ProjetoFinanceiroResponse;
import com.api.financeiro.service.FinanceiroService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/financeiro")
public class FinanceiroController {

    private final FinanceiroService financeiroService;

    public FinanceiroController(FinanceiroService financeiroService) {
        this.financeiroService = financeiroService;
    }

    @GetMapping("/projetos")
    public ResponseEntity<List<ProjetoFinanceiroResponse>> listarProjetosFinanceiro(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return ResponseEntity.ok(financeiroService.listarProjetosFinanceiro(authorization));
    }

    @GetMapping("/projetos/{projetoId}/detalhes")
    public ResponseEntity<ProjetoDetalheResponse> detalharProjeto(
            @PathVariable Integer projetoId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return ResponseEntity.ok(financeiroService.detalharProjeto(projetoId, authorization));
    }

    @GetMapping("/profissionais")
    public ResponseEntity<List<ProfissionalGanhosResponse>> listarTodosProfissionais(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return ResponseEntity.ok(financeiroService.listarTodosProfissionais(authorization));
    }

    @GetMapping("/profissionais/{usuarioId}")
    public ResponseEntity<ProfissionalGanhosResponse> detalharGanhosProfissional(
            @PathVariable Integer usuarioId,
            @RequestParam(required = false, defaultValue = "0") BigDecimal bonus,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return ResponseEntity.ok(financeiroService.detalharGanhosProfissional(usuarioId, bonus, authorization));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardFinanceiroResponse> obterDadosDashboard(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return ResponseEntity.ok(financeiroService.obterDadosDashboard(authorization));
    }
}
