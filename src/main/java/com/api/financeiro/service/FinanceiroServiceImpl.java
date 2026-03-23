package com.api.financeiro.service;

import com.api.financeiro.dto.query.ProjetoFinanceiroQueryDto;
import com.api.financeiro.dto.response.DashboardFinanceiroResponse;
import com.api.financeiro.dto.response.ProfissionalGanhosResponse;
import com.api.financeiro.dto.response.ProjetoFinanceiroResponse;
import com.api.financeiro.repository.FinanceiroQueryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class FinanceiroServiceImpl implements FinanceiroService {

    private final FinanceiroQueryRepository financeiroQueryRepository;

    public FinanceiroServiceImpl(FinanceiroQueryRepository financeiroQueryRepository) {
        this.financeiroQueryRepository = financeiroQueryRepository;
    }

    @Override
    public List<ProjetoFinanceiroResponse> listarProjetosFinanceiro() {
        return financeiroQueryRepository.listarProjetosFinanceiro()
                .stream()
                .map(this::toProjetoFinanceiroResponse)
                .toList();
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

    private ProjetoFinanceiroResponse toProjetoFinanceiroResponse(ProjetoFinanceiroQueryDto dto) {
        return new ProjetoFinanceiroResponse(
                dto.projetoId(),
                dto.nomeProjeto(),
                dto.tipoProjeto(),
                dto.totalHoras().setScale(2, RoundingMode.HALF_UP),
                dto.custoTotal().setScale(2, RoundingMode.HALF_UP)
        );
    }
}