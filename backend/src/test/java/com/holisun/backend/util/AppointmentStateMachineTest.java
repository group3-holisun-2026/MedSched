package com.holisun.backend.util;

import com.holisun.backend.enums.AppointmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AppointmentStateMachineTest {

    private AppointmentStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        // Inițializăm mașina de stări (nu are nevoie de mock-uri fiindcă e doar logică pură)
        stateMachine = new AppointmentStateMachine();
    }

    // Aici vom injecta toate perechile posibile din Enum
    static Stream<AppointmentTransition> provideAllTransitions() {
        return Stream.of(AppointmentStatus.values())
                .flatMap(from -> Stream.of(AppointmentStatus.values())
                        .map(to -> new AppointmentTransition(from, to)));
    }

    @ParameterizedTest
    @MethodSource("provideAllTransitions")
    void testStateTransitions(AppointmentTransition transition) {
        AppointmentStatus from = transition.from;
        AppointmentStatus to = transition.to;

        // Verificăm dacă este o tranziție validă conform secțiunii 1.1 din cerințe
        boolean isValid = isTransitionValid(from, to);

        if (isValid) {
            // Dacă e validă, nu ar trebui să arunce nicio excepție
            assertDoesNotThrow(() -> stateMachine.assertTransition(from, to),
                    "Tranziția din " + from + " în " + to + " ar fi trebuit să fie PERMISĂ.");
        } else {
            // Dacă e nevalidă, trebuie să arunce ResponseStatusException cu 409 CONFLICT
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> stateMachine.assertTransition(from, to),
                    "Tranziția din " + from + " în " + to + " ar fi trebuit să fie BLOCATĂ.");

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        }
    }

    // Logica manuală care reprezintă "Adevărul de Business" din cerințe
    private boolean isTransitionValid(AppointmentStatus from, AppointmentStatus to) {
        return (from == AppointmentStatus.SCHEDULED && (to == AppointmentStatus.CONFIRMED || to == AppointmentStatus.CANCELLED)) ||
                (from == AppointmentStatus.CONFIRMED && (to == AppointmentStatus.IN_PROGRESS || to == AppointmentStatus.NO_SHOW || to == AppointmentStatus.CANCELLED)) ||
                (from == AppointmentStatus.IN_PROGRESS && to == AppointmentStatus.COMPLETED);
    }

    // Clasă de suport pentru a ține perechile
    record AppointmentTransition(AppointmentStatus from, AppointmentStatus to) {}
}