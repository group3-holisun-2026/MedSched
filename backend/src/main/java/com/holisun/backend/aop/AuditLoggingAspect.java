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


        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getCredentials() instanceof String) {
            String token = (String) auth.getCredentials();
            try {
                userId = jwtUtil.extractUserId(token);
            } catch (Exception e) {

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