package com.holisun.backend.exception;

import com.holisun.backend.dto.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.DateTimeException;
import java.time.Instant;

/**
 * Centralizeaza maparea exceptiilor la un raspuns JSON consistent (ErrorResponse).
 * Extinde ResponseEntityExceptionHandler ca sa preia gratuit maparea corecta pt
 * exceptiile "de framework" ale Spring MVC (JSON invalid, @Valid esuat, ruta
 * nemapata — inclusiv ResponseStatusException folosit in RoomService/EquipmentService,
 * pt ca extinde ErrorResponseException care e acoperit acolo). Fara asta, catch-all-ul
 * de mai jos le-ar fi furat din lantul de rezolvare implicit al Spring si le-ar fi
 * transformat pe toate in 500 brut — orice @ExceptionHandler declarat aici (mostenit
 * sau propriu) are prioritate fata de rezolvarea implicita.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({EntityNotFoundException.class, ClinicalServiceException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<String> handleResourceConflictException(ResourceConflictException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(IllegalStateException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler({DateTimeException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Nu aveti drepturile necesare pentru aceasta actiune.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "A aparut o eroare neasteptata.", request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> OptimisticLockingFailureException(OptimisticLockingFailureException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Programarea a fost modificată între timp, reîncărcați și încercați din nou", request);
    }

    /**
     * Plasa de siguranta pentru constrangerile EXCLUDE de la nivel de Postgres (vezi V7__.sql) —
     * cazul in care doua tranzactii trec amandoua de AvailabilityValidatorService/
     * EquipmentAllocationService (verificare Java) in aceeasi fereastra de timp; a doua e respinsa
     * de DB la commit, nu de logica aplicatiei. Prinde si alte violari de integritate (unicitate etc.)
     * — tratate tot ca 409, nu ca 500, pentru ca reprezinta un conflict cu date deja existente.
     *
     * CannotAcquireLockException e inclus separat pentru ca, verificat empiric, doua INSERT-uri
     * concurente care se ciocnesc pe aceeasi constrangere EXCLUDE nu ajung mereu la o violare curata —
     * Postgres poate raporta un deadlock real intre cele doua tranzactii cat timp verifica indexul GiST
     * ("deadlock detected ... while checking exclusion constraint"), iar tranzactia aleasa victima
     * primeste asta, nu un DataIntegrityViolationException. E acelasi rezultat de business (a doua
     * cerere trebuie respinsa cu conflict, nu 500), doar alt tip de exceptie la nivel de driver.
     */
    @ExceptionHandler({DataIntegrityViolationException.class, CannotAcquireLockException.class})
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Resursa selectată a fost ocupată chiar înainte de salvare — reîncercați.", request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                               HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        String path = request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : request.getDescription(false);
        String message = body instanceof ProblemDetail problemDetail ? problemDetail.getDetail() : ex.getMessage();
        ErrorResponse errorBody = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path);
        return ResponseEntity.status(status).headers(headers).body(errorBody);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}