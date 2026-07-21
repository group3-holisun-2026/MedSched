import { useState, useEffect, useCallback, useRef } from "react";
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
    const [view, setView] = useState("week");
    const [date, setDate] = useState(new Date());
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const pollingRef = useRef(null);

    const [modalMode, setModalMode] = useState(null); // "create" | "details" | null
    const [selectedSlot, setSelectedSlot] = useState(null);
    const [selectedEvent, setSelectedEvent] = useState(null);
    const [cancelling, setCancelling] = useState(false);

    const fetchAppointments = useCallback(async () => {
        const from = view === "day" ? startOfDay(date) : startOfWeek(date, { locale: ro });
        const to = view === "day" ? endOfDay(date) : endOfWeek(date, { locale: ro });

        try {
            const data = await getCalendarAppointments({
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
        } catch (err) {
            setError("Actualizarea a esuat, reincercam...");
        } finally {
            setLoading(false);
        }
    }, [view, date]);

    useEffect(() => {
        setLoading(true);
        fetchAppointments();
    }, [fetchAppointments]);

    useEffect(() => {
        pollingRef.current = setInterval(() => {
            fetchAppointments();
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

    // Pre-completeaza formularul cu datele evenimentului selectat, pentru reprogramare
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

            {error && <p style={{ color: "#c0392b" }}>{error}</p>}
            {loading && <p>Se incarca...</p>}

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
                        <p>Status: {selectedEvent.status}</p>
                        <p>
                            {format(selectedEvent.start, "dd.MM.yyyy HH:mm")} —{" "}
                            {format(selectedEvent.end, "HH:mm")}
                        </p>

                        {selectedEvent.status !== "CANCELLED" && selectedEvent.status !== "COMPLETED" && (
                            <div style={{ display: "flex", gap: "8px", marginTop: "16px" }}>
                                <Button variant="outline" onClick={handleEditFromDetails}>
                                    Reprogrameaza
                                </Button>
                                <Button
                                    variant="outline"
                                    onClick={handleCancelAppointment}
                                    disabled={cancelling}
                                >
                                    {cancelling ? "Se anuleaza..." : "Anuleaza programarea"}
                                </Button>
                            </div>
                        )}
                    </div>
                )}
            </Modal>
        </div>
    );
}