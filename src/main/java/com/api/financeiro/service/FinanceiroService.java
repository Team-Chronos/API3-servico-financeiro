package com.api.financeiro.service;

import com.api.financeiro.dto.response.DashboardFinanceiroResponse;
import com.api.financeiro.dto.response.ProfissionalGanhosResponse;
import com.api.financeiro.dto.response.ProjetoDetalheResponse;
import com.api.financeiro.dto.response.ProjetoFinanceiroResponse;

import java.math.BigDecimal;
import java.util.List;

public interface FinanceiroService {

    List<ProjetoFinanceiroResponse> listarProjetosFinanceiro(String authorization, Integer ano, Integer mes);

    ProfissionalGanhosResponse detalharGanhosProfissional(Integer usuarioId, BigDecimal bonus, String authorization, Integer ano, Integer mes);

    List<ProfissionalGanhosResponse> listarTodosProfissionais(String authorization, Integer ano, Integer mes);

    DashboardFinanceiroResponse obterDadosDashboard(String authorization, Integer ano, Integer mes);

    ProjetoDetalheResponse detalharProjeto(Integer projetoId, String authorization, Integer ano, Integer mes);
}
