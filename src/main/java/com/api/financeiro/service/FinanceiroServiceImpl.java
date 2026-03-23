package com.api.financeiro.service;

import com.api.financeiro.dto.response.DashboardFinanceiroResponse;
import com.api.financeiro.dto.response.ProfissionalGanhosResponse;
import com.api.financeiro.dto.response.ProjetoFinanceiroResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FinanceiroServiceImpl implements FinanceiroService {

    @Override
    public List<ProjetoFinanceiroResponse> listarProjetosFinanceiro() {
        return List.of();
    }

    @Override
    public ProfissionalGanhosResponse detalharGanhosProfissional(Integer usuarioId, BigDecimal bonus) {
        throw new UnsupportedOperationException("Funcionalidade ainda não implementada");
    }

    @Override
    public List<ProfissionalGanhosResponse> listarTodosProfissionais() {
        return List.of();
    }

    @Override
    public DashboardFinanceiroResponse obterDadosDashboard() {
        throw new UnsupportedOperationException("Funcionalidade ainda não implementada");
    }
}