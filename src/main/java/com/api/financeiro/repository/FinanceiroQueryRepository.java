package com.api.financeiro.repository;

import com.api.financeiro.dto.query.ProjetoFinanceiroQueryDto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class FinanceiroQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final RowMapper<ProjetoFinanceiroQueryDto> projetoFinanceiroRowMapper = (rs, rowNum) ->
            new ProjetoFinanceiroQueryDto(
                    rs.getInt("projeto_id"),
                    rs.getString("nome_projeto"),
                    rs.getString("tipo_projeto"),
                    getBigDecimal(rs, "total_horas"),
                    getBigDecimal(rs, "custo_total")
            );

    public FinanceiroQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProjetoFinanceiroQueryDto> listarProjetosFinanceiro() {
        String sql = """
                SELECT
                    p.id AS projeto_id,
                    p.nome AS nome_projeto,
                    p.tipo_projeto AS tipo_projeto,
                    CAST(
                        COALESCE(SUM(
                            CASE
                                WHEN ta.data_inicio IS NOT NULL AND ta.data_fim IS NOT NULL
                                THEN TIMESTAMPDIFF(SECOND, ta.data_inicio, ta.data_fim)
                                ELSE 0
                            END
                        ) / 3600.0, 0) AS DECIMAL(10,2)
                    ) AS total_horas,
                    CAST(
                        COALESCE(SUM(
                            CASE
                                WHEN ta.data_inicio IS NOT NULL AND ta.data_fim IS NOT NULL
                                THEN (TIMESTAMPDIFF(SECOND, ta.data_inicio, ta.data_fim) / 3600.0) * COALESCE(p.valor_hora_base, 0)
                                ELSE 0
                            END
                        ), 0) AS DECIMAL(12,2)
                    ) AS custo_total
                FROM projeto p
                INNER JOIN tarefa t ON t.projeto_id = p.id
                LEFT JOIN tarefa_atividade ta ON ta.tarefa_id = t.id
                GROUP BY p.id, p.nome, p.tipo_projeto
                ORDER BY p.nome
                """;

        return jdbcTemplate.query(sql, projetoFinanceiroRowMapper);
    }

    private static BigDecimal getBigDecimal(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? BigDecimal.ZERO : value;
    }
}