package com.api.financeiro.service;

import com.api.financeiro.dto.response.DashboardFinanceiroResponse;
import com.api.financeiro.dto.response.ProfissionalGanhosResponse;
import com.api.financeiro.dto.response.ProjetoFinanceiroResponse;

import java.math.BigDecimal;
import java.util.List;

public interface FinanceiroService {

    List<ProjetoFinanceiroResponse> listarProjetosFinanceiro();

    ProfissionalGanhosResponse detalharGanhosProfissional(Integer usuarioId, BigDecimal bonus);

    List<ProfissionalGanhosResponse> listarTodosProfissionais();

    DashboardFinanceiroResponse obterDadosDashboard();
}