import { useState, useEffect, useLayoutEffect, useCallback, useMemo, useRef } from "react";
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
import { notificationsApi } from "../../api/notifications";
import { useAuth } from "../../context/AuthContext";
import Modal from "../../components/Modal";
import Button from "../../components/Button";
import DoctorFilterMenu from "../../components/Calendar/DoctorFilterMenu";
import { getDoctorColor } from "../../components/Calendar/doctorColors";
import {
    formatSchedule,
    isoDayOfWeek,
    isWorkingAt,
    normalizeSchedule,
} from "../../components/Calendar/workingHours";
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

// Sectiunea de notificari din modalul de detalii. Aceeasi paleta ca statusurile de programare,
// ca utilizatorul sa nu invete doua coduri de culoare diferite.
const NOTIFICATION_TRIGGER_LABELS = {
    CONFIRMATION: "Confirmare",
    REMINDER_24H: "Reamintire 24h",
    RESCHEDULED: "Reprogramare",
    CANCELLED: "Anulare",
};

const NOTIFICATION_STATUS_LABELS = {
    PENDING: "În așteptare",
    SENT: "Trimis",
    FAILED: "Eșuat",
    CANCELLED: "Anulat",
};

const NOTIFICATION_STATUS_COLORS = {
    PENDING: "#3174ad",
    SENT: "#2e8b57",
    FAILED: "#c0392b",
    CANCELLED: "#a0a0a0",
};

// Galbenul (IN_PROGRESS) si grina (CANCELLED) sunt prea deschise pentru text alb — pe ele
// scriem cu inchis, altfel eticheta nu se poate citi pe blocul colorat.
const DARK_TEXT_STATUSES = new Set(["IN_PROGRESS", "CANCELLED"]);

// Fundalul ramane statusul (legenda de sus, aceeasi de la Modulul 3), iar medicul e o dunga
// de culoare in stanga blocului. Asa se pot citi amandoua deodata; daca am fi colorat fundalul
// pe medic, statusul — informatia pe care se iau deciziile — ar fi disparut.
function eventStyleGetter(event) {
    const backgroundColor = STATUS_COLORS[event.status] || "#3174ad";
    return {
        style: {
            backgroundColor,
            color: DARK_TEXT_STATUSES.has(event.status) ? "#1f2937" : "#ffffff",
            borderLeft: `5px solid ${getDoctorColor(event.raw?.doctorId).dot}`,
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
            {appointment.serviceName && <div>{appointment.serviceName}</div>}
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

    // Pagina se intinde exact pana la marginea de jos a ferestrei, ca grila sa fie singurul lucru
    // care deruleaza. Inaltimea fixa de dinainte (80vh) plus navbar, titlu si legende depaseau
    // ecranul cu cateva sute de pixeli, asa ca pagina intra cu antetul deja iesit in sus.
    // Masuram, nu scadem o constanta: navbarul isi schimba inaltimea la wrap si intre roluri.
    const pageRef = useRef(null);
    const [pageHeight, setPageHeight] = useState(null);

    useLayoutEffect(() => {
        function measure() {
            if (!pageRef.current) return;
            const topInDocument = pageRef.current.getBoundingClientRect().top + window.scrollY;
            // Podeaua evita ca pe ecrane mici calendarul sa fie strivit pana la ilizibil —
            // acolo pagina redevine derulabila, ceea ce e in regula.
            setPageHeight(Math.max(window.innerHeight - topInDocument, 420));
        }

        measure();
        window.addEventListener("resize", measure);
        return () => window.removeEventListener("resize", measure);
    }, []);

    // Grila ramane ancorata la 08:00, dar la ora 15:00 primul lucru vizibil nu trebuie sa fie
    // dimineata deja consumata: o deschidem pe ora curenta, cu o jumatate de ora inainte, ca sa
    // se vada si ce tocmai s-a terminat. Inainte de 08:30 tinta ramane 08:00, adica varful grilei.
    //
    // Calculat o singura data, la montare: react-big-calendar aplica scroll-ul doar in
    // componentDidMount (TimeGrid.calculateScroll), iar o valoare care se schimba la fiecare
    // randare ar fi oricum ignorata dupa aceea — dar ar reintroduce un Date nou la fiecare poll.
    const initialScrollTime = useMemo(() => {
        const now = new Date();
        const minutesPastEight = (now.getHours() - 8) * 60 + now.getMinutes() - 30;

        // Construit pe ziua de azi, nu prin scaderea a 30 de minute din `now`: la 00:10 scaderea
        // ar sari in ziua precedenta, iar raportul calculat de biblioteca ar trimite grila la coada.
        const target = new Date(now);
        target.setHours(8, 0, 0, 0);
        if (minutesPastEight > 0) target.setMinutes(minutesPastEight);
        return target;
    }, []);

    // Filtrul de medici / cabinet (PR #159). `null` = DoctorFilterMenu inca nu a raportat nimic;
    // pana atunci nu tragem programari, ca sa nu facem doua cereri la fiecare montare a paginii.
    // Aceleasi roluri ca ALLOWED_ROLES din DoctorFilterMenu: pentru DOCTOR meniul nu se randeaza
    // (backendul ii forteaza oricum propriul calendar), deci nu avem ce astepta.
    const [filters, setFilters] = useState(null);
    const canFilter = role === "ADMIN" || role === "RECEPTION";

    const [modalMode, setModalMode] = useState(null); // "create" | "edit" | "details" | null
    const [selectedSlot, setSelectedSlot] = useState(null);
    const [selectedEvent, setSelectedEvent] = useState(null);
    const [cancelling, setCancelling] = useState(false);

    // Detaliul complet (AppointmentResponse) al programarii deschise in modalul de detalii,
    // necesar ca sa stim doctor.userId (nu vine in DTO-ul "slim" de calendar)
    const [eventDetail, setEventDetail] = useState(null);
    const [eventDetailLoading, setEventDetailLoading] = useState(false);
    // null = fetch-ul a esuat; [] = nu exista notificari pentru programarea asta
    const [eventNotifications, setEventNotifications] = useState([]);
    const [eventNotificationsLoading, setEventNotificationsLoading] = useState(false);
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

    // "Deselecteaza tot" trebuie sa insemne calendar gol. Backendul trateaza o lista goala de
    // doctorIds ca "toti medicii activi" (CalendarService.getByDateRangeAndDoctors), deci daca
    // am trimite cererea oricum am afisa exact opusul a ce a cerut utilizatorul.
    const nothingSelected = Boolean(filters) && !filters.roomId && filters.doctorIds?.length === 0;

    const fetchAppointments = useCallback(async ({ isPoll = false } = {}) => {
        // Pana cand filtrul raporteaza prima data, nu stim ce medici sa cerem — a doua cerere ar
        // fi oricum aruncata de prima notificare a meniului.
        if (canFilter && filters === null) return;

        const from = view === "day" ? startOfDay(date) : startOfWeek(date, { locale: ro });
        const to = view === "day" ? endOfDay(date) : endOfWeek(date, { locale: ro });

        try {
            const data = nothingSelected
                ? []
                : await appointmentApi.getCalendarAppointments({
                      from: from.toISOString(),
                      to: to.toISOString(),
                      doctorIds: filters?.doctorIds,
                      roomId: filters?.roomId,
                  });

            const mapped = data.map((appt) => ({
                id: appt.id,
                // Partile lipsa se sar, nu se interpoleaza: un serviciu nesetat producea
                // literalmente "Ion Vasilescu — null (Dr. Popescu)" in tooltip si in modal.
                title: [appt.patientName, appt.serviceName].filter(Boolean).join(" — ")
                    + (appt.doctorName ? ` (${appt.doctorName})` : ""),
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
    }, [view, date, filters, canFilter, nothingSelected]);

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

    // Referinta trebuie sa fie stabila: DoctorFilterMenu tine `onFilterChange` in dependentele
    // efectului care notifica, iar o functie noua la fiecare randare ar reporni debounce-ul la
    // nesfarsit (notificare -> randare -> efect -> notificare).
    const handleFilterChange = useCallback((next) => setFilters(next), []);

    // Cu un singur medic bifat, grila poate arata exact cand lucreaza el. Cu doi sau mai multi
    // orele s-ar suprapune si banda colorata n-ar mai insemna nimic, deci evidentierea se
    // aprinde doar pe selectie unica — la fel si pentru DOCTOR, care isi vede propriul calendar
    // fara sa aiba meniul de filtrare.
    const soloDoctor = useMemo(() => {
        const selected = filters?.selectedDoctors;
        return selected?.length === 1 ? selected[0] : null;
    }, [filters]);

    const soloSchedule = useMemo(() => normalizeSchedule(soloDoctor), [soloDoctor]);

    // Un medic fara nicio tura definita ar innegri toata grila, ceea ce arata a bug, nu a
    // informatie — in cazul asta lasam calendarul neutru.
    const highlightWorkingHours = Boolean(soloDoctor) && soloSchedule.length > 0;

    const slotPropGetter = useCallback(
        (slotDate) => {
            if (!highlightWorkingHours) return {};
            const minutes = slotDate.getHours() * 60 + slotDate.getMinutes();
            return isWorkingAt(soloSchedule, isoDayOfWeek(slotDate), minutes)
                ? { className: "rbc-slot-working" }
                : { className: "rbc-slot-off-duty" };
        },
        [highlightWorkingHours, soloSchedule]
    );

    // Vederea pe zi are o singura coloana: acolo "nu are program" e o afirmatie utila,
    // pentru ca inseamna ca ziua aceea e goala pentru medicul selectat.
    const soloDayShifts = useMemo(() => {
        if (!highlightWorkingHours || view !== "day") return null;
        return soloSchedule.filter((s) => s.dayOfWeek === isoDayOfWeek(date));
    }, [highlightWorkingHours, soloSchedule, view, date]);

    function handleSelectSlot(slotInfo) {
        setSelectedSlot(slotInfo);
        setModalMode("create");
    }

    async function handleSelectEvent(event) {
        setSelectedEvent(event);
        setModalMode("details");
        setEventDetail(null);
        setEventDetailLoading(true);
        setEventNotifications([]);

        // Notificarile se aduc doar aici, la deschiderea modalului — nu la fiecare randare a
        // calendarului. Endpoint-ul e ADMIN-only, deci pentru celelalte roluri nici nu il chemam
        // (un 403 in consola la fiecare click ar fi doar zgomot).
        if (role === "ADMIN") {
            setEventNotificationsLoading(true);
            notificationsApi
                .list({ appointmentId: event.id, size: 10 })
                .then((page) => setEventNotifications(page.content ?? []))
                .catch(() => setEventNotifications(null))
                .finally(() => setEventNotificationsLoading(false));
        }

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
        setEventNotifications([]);
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
        // Folosim eventDetail (AppointmentResponse complet, deja incarcat in modalul de detalii),
        // nu selectedEvent.raw — DTO-ul "slim" de calendar nu are patient/doctor/room/service, deci
        // formularul de reprogramare ramanea neprefillat cu ID-urile curente.
        //
        // Butoanele de actiune nu se randeaza cat timp detaliul se incarca, deci aici eventDetail
        // lipseste doar daca fetch-ul a picat — o spunem, in loc sa lasam butonul mort.
        if (!eventDetail) {
            toast.error("Detaliile programarii nu s-au putut incarca. Reincercati.");
            return;
        }
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

        // Fisa de consultatie e continut clinic: RECEPTION nu are acces (ConsultationController
        // e @PreAuthorize DOCTOR/ADMIN, iar ruta /appointments/:id/record e la fel de restrictiva),
        // deci butonul ar fi dus receptia intr-un perete.
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

    return (
        <div
            ref={pageRef}
            style={{
                padding: "20px",
                height: pageHeight ?? "80vh",
                boxSizing: "border-box",
                display: "flex",
                flexDirection: "column",
            }}
        >
            {/* Meniul de filtrare sta in afara blocului de `loading`: la prima incarcare el e cel
                care spune ce medici sa cerem, deci trebuie sa fie randat inainte de calendar.
                Pentru rolul DOCTOR componenta se ascunde singura. */}
            <div style={{ display: "flex", alignItems: "center", gap: "12px", flexWrap: "wrap" }}>
                <h1 style={{ margin: 0 }}>Calendar Programari</h1>
                <DoctorFilterMenu onFilterChange={handleFilterChange} />
            </div>

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
                        {/* Pentru DOCTOR toate blocurile sunt ale aceluiasi medic, deci dunga nu
                            distinge nimic si nota ar fi zgomot. */}
                        {canFilter && (
                            <span style={{ fontSize: "0.85rem", color: "#666" }}>
                                Dunga din stanga = medicul (aceleasi culori ca in filtru)
                            </span>
                        )}
                    </div>

                    {/* Cand un singur medic e bifat, spunem in clar al cui e programul colorat —
                        altfel banda alba din grila e ambigua intre "liber" si "inchis". */}
                    {highlightWorkingHours && (
                        <div
                            style={{
                                display: "flex",
                                alignItems: "center",
                                gap: "8px",
                                flexWrap: "wrap",
                                marginBottom: "10px",
                                fontSize: "0.85rem",
                                color: "#444",
                            }}
                        >
                            <span
                                aria-hidden="true"
                                style={{
                                    display: "inline-block",
                                    width: "12px",
                                    height: "12px",
                                    borderRadius: "3px",
                                    backgroundColor: "#ffffff",
                                    border: `2px solid ${getDoctorColor(soloDoctor.id).dot}`,
                                }}
                            />
                            <span>
                                Zonele albe = programul lui <strong>{soloDoctor.fullName}</strong>
                                {" — "}
                                {formatSchedule(soloSchedule)}
                            </span>
                        </div>
                    )}

                    {soloDayShifts?.length === 0 && (
                        <p
                            style={{
                                marginBottom: "10px",
                                fontSize: "0.85rem",
                                color: "#8a6d3b",
                            }}
                        >
                            {soloDoctor.fullName} nu are program de lucru in aceasta zi.
                        </p>
                    )}

                    {/* minHeight: 0 e obligatoriu — fara el copilul flex refuza sa coboare sub
                        inaltimea continutului, grila de 12 ore ramane la 1440px si impinge
                        pagina in jos exact ca inainte, doar ca prin alt drum. */}
                    <div style={{ flex: "1 1 auto", minHeight: 0 }}>
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
                            slotPropGetter={slotPropGetter}
                            components={{ event: EventContent }}
                            // Slot de 15 minute, 4 sloturi pe grup => o eticheta pe ora, dar
                            // selectia din grila cade pe :00 / :15 / :30 / :45.
                            step={15}
                            timeslots={4}
                            // Grila e limitata la programul clinicii (08:00-20:00), iar scroll-ul
                            // initial cade pe ora curenta (vezi initialScrollTime).
                            min={CALENDAR_MIN_TIME}
                            max={CALENDAR_MAX_TIME}
                            scrollToTime={initialScrollTime}
                        />
                    </div>

                    {events.length === 0 && !error && (
                        <p style={{ textAlign: "center", color: "#999", marginTop: "12px" }}>
                            {nothingSelected
                                ? "Niciun medic bifat in filtru — calendarul e gol."
                                : "Nicio programare in acest interval."}
                        </p>
                    )}
                </>
            )}

            {/* Modal creare programare */}
            <Modal isOpen={modalMode === "create"} onClose={closeModal} title="Programare noua">
                <AppointmentForm
                    initialData={createInitialData}
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
                        {/* Antet: cine si ce, cu statusul ca pastila — inainte totul era o singura
                            linie "Pacient — Serviciu (Medic)" din care nu se distingea nimic. */}
                        <div className="flex items-start justify-between gap-3 border-b border-border pb-3">
                            <div className="min-w-0">
                                <div className="truncate text-base font-semibold">
                                    {selectedEvent.raw.patientName}
                                </div>
                                <div className="truncate text-sm text-muted-foreground">
                                    {selectedEvent.raw.serviceName || "Serviciu nespecificat"}
                                </div>
                            </div>
                            <span
                                className="shrink-0 whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold text-white"
                                style={{ backgroundColor: STATUS_COLORS[selectedEvent.status] || "#a0a0a0" }}
                            >
                                {STATUS_LABELS[selectedEvent.status] || selectedEvent.status}
                            </span>
                        </div>

                        <dl className="mt-3 grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
                            <dt className="text-muted-foreground">Data</dt>
                            <dd className="font-medium">
                                {format(selectedEvent.start, "EEEE, dd MMMM yyyy", { locale: ro })}
                            </dd>

                            <dt className="text-muted-foreground">Interval</dt>
                            <dd className="font-medium tabular-nums">
                                {format(selectedEvent.start, "HH:mm")} — {format(selectedEvent.end, "HH:mm")}
                                <span className="ml-2 font-normal text-muted-foreground">
                                    ({Math.round((selectedEvent.end - selectedEvent.start) / 60000)} min)
                                </span>
                            </dd>

                            <dt className="text-muted-foreground">Medic</dt>
                            <dd className="font-medium">{selectedEvent.raw.doctorName}</dd>

                            {selectedEvent.raw.roomName && (
                                <>
                                    <dt className="text-muted-foreground">Cabinet</dt>
                                    <dd className="font-medium">{selectedEvent.raw.roomName}</dd>
                                </>
                            )}

                            {/* Din eventDetail, nu din DTO-ul de calendar, care nu le contine.
                                Apar doar dupa ce se incarca — de aceea sunt conditionate separat. */}
                            {eventDetail?.priceAtBooking != null && (
                                <>
                                    <dt className="text-muted-foreground">Preț</dt>
                                    <dd className="font-medium tabular-nums">
                                        {eventDetail.priceAtBooking} RON
                                    </dd>
                                </>
                            )}

                            {eventDetail?.notes && (
                                <>
                                    <dt className="text-muted-foreground">Observații</dt>
                                    <dd className="whitespace-pre-wrap">{eventDetail.notes}</dd>
                                </>
                            )}
                        </dl>

                        {eventDetailLoading && (
                            <p className="mt-3 text-sm text-muted-foreground">
                                Se încarcă acțiunile disponibile...
                            </p>
                        )}

                        {/* Coada de notificari e ADMIN-only in backend, deci pentru RECEPTION
                            si DOCTOR sectiunea nu se randeaza deloc (nu se randeaza goala). */}
                        {role === "ADMIN" && (
                            <div className="mt-4 border-t border-border pt-3">
                                <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                                    Notificări email
                                </h3>

                                {eventNotificationsLoading && (
                                    <p className="text-sm text-muted-foreground">Se încarcă notificările...</p>
                                )}

                                {!eventNotificationsLoading && eventNotifications === null && (
                                    <p className="text-sm text-destructive">
                                        Notificările nu au putut fi încărcate.
                                    </p>
                                )}

                                {!eventNotificationsLoading && eventNotifications?.length === 0 && (
                                    <p className="text-sm text-muted-foreground">
                                        Nicio notificare pentru această programare.
                                    </p>
                                )}

                                {!eventNotificationsLoading && eventNotifications?.length > 0 && (
                                    <ul className="divide-y divide-border overflow-hidden rounded-lg border border-border">
                                        {eventNotifications.map((n) => (
                                            <li
                                                key={n.id}
                                                className="flex items-center justify-between gap-3 px-3 py-2 text-sm"
                                            >
                                                <span className="min-w-0 truncate">
                                                    {NOTIFICATION_TRIGGER_LABELS[n.trigger] || n.trigger}
                                                </span>
                                                <span
                                                    className="shrink-0 font-medium"
                                                    style={{ color: NOTIFICATION_STATUS_COLORS[n.status] || "#a0a0a0" }}
                                                >
                                                    {NOTIFICATION_STATUS_LABELS[n.status] || n.status}
                                                </span>
                                                <span className="shrink-0 tabular-nums text-muted-foreground">
                                                    {format(new Date(n.sentAt ?? n.nextAttemptAt ?? n.createdAt), "dd.MM HH:mm")}
                                                </span>
                                            </li>
                                        ))}
                                    </ul>
                                )}
                            </div>
                        )}

                        {renderActionButtons()}
                    </div>
                )}
            </Modal>
        </div>
    );
}