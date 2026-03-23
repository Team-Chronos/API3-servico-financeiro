package com.api.financeiro.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FinanceiroQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FinanceiroQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}