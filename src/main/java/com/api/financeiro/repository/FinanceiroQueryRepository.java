package com.api.financeiro.repository;

import com.api.financeiro.dto.query.DashboardFinanceiroQueryDto;
import com.api.financeiro.dto.query.ProfissionalProjetoQueryDto;
import com.api.financeiro.dto.query.ProjetoFinanceiroQueryDto;
import com.api.financeiro.dto.query.UsuarioAtivoDto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
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

    private final RowMapper<ProfissionalProjetoQueryDto> profissionalProjetoRowMapper = (rs, rowNum) ->
            new ProfissionalProjetoQueryDto(
                    rs.getInt("usuario_id"),
                    rs.getString("usuario_nome"),
                    rs.getInt("projeto_id"),
                    rs.getString("nome_projeto"),
                    getBigDecimal(rs, "horas_trabalhadas"),
                    getBigDecimal(rs, "valor_hora_projeto")
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

    public List<ProfissionalProjetoQueryDto> listarProjetosDoProfissional(Integer usuarioId) {
        String sql = """
                SELECT
                    u.id AS usuario_id,
                    u.nome AS usuario_nome,
                    p.id AS projeto_id,
                    p.nome AS nome_projeto,
                    CAST(
                        COALESCE(SUM(
                            CASE
                                WHEN ta.data_inicio IS NOT NULL AND ta.data_fim IS NOT NULL
                                THEN TIMESTAMPDIFF(SECOND, ta.data_inicio, ta.data_fim)
                                ELSE 0
                            END
                        ) / 3600.0, 0) AS DECIMAL(10,2)
                    ) AS horas_trabalhadas,
                    CAST(COALESCE(p.valor_hora_base, 0) AS DECIMAL(10,2)) AS valor_hora_projeto
                FROM usuario u
                INNER JOIN tarefa t ON t.responsavel_id = u.id
                INNER JOIN projeto p ON p.id = t.projeto_id
                LEFT JOIN tarefa_atividade ta ON ta.tarefa_id = t.id
                WHERE u.id = :usuarioId
                  AND u.ativo = true
                GROUP BY u.id, u.nome, p.id, p.nome, p.valor_hora_base
                ORDER BY p.nome
                """;

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("usuarioId", usuarioId);

        return jdbcTemplate.query(sql, params, profissionalProjetoRowMapper);
    }

    public List<UsuarioAtivoDto> listarUsuariosAtivosComApontamento() {
        String sql = """
                SELECT DISTINCT
                    u.id AS usuario_id,
                    u.nome AS usuario_nome
                FROM usuario u
                INNER JOIN tarefa t ON t.responsavel_id = u.id
                WHERE u.ativo = true
                ORDER BY u.nome
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new UsuarioAtivoDto(
                        rs.getInt("usuario_id"),
                        rs.getString("usuario_nome")
                )
        );
    }

    public DashboardFinanceiroQueryDto obterDashboard() {
        String sql = """
                SELECT
                    CAST(COALESCE((
                        SELECT SUM(
                            CASE
                                WHEN ta.data_inicio IS NOT NULL AND ta.data_fim IS NOT NULL
                                THEN TIMESTAMPDIFF(SECOND, ta.data_inicio, ta.data_fim)
                                ELSE 0
                            END
                        ) / 3600.0
                        FROM tarefa_atividade ta
                    ), 0) AS DECIMAL(12,2)) AS total_horas,

                    CAST(COALESCE((
                        SELECT SUM(
                            CASE
                                WHEN ta.data_inicio IS NOT NULL AND ta.data_fim IS NOT NULL
                                THEN (TIMESTAMPDIFF(SECOND, ta.data_inicio, ta.data_fim) / 3600.0) * COALESCE(p.valor_hora_base, 0)
                                ELSE 0
                            END
                        )
                        FROM tarefa t
                        INNER JOIN projeto p ON p.id = t.projeto_id
                        LEFT JOIN tarefa_atividade ta ON ta.tarefa_id = t.id
                    ), 0) AS DECIMAL(12,2)) AS custo_total,

                    (
                        SELECT COUNT(DISTINCT p.id)
                        FROM projeto p
                        INNER JOIN tarefa t ON t.projeto_id = p.id
                    ) AS total_projetos,

                    (
                        SELECT COUNT(t.id)
                        FROM tarefa t
                        WHERE t.status = 'concluida'
                    ) AS tarefas_concluidas,

                    (
                        SELECT COUNT(DISTINCT p.id)
                        FROM projeto p
                        WHERE EXISTS (
                            SELECT 1
                            FROM tarefa t
                            WHERE t.projeto_id = p.id
                        )
                        AND NOT EXISTS (
                            SELECT 1
                            FROM tarefa t2
                            WHERE t2.projeto_id = p.id
                              AND t2.status <> 'concluida'
                        )
                    ) AS projetos_concluidos,

                    (
                        SELECT COUNT(DISTINCT u.id)
                        FROM usuario u
                        INNER JOIN tarefa t ON t.responsavel_id = u.id
                        WHERE u.ativo = true
                    ) AS total_desenvolvedores
                """;

        return jdbcTemplate.queryForObject(
                sql,
                EmptySqlParameterSource.INSTANCE,
                (rs, rowNum) -> new DashboardFinanceiroQueryDto(
                        getBigDecimal(rs, "total_horas"),
                        getBigDecimal(rs, "custo_total"),
                        rs.getLong("total_projetos"),
                        rs.getLong("tarefas_concluidas"),
                        rs.getLong("projetos_concluidos"),
                        rs.getLong("total_desenvolvedores")
                )
        );
    }

    private static BigDecimal getBigDecimal(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? BigDecimal.ZERO : value;
    }
}