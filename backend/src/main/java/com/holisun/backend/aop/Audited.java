package com.holisun.backend.aop;

import com.holisun.backend.enums.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Placeholder — owner real: P3 (documents/module2/backend_module2_tasks.md, sectiunea 3).
 * Marcheaza metodele de controller care trebuie logate in AuditLog. Fara
 * AuditLoggingAspect (P3, aop/AuditLoggingAspect.java), adnotarea nu are niciun
 * efect la runtime inca — e doar contractul pe care se agata P5.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {

    AuditAction action();

    String entityName();
}
