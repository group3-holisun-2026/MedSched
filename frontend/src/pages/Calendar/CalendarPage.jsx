import { useState, useEffect, useCallback, useMemo, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { Calendar, dateFnsLocalizer } from "react-big-calendar";
import format from "date-fns/format";
import parse from "date-fns/parse";
import startOfWeek from "date-fns/startOfWeek";
import endOfWeek from "date-fns/endOfWeek";
import startOfDay from "date-fns/startOfDay";
import endOfDay from "date-fns/endOfDay";
import getDay from "date-fns/getDay";
import ro from "date-fns/locale/ro";
import "react-big-calendar/lib/css/react-big-calendar.css";
import "./calendar-overrides.css";
import { appointmentApi } from "../../api/appointments";
import { useAuth } from "../../context/AuthContext";
import Modal from "../../components/Modal";
import Button from "../../components/Button";
import AppointmentForm from "./AppointmentForm";
import { toast } from "sonner";

const locales = { ro };

const localizer = dateFnsLocalizer({
  format,
  parse,
  startOfWeek: () => startOfWeek(new Date(), { locale: ro }),
  getDay,
  locales,
});

// Doar time-of-day-ul din aceste date conteaza pentru react-big-calendar (min/max) — ziua e
// arbitrara, dar TREBUIE sa fie apropiata de "acum", nu o data fixa din trecut (gen 1970/1972).
// Altfel react-big-calendar compara ore aflate in DST-uri diferite (ianuarie 1972 = ora de iarna
// UTC+2, iulie 2026 = ora de vara UTC+3) si intervalul afisat se decaleaza cu o ora fata de cel cerut.
const calendarBoundsRef = new Date();
const CALENDAR_MIN_TIME = new Date(calendarBoundsRef.getFullYear(), calendarBoundsRef.getMonth(), calendarBoundsRef.getDate(), 8, 0, 0);
const CALENDAR_MAX_TIME = new Date(calendarBoundsRef.getFullYear(), calendarBoundsRef.getMonth(), calendarBoundsRef.getDate(), 20, 0, 0);

const STATUS_COLORS = {
  SCHEDULED: "#3174ad",
  CONFIRMED: "#2e8b57",
  IN_PROGRESS: "#e0a800",
  COMPLETED: "#6c757d",
  NO_SHOW: "#c0392b",
  CANCELLED: "#a0a0a0",
};

const STATUS_LABELS = {
    SCHEDULED: "Programat",
    CONFIRMED: "Confirmat",
    IN_PROGRESS: "Consultație activă",
    COMPLETED: "Finalizat",
    NO_SHOW: "Neprezentat",
    CANCELLED: "Anulat",
};

// Galbenul (IN_PROGRESS) si grina (CANCELLED) sunt prea deschise pentru text alb — pe ele
// scriem cu inchis, altfel eticheta nu se poate citi pe blocul colorat.
const DARK_TEXT_STATUSES = new Set(["IN_PROGRESS", "CANCELLED"]);

function eventStyleGetter(event) {
    const backgroundColor = STATUS_COLORS[event.status] || "#3174ad";
    return {
        style: {
            backgroundColor,
            color: DARK_TEXT_STATUSES.has(event.status) ? "#1f2937" : "#ffffff",
        },
    };
}

// Titlul intr-o singura linie ("Pacient — Serviciu (Medic)") se reteza mereu. Il spargem
// pe randuri, ca sa se vada cat incape, si pastram textul complet in tooltip-ul nativ.
function EventContent({ event }) {
    const appointment = event.raw;
    return (
        <div title={event.title} style={{ lineHeight: 1.25, fontSize: "11px" }}>
            <div style={{ fontWeight: 600 }}>{appointment.patientName}</div>
            <div>{appointment.serviceName}</div>
            <div style={{ opacity: 0.85 }}>{appointment.doctorName}</div>
        </div>
    );
}

export default function CalendarPage() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const role = user?.role;

    const [view, setView] = useState("week");
    const [date, setDate] = useState(new Date());
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [switchingView, setSwitchingView] = useState(false);
    const [error, setError] = useState(null);
    const [pollFailCount, setPollFailCount] = useState(0);
    const pollingRef = useRef(null);
    const isFirstLoad = useRef(true);

    const [modalMode, setModalMode] = useState(null); // "create" | "edit" | "details" | null
    const [selectedSlot, setSelectedSlot] = useState(null);
    const [selectedEvent, setSelectedEvent] = useState(null);
    const [cancelling, setCancelling] = useState(false);

    // Detaliul complet (AppointmentResponse) al programarii deschise in modalul de detalii,
    // necesar ca sa stim doctor.userId (nu vine in DTO-ul "slim" de calendar)
    const [eventDetail, setEventDetail] = useState(null);
    const [eventDetailLoading, setEventDetailLoading] = useState(false);
    const [actionProcessing, setActionProcessing] = useState(false);

    // Memoizat: un obiect literal nou la fiecare randare ar reinitializa formularul de
    // programare (vezi efectul de hidratare din AppointmentForm).
    //
    // Ora slotului merge ca ora de perete locala, nu prin toISOString(): backendul lucreaza cu
    // LocalDateTime, iar formularul taie oricum stringul la 16 caractere. Cu UTC, un slot de 09:00
    // vara ajungea prefillat ca 06:00 (offsetul Romaniei) si se salva tot asa.
    const createInitialData = useMemo(
        () => (selectedSlot ? { startTime: format(selectedSlot.start, "yyyy-MM-dd'T'HH:mm") } : null),
        [selectedSlot]
    );

    const fetchAppointments = useCallback(async ({ isPoll = false } = {}) => {
        const from = view === "day" ? startOfDay(date) : startOfWeek(date, { locale: ro });
        const to = view === "day" ? endOfDay(date) : endOfWeek(date, { locale: ro });

        try {
            const data = await appointmentApi.getCalendarAppointments({
                from: from.toISOString(),
                to: to.toISOString(),
            });

            const mapped = data.map((appt) => ({
                id: appt.id,
                title: `${appt.patientName} — ${appt.serviceName} (${appt.doctorName})`,
                start: new Date(appt.startTime),
                end: new Date(appt.endTime),
                status: appt.status,
                raw: appt,
            }));

            setEvents(mapped);
            setError(null);
            setPollFailCount(0);
        } catch (err) {
            if (isPoll) {
                setPollFailCount((prev) => prev + 1);
            } else {
                setError("Nu am putut incarca programarile. Reincercam...");
            }
        } finally {
            setLoading(false);
            setSwitchingView(false);
            isFirstLoad.current = false;
        }
    }, [view, date]);

    useEffect(() => {
        if (isFirstLoad.current) {
            setLoading(true);
        } else {
          setError("Nu am putut incarca programarile. Reincercam...");
        }
      } finally {
        setLoading(false);
        setSwitchingView(false);
        isFirstLoad.current = false;
      }
    },
    [view, date],
  );

    // Fetch initial + la schimbare vedere/data
    useEffect(() => {
        if (isFirstLoad.current) {
            setLoading(true);
        } else {
            setSwitchingView(true);
        }
        fetchAppointments();
    }, [fetchAppointments]);

    // Nu facem polling cat timp un modal e deschis: reimprospatarea re-randeaza pagina sub
    // formularul pe care userul tocmai il completeaza, fara ca el sa vada calendarul oricum.
    useEffect(() => {
        if (modalMode) return undefined;

        pollingRef.current = setInterval(() => {
            fetchAppointments({ isPoll: true });
        }, 20000);

        return () => clearInterval(pollingRef.current);
    }, [fetchAppointments, modalMode]);

    return () => clearInterval(pollingRef.current);
  }, [fetchAppointments]);

function handleSelectSlot(slotInfo) {
    setSelectedSlot(slotInfo);
    setModalMode("create");
}

async function handleSelectEvent(event) {
    setSelectedEvent(event);
    setModalMode("details");
    setEventDetail(null);
    setEventDetailLoading(true);

    try {
        const detail = await appointmentApi.getById(event.id);
        setEventDetail(detail);
    } catch (err) {
        toast.error("Nu s-au putut incarca detaliile programarii.");
    } finally {
        setEventDetailLoading(false);
    }
}

function closeModal() {
    setModalMode(null);
    setSelectedSlot(null);
    setSelectedEvent(null);
    setEventDetail(null);
}

  function closeModal() {
    setModalMode(null);
    setSelectedSlot(null);
    setSelectedEvent(null);
  }

  function handleFormSaved() {
    closeModal();
    fetchAppointments();
  }

  // Citeste mesajul de eroare din raspunsul serverului indiferent de shape: ResourceConflictException
  // (folosit la crearea/reprogramarea unei programari, Modulul 3) inca intoarce text brut in body, in
  // timp ce noile endpoint-uri de tranzitie (confirm/check-in/no-show/complete/cancel, respinse de
  // AppointmentStateMachine via ResponseStatusException) trec prin GlobalExceptionHandler si ajung ca
  // ErrorResponse standard cu camp `message`. Verificam ambele forme ca sa nu ajungem cu mesajul
  // generic de fallback cand serverul chiar a trimis un mesaj util.
  function extractErrorMessage(err, fallback) {
    const data = err?.response?.data;
    if (typeof data === "string" && data.trim()) return data;
    if (data && typeof data.message === "string" && data.message.trim())
      return data.message;
    return fallback;
  }

  // Ruleaza o tranzitie de status pe programarea selectata: stare de "se proceseaza" (dezactiveaza
  // toate butoanele de actiune cat timp e in curs), toast de reusita/eroare, si tratare speciala
  // pentru 409 (tranzitie invalida - de ex. altcineva a actionat deja din alta sesiune, iar noi inca
  // vedem starea veche din cauza polling-ului de 20s): afisam mesajul de conflict venit din server SI
  // reimprospatam automat calendarul, ca userul sa vada starea reala curenta, nu doar un toast izolat
  // care lasa UI-ul desincronizat.
  async function runTransition(
    action,
    apiCall,
    successMessage,
    conflictFallback,
  ) {
    if (!selectedEvent || processingAction) return;
    setProcessingAction(action);
    try {
      await apiCall(selectedEvent.id);
      toast.success(successMessage);
      closeModal();
      fetchAppointments();
    } catch (err) {
      if (err.response?.status === 409) {
        toast.error(extractErrorMessage(err, conflictFallback));
        closeModal(); // selectedEvent nu mai reflecta starea reala - nu il tinem deschis pe date vechi
        fetchAppointments();
      } else {
        toast.error(
          extractErrorMessage(err, "Actiunea a esuat. Incercati din nou."),
        );
      }
    } finally {
      setProcessingAction(null);
    }
  }

  function handleConfirmAppointment() {
    return runTransition(
      "confirm",
      appointmentApi.confirm,
      "Programarea a fost confirmata.",
      "Programarea nu mai poate fi confirmata - altcineva a schimbat deja statusul.",
    );
  }

function renderActionButtons() {
        if (!selectedEvent || eventDetailLoading) return null;

        const status = selectedEvent.status;
        const busy = processingAction !== null;
        const buttons = [];

        if (status === "SCHEDULED" && (role === "ADMIN" || role === "RECEPTION")) {
            buttons.push(
                <Button key="confirm" variant="outline" onClick={handleConfirm} disabled={busy}>
                    {processingAction === "confirm" ? "Se confirma..." : "Confirma"}
                </Button>,
                <Button key="reprogram" variant="outline" onClick={handleEditFromDetails} disabled={busy}>
                    Reprogrameaza
                </Button>,
                <Button key="cancel" variant="outline" onClick={handleCancelAppointment} disabled={busy}>
                    {processingAction === "cancel" ? "Se anuleaza..." : "Anuleaza"}
                </Button>
            );
        }

        if (status === "CONFIRMED" && (role === "ADMIN" || role === "RECEPTION")) {
            buttons.push(
                <Button key="checkin" variant="outline" onClick={handleCheckIn} disabled={busy}>
                    {processingAction === "checkIn" ? "Se proceseaza..." : "Pacient sosit"}
                </Button>,
                <Button key="noshow" variant="outline" onClick={handleNoShow} disabled={busy}>
                    {processingAction === "noShow" ? "Se proceseaza..." : "Neprezentat"}
                </Button>,
                <Button key="reprogram" variant="outline" onClick={handleEditFromDetails} disabled={busy}>
                    Reprogrameaza
                </Button>,
                <Button key="cancel" variant="outline" onClick={handleCancelAppointment} disabled={busy}>
                    {processingAction === "cancel" ? "Se anuleaza..." : "Anuleaza"}
                </Button>
            );
        }

        if (status === "CONFIRMED" && role === "DOCTOR" && isOwnAppointment) {
            buttons.push(
                <Button key="checkin" variant="outline" onClick={handleCheckIn} disabled={busy}>
                    {processingAction === "checkIn" ? "Se proceseaza..." : "Pacient sosit"}
                </Button>
            );
        }

        if (status === "IN_PROGRESS" && (role === "ADMIN" || (role === "DOCTOR" && isOwnAppointment))) {
            buttons.push(
                <Button key="record" variant="outline" onClick={handleOpenRecord} disabled={busy}>
                    Deschide fisa de consultatie
                </Button>
            );
        }

        if (status === "COMPLETED" && (role === "ADMIN" || role === "DOCTOR")) {
            buttons.push(
                <Button key="view-record" variant="outline" onClick={handleOpenRecord}>
                    Vezi fisa
                </Button>
            );
        }

        if (buttons.length === 0) return null;

        return (
            <div style={{ display: "flex", gap: "8px", marginTop: "16px", flexWrap: "wrap" }}>
                {buttons}
            </div>
        );
    }
