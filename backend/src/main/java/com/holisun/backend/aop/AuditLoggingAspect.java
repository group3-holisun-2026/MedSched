package com.holisun.backend.aop;

import com.holisun.backend.entity.AuditLog;
import com.holisun.backend.repository.AuditLogRepository;
import com.holisun.backend.security.JwtUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Aspect
@Component
public class AuditLoggingAspect {

    /** Folosit doar cand chiar nu exista utilizator autentificat (nu ar trebui sa apara pe /api/**). */
    private static final UUID UNKNOWN_USER = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final AuditLogRepository auditLogRepository;
    private final JwtUtil jwtUtil;

    public AuditLoggingAspect(AuditLogRepository auditLogRepository, JwtUtil jwtUtil) {
        this.auditLogRepository = auditLogRepository;
        this.jwtUtil = jwtUtil;
    }

    @Around("@annotation(audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Object result = joinPoint.proceed();


        UUID entityId = null;
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof UUID) {
                entityId = (UUID) arg;
                break;
            }
        }


        // JwtAuthenticationFilter pune userId-ul (UUID) ca PRINCIPAL si lasa credentials null.
        // Varianta veche citea getCredentials() ca String, deci nu se potrivea niciodata si toate
        // intrarile ajungeau pe UUID-ul zero — adica exact intrebarea la care trebuie sa raspunda
        // auditul ("cine a deschis fisa pacientului X", NFR-1) ramanea fara raspuns.
        UUID userId = UNKNOWN_USER;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            if (auth.getPrincipal() instanceof UUID principalId) {
                userId = principalId;
            } else if (auth.getCredentials() instanceof String token) {
                // Plasa de siguranta daca cineva schimba filtrul sa puna token-ul in credentials.
                try {
                    userId = jwtUtil.extractUserId(token);
                } catch (Exception ignored) {
                    // ramane UNKNOWN_USER
                }
            }
        }


        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(audited.action().name());
        log.setEntityName(audited.entityName());
        log.setEntityId(entityId != null ? entityId : UUID.randomUUID());
        log.setTimestamp(LocalDateTime.now());

        auditLogRepository.save(log);

        return result;
    }
}