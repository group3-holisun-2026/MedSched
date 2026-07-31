import { useState, useEffect, useCallback, useRef } from "react";
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
import { appointmentApi } from "../../api/appointments";
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

const STATUS_COLORS = {
  SCHEDULED: "#3174ad",
  CONFIRMED: "#2e8b57",
  IN_PROGRESS: "#e0a800",
  COMPLETED: "#6c757d",
  NO_SHOW: "#c0392b",
  CANCELLED: "#a0a0a0",
};

function eventStyleGetter(event) {
  const backgroundColor = STATUS_COLORS[event.status] || "#3174ad";
  return { style: { backgroundColor } };
}

export default function CalendarPage() {
  const navigate = useNavigate();
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
  // O singura stare pentru toate actiunile de tranzitie (confirm/check-in/no-show/cancel) de pe
  // programarea selectata - null cand nu ruleaza nimic, altfel numele actiunii curente. Dezactivam
  // TOATE butoanele cat timp una e in curs (nu doar butonul apasat), ca sa nu putem trimite doua
  // tranzitii simultan pe aceeasi programare (ex. click Confirma, apoi click Anuleaza inainte sa
  // revina primul raspuns).
  const [processingAction, setProcessingAction] = useState(null);

  const fetchAppointments = useCallback(
    async ({ isPoll = false } = {}) => {
      const from =
        view === "day" ? startOfDay(date) : startOfWeek(date, { locale: ro });
      const to =
        view === "day" ? endOfDay(date) : endOfWeek(date, { locale: ro });

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
          // La polling, nu bombardam userul cu erori repetate - doar incrementam contorul
          setPollFailCount((prev) => prev + 1);
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

  // Polling la fiecare 20s
  useEffect(() => {
    pollingRef.current = setInterval(() => {
      fetchAppointments({ isPoll: true });
    }, 20000);

    return () => clearInterval(pollingRef.current);
  }, [fetchAppointments]);

  function handleSelectSlot(slotInfo) {
    setSelectedSlot(slotInfo);
    setModalMode("create");
  }

  function handleSelectEvent(event) {
    setSelectedEvent(event);
    setModalMode("details");
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

  function handleCheckIn() {
    return runTransition(
      "checkIn",
      appointmentApi.checkIn,
      "Pacientul a fost inregistrat ca sosit.",
      "Check-in-ul nu mai e posibil - statusul programarii s-a schimbat intre timp.",
    );
  }

  function handleNoShow() {
    return runTransition(
      "noShow",
      appointmentApi.noShow,
      "Programarea a fost marcata ca neprezentare.",
      "Actiunea nu mai e posibila - statusul programarii s-a schimbat intre timp.",
    );
  }

  function handleCancelAppointment() {
    return runTransition(
      "cancel",
      appointmentApi.cancel,
      "Programarea a fost anulata.",
      "Anularea nu mai e posibila - altcineva a schimbat deja statusul programarii.",
    );
  }

  function handleEditFromDetails() {
    const raw = selectedEvent.raw;
    setSelectedSlot({
      start: new Date(raw.startTime),
      end: new Date(raw.endTime),
      initialData: raw,
    });
    setModalMode("edit");
  }

  return (
    <div style={{ padding: "20px", height: "80vh" }}>
      <h1>Calendar Programari</h1>

      {/* Eroare la incarcarea initiala/schimbare vedere - vizibila, cu retry implicit prin polling */}
      {error && <p style={{ color: "#c0392b" }}>{error}</p>}

      {/* Eroare discreta la polling esuat repetat - nu bombardam cu toast la fiecare interval */}
      {pollFailCount > 0 && !error && (
        <p style={{ color: "#8a6d3b", fontSize: "0.85rem" }}>
          Ultima actualizare automata a esuat, reincercam...
        </p>
      )}

      {/* Loading state: incarcare initiala */}
      {loading && (
        <div style={{ padding: "40px", textAlign: "center", color: "#666" }}>
          Se incarca calendarul...
        </div>
      )}

      {/* Loading state: schimbare vedere/data, calendarul ramane vizibil dedesubt */}
      {!loading && switchingView && (
        <p style={{ color: "#666", fontSize: "0.9rem" }}>Se actualizeaza...</p>
      )}

      {!loading && (
        <>
          <Calendar
            localizer={localizer}
            events={events}
            startAccessor="start"
            endAccessor="end"
            view={view}
            date={date}
            onView={setView}
            onNavigate={setDate}
            views={["day", "week"]}
            style={{ height: "100%" }}
            selectable
            onSelectSlot={handleSelectSlot}
            onSelectEvent={handleSelectEvent}
            eventPropGetter={eventStyleGetter}
          />

          {/* Empty state: nicio programare in intervalul vizualizat */}
          {events.length === 0 && !error && (
            <p
              style={{ textAlign: "center", color: "#999", marginTop: "12px" }}
            >
              Nicio programare in acest interval.
            </p>
          )}
        </>
      )}

      {/* Modal creare programare */}
      <Modal
        isOpen={modalMode === "create"}
        onClose={closeModal}
        title="Programare noua"
      >
        <AppointmentForm
          initialData={
            selectedSlot
              ? { startTime: selectedSlot.start.toISOString() }
              : null
          }
          onSave={handleFormSaved}
          onCancel={closeModal}
        />
      </Modal>

      {/* Modal reprogramare (editare) */}
      <Modal
        isOpen={modalMode === "edit"}
        onClose={closeModal}
        title="Reprogramare"
      >
        <AppointmentForm
          initialData={selectedSlot?.initialData}
          onSave={handleFormSaved}
          onCancel={closeModal}
        />
      </Modal>

      {/* Modal detalii programare */}
      <Modal
        isOpen={modalMode === "details"}
        onClose={closeModal}
        title="Detalii programare"
      >
        {selectedEvent && (
          <div>
            <p>
              <strong>{selectedEvent.title}</strong>
            </p>
            <p>Status: {selectedEvent.status}</p>
            <p>
              {format(selectedEvent.start, "dd.MM.yyyy HH:mm")} —{" "}
              {format(selectedEvent.end, "HH:mm")}
            </p>

            {/*
                            Panou de actiuni, conditionat de statusul curent (conform masinii de stari
                            din backend_module4_tasks.md §1.1: SCHEDULED->CONFIRMED/CANCELLED,
                            CONFIRMED->IN_PROGRESS/NO_SHOW/CANCELLED, restul sunt stari terminale).
                            Filtrarea suplimentara pe ROL (ex. check-in doar pe propria programare
                            pentru DOCTOR) e un item separat, nu il implementam aici - nu e o gaura de
                            securitate, backend-ul respinge oricum cu 403 orice actiune neautorizata,
                            indiferent ce arata UI-ul.
                        */}
            {(selectedEvent.status === "SCHEDULED" ||
              selectedEvent.status === "CONFIRMED") && (
              <div
                style={{
                  display: "flex",
                  gap: "8px",
                  marginTop: "16px",
                  flexWrap: "wrap",
                }}
              >
                {selectedEvent.status === "SCHEDULED" && (
                  <Button
                    variant="primary"
                    onClick={handleConfirmAppointment}
                    disabled={processingAction !== null}
                  >
                    {processingAction === "confirm"
                      ? "Se confirma..."
                      : "Confirma"}
                  </Button>
                )}

                {selectedEvent.status === "CONFIRMED" && (
                  <>
                    <Button
                      variant="primary"
                      onClick={handleCheckIn}
                      disabled={processingAction !== null}
                    >
                      {processingAction === "checkIn"
                        ? "Se proceseaza..."
                        : "Pacient sosit"}
                    </Button>
                    <Button
                      variant="secondary"
                      onClick={handleNoShow}
                      disabled={processingAction !== null}
                    >
                      {processingAction === "noShow"
                        ? "Se proceseaza..."
                        : "Neprezentat"}
                    </Button>
                  </>
                )}

                <Button
                  variant="outline"
                  onClick={handleEditFromDetails}
                  disabled={processingAction !== null}
                >
                  Reprogrameaza
                </Button>
                <Button
                  variant="outline"
                  onClick={handleCancelAppointment}
                  disabled={processingAction !== null}
                >
                  {processingAction === "cancel"
                    ? "Se anuleaza..."
                    : "Anuleaza programarea"}
                </Button>
              </div>
            )}

            {selectedEvent.status === "IN_PROGRESS" && (
              <div style={{ marginTop: "16px" }}>
                <Button
                  variant="outline"
                  onClick={() =>
                    navigate(`/appointments/${selectedEvent.id}/record`)
                  }
                >
                  Deschide fisa de consultatie
                </Button>
              </div>
            )}

            {selectedEvent.status === "COMPLETED" && (
              <div style={{ marginTop: "16px" }}>
                <Button
                  variant="outline"
                  onClick={() =>
                    navigate(`/appointments/${selectedEvent.id}/record`)
                  }
                >
                  Vezi fisa
                </Button>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
