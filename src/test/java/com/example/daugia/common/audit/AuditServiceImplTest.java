package com.example.daugia.common.audit;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AuditServiceImplTest {

    @Test
    void logMethodsUseRequiresNewPropagation() throws Exception {
        Method withRequest = AuditServiceImpl.class.getMethod("log", String.class, AuditAction.class, String.class,
                String.class, AuditOutcome.class, jakarta.servlet.http.HttpServletRequest.class, String.class);
        Method withoutRequest = AuditServiceImpl.class.getMethod("log", String.class, AuditAction.class, String.class,
                String.class, AuditOutcome.class, String.class);

        Transactional withRequestTx = withRequest.getAnnotation(Transactional.class);
        Transactional withoutRequestTx = withoutRequest.getAnnotation(Transactional.class);

        assertThat(withRequestTx).isNotNull();
        assertThat(withRequestTx.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        assertThat(withoutRequestTx).isNotNull();
        assertThat(withoutRequestTx.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}