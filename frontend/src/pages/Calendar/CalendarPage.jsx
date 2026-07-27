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

// Doar time-of-day-ul din aceste date conteaza pentru react-big-calendar (min/max) - ziua e arbitrara.
const CALENDAR_MIN_TIME = new Date(1972, 0, 1, 8, 0, 0);
const CALENDAR_MAX_TIME = new Date(1972, 0, 1, 20, 0, 0);

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
    IN_PROGRESS: "Consultatie activa",
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
            setSwitchingView(true);
        }
        fetchAppointments();
    }, [fetchAppointments]);

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

    function handleFormSaved() {
        closeModal();
        fetchAppointments();
    }

    async function handleCancelAppointment() {
        if (!selectedEvent) return;
        setCancelling(true);
        try {
            await appointmentApi.cancel(selectedEvent.id);
            toast.success("Programarea a fost anulata.");
            closeModal();
            fetchAppointments();
        } catch (err) {
            toast.error("Anularea a esuat. Incercati din nou.");
        } finally {
            setCancelling(false);
        }
    }

    function handleEditFromDetails() {
        // Folosim eventDetail (AppointmentResponse complet), nu selectedEvent.raw (DTO-ul
        // "slim" de calendar) - raw nu are patient/doctor/room/service ca sa putem prefilla
        // formularul de reprogramare cu ID-urile curente.
        if (!eventDetail) return;
        setSelectedSlot({
            start: new Date(eventDetail.startTime),
            end: new Date(eventDetail.endTime),
            initialData: eventDetail,
        });
        setModalMode("edit");
    }

    // Actiune generica de tranzitie de status, cu stare de procesare si refresh calendar
    async function runTransition(apiCall, successMessage) {
        if (!selectedEvent) return;
        setActionProcessing(true);
        try {
            await apiCall(selectedEvent.id);
            toast.success(successMessage);
            closeModal();
            fetchAppointments();
        } catch (err) {
            const conflict = err.response?.status === 409;
            const message =
                (typeof err.response?.data === "string" ? err.response.data : err.response?.data?.message) ||
                "Actiunea a esuat. Incercati din nou.";
            toast.error(message);
            if (conflict) {
                // Altcineva a schimbat starea intre timp - resincronizam calendarul, nu lasam UI-ul desincronizat
                fetchAppointments();
            }
        } finally {
            setActionProcessing(false);
        }
    }

    function handleConfirm() {
        runTransition(appointmentApi.confirm, "Programarea a fost confirmata.");
    }

    function handleCheckIn() {
        runTransition(appointmentApi.checkIn, "Pacientul a fost marcat ca sosit.");
    }

    function handleNoShow() {
        runTransition(appointmentApi.noShow, "Programarea a fost marcata ca neprezentare.");
    }

    function handleOpenRecord() {
        navigate(`/appointments/${selectedEvent.id}/record`);
        closeModal();
    }

    // Determina daca programarea deschisa e a medicului curent (rol DOCTOR)
    const isOwnAppointment = eventDetail?.doctor?.userId === user?.id;

    function renderActionButtons() {
        if (!selectedEvent || eventDetailLoading) return null;

        const status = selectedEvent.status;
        const buttons = [];

        if (status === "SCHEDULED" && (role === "ADMIN" || role === "RECEPTION")) {
            buttons.push(
                <Button key="confirm" variant="outline" onClick={handleConfirm} disabled={actionProcessing}>
                    Confirma
                </Button>,
                <Button key="reprogram" variant="outline" onClick={handleEditFromDetails} disabled={actionProcessing}>
                    Reprogrameaza
                </Button>,
                <Button key="cancel" variant="outline" onClick={handleCancelAppointment} disabled={actionProcessing || cancelling}>
                    {cancelling ? "Se anuleaza..." : "Anuleaza"}
                </Button>
            );
        }

        if (status === "CONFIRMED" && (role === "ADMIN" || role === "RECEPTION")) {
            buttons.push(
                <Button key="checkin" variant="outline" onClick={handleCheckIn} disabled={actionProcessing}>
                    Pacient sosit
                </Button>,
                <Button key="noshow" variant="outline" onClick={handleNoShow} disabled={actionProcessing}>
                    Neprezentat
                </Button>,
                <Button key="reprogram" variant="outline" onClick={handleEditFromDetails} disabled={actionProcessing}>
                    Reprogrameaza
                </Button>,
                <Button key="cancel" variant="outline" onClick={handleCancelAppointment} disabled={actionProcessing || cancelling}>
                    {cancelling ? "Se anuleaza..." : "Anuleaza"}
                </Button>
            );
        }

        if (status === "CONFIRMED" && role === "DOCTOR" && isOwnAppointment) {
            buttons.push(
                <Button key="checkin" variant="outline" onClick={handleCheckIn} disabled={actionProcessing}>
                    Pacient sosit
                </Button>
            );
        }

        if (status === "IN_PROGRESS" && (role === "ADMIN" || (role === "DOCTOR" && isOwnAppointment))) {
            buttons.push(
                <Button key="record" variant="outline" onClick={handleOpenRecord} disabled={actionProcessing}>
                    Deschide fisa de consultatie
                </Button>
            );
        }

        if (status === "COMPLETED") {
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

    return (
        <div style={{ padding: "20px", height: "80vh" }}>
            <h1>Calendar Programari</h1>

            {error && <p style={{ color: "#c0392b" }}>{error}</p>}

            {pollFailCount > 0 && !error && (
                <p style={{ color: "#8a6d3b", fontSize: "0.85rem" }}>
                    Ultima actualizare automata a esuat, reincercam...
                </p>
            )}

            {loading && (
                <div style={{ padding: "40px", textAlign: "center", color: "#666" }}>
                    Se incarca calendarul...
                </div>
            )}

            {!loading && switchingView && (
                <p style={{ color: "#666", fontSize: "0.9rem" }}>Se actualizeaza...</p>
            )}

            {!loading && (
                <>
                    {/* Legenda de statusuri */}
                    <div style={{ display: "flex", gap: "16px", flexWrap: "wrap", marginBottom: "10px" }}>
                        {Object.entries(STATUS_LABELS).map(([key, label]) => (
                            <div key={key} style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "0.85rem" }}>
                                <span
                                    style={{
                                        display: "inline-block",
                                        width: "12px",
                                        height: "12px",
                                        borderRadius: "3px",
                                        backgroundColor: STATUS_COLORS[key],
                                    }}
                                />
                                {label}
                            </div>
                        ))}
                    </div>

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
                        min={CALENDAR_MIN_TIME}
                        max={CALENDAR_MAX_TIME}
                        selectable
                        onSelectSlot={handleSelectSlot}
                        onSelectEvent={handleSelectEvent}
                        eventPropGetter={eventStyleGetter}
                        components={{ event: EventContent }}
                        // Slot de 15 minute, 4 sloturi pe grup => o eticheta pe ora, dar
                        // selectia din grila cade pe :00 / :15 / :30 / :45.
                        step={15}
                        timeslots={4}
                        // Grila ramane pe 24h (nu ascundem nimic), dar se deschide la ora 7.
                        scrollToTime={new Date(1970, 0, 1, 7, 0, 0)}
                    />

                    {events.length === 0 && !error && (
                        <p style={{ textAlign: "center", color: "#999", marginTop: "12px" }}>
                            Nicio programare in acest interval.
                        </p>
                    )}
                </>
            )}

            {/* Modal creare programare */}
            <Modal isOpen={modalMode === "create"} onClose={closeModal} title="Programare noua">
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
            <Modal isOpen={modalMode === "edit"} onClose={closeModal} title="Reprogramare">
                <AppointmentForm
                    initialData={selectedSlot?.initialData}
                    onSave={handleFormSaved}
                    onCancel={closeModal}
                />
            </Modal>

            {/* Modal detalii programare */}
            <Modal isOpen={modalMode === "details"} onClose={closeModal} title="Detalii programare">
                {selectedEvent && (
                    <div>
                        <p><strong>{selectedEvent.title}</strong></p>
                        <p>Status: {STATUS_LABELS[selectedEvent.status] || selectedEvent.status}</p>
                        <p>
                            {format(selectedEvent.start, "dd.MM.yyyy HH:mm")} —{" "}
                            {format(selectedEvent.end, "HH:mm")}
                        </p>

                        {eventDetailLoading && <p style={{ color: "#666" }}>Se incarca actiunile disponibile...</p>}

                        {renderActionButtons()}
                    </div>
                )}
            </Modal>
        </div>
    );
}