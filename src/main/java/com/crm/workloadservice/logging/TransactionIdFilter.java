package com.crm.workloadservice.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class TransactionIdFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TransactionIdFilter.class);
    public static final String TX_HEADER = "X-Transaction-Id";
    public static final String TX_MDC_KEY = "transactionId";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) req;

        String txId = http.getHeader(TX_HEADER);
        if (txId == null || txId.isBlank()) {
            txId = UUID.randomUUID().toString();
        }
        MDC.put(TX_MDC_KEY, txId);
        try {
            chain.doFilter(req, res);
            log.info("Transaction: {} {} -> status={}",
                    http.getMethod(), http.getRequestURI(),
                    ((HttpServletResponse) res).getStatus());
        } finally {
            MDC.clear();
        }
    }
}