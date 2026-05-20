package com.api.financeiro.service;

import com.api.financeiro.dto.response.DashboardFinanceiroResponse;
import com.api.financeiro.dto.response.ProfissionalGanhosResponse;
import com.api.financeiro.dto.response.ProjetoDetalheResponse;
import com.api.financeiro.dto.response.ProjetoFinanceiroResponse;

import java.math.BigDecimal;
import java.util.List;

public interface FinanceiroService {

    List<ProjetoFinanceiroResponse> listarProjetosFinanceiro(String authorization);

    ProfissionalGanhosResponse detalharGanhosProfissional(Integer usuarioId, BigDecimal bonus, String authorization);

    List<ProfissionalGanhosResponse> listarTodosProfissionais(String authorization);

    DashboardFinanceiroResponse obterDadosDashboard(String authorization);

    ProjetoDetalheResponse detalharProjeto(Integer projetoId, String authorization);
}
