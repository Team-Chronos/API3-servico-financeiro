package com.api.financeiro.controller;

import com.api.financeiro.dto.response.ProfissionalGanhosResponse;
import com.api.financeiro.dto.response.ProjetoFinanceiroResponse;
import com.api.financeiro.service.FinanceiroService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Financeiro API OK");
    }

    @GetMapping("/projetos")
    public ResponseEntity<List<ProjetoFinanceiroResponse>> listarProjetosFinanceiro() {
        return ResponseEntity.ok(financeiroService.listarProjetosFinanceiro());
    }

    @GetMapping("/profissionais/{usuarioId}")
    public ResponseEntity<ProfissionalGanhosResponse> detalharGanhosProfissional(
            @PathVariable Integer usuarioId,
            @RequestParam(required = false, defaultValue = "0") BigDecimal bonus
    ) {
        return ResponseEntity.ok(financeiroService.detalharGanhosProfissional(usuarioId, bonus));
    }
}