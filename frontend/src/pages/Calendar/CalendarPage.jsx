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
    const [switchingView, setSwitchingView] = useState(false);
    const [error, setError] = useState(null);
    const [pollFailCount, setPollFailCount] = useState(0);
    const pollingRef = useRef(null);
    const isFirstLoad = useRef(true);

    const [modalMode, setModalMode] = useState(null); // "create" | "edit" | "details" | null
    const [selectedSlot, setSelectedSlot] = useState(null);
    const [selectedEvent, setSelectedEvent] = useState(null);
    const [cancelling, setCancelling] = useState(false);

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
    }, [view, date]);

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