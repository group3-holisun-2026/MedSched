import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { useAuth } from '../../context/AuthContext';
import { appointmentApi } from '../../api/appointments';
import { consultationRecordApi } from '../../api/consultationRecord';
import ConsultationRecordForm from './ConsultationRecordForm';
import Card from '../../components/Card';
import Button from '../../components/Button';

const GRACE_MINUTES = 30;

const STATUS_LABELS = {
    SCHEDULED: 'Programat',
    CONFIRMED: 'Confirmat',
    IN_PROGRESS: 'Consultație activă',
    COMPLETED: 'Finalizat',
    NO_SHOW: 'Neprezentat',
    CANCELLED: 'Anulat',
};

const STATUS_STYLES = {
    SCHEDULED: 'bg-sky-50 text-sky-700 ring-sky-600/20',
    CONFIRMED: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
    IN_PROGRESS: 'bg-amber-50 text-amber-700 ring-amber-600/20',
    COMPLETED: 'bg-slate-100 text-slate-600 ring-slate-500/20',
    NO_SHOW: 'bg-red-50 text-red-700 ring-red-600/20',
    CANCELLED: 'bg-slate-100 text-slate-500 ring-slate-400/20',
};

const DATE_LONG = new Intl.DateTimeFormat('ro-RO', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
});

function hourLabel(value) {
    return value.toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' });
}

function triggerDownload(blob, fileName) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    // Fara revoke, fiecare export lasa un blob in memorie pana la reincarcarea paginii.
    URL.revokeObjectURL(url);
}

export default function ConsultationRecordPage() {
    const { appointmentId } = useParams();
    const navigate = useNavigate();
    const { user } = useAuth();

    const [record, setRecord] = useState(null);
    const [appointment, setAppointment] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [finalizing, setFinalizing] = useState(false);
    const [exporting, setExporting] = useState(false);
    const [error, setError] = useState(null);
    const [notFound, setNotFound] = useState(false);

    // Recalcul la fiecare minut doar ca sa reimprospatam etichetele de timp din bannere
    // (ferestrele sunt derivate local din datele programarii, fara polling dedicat).
    const [, setTick] = useState(0);

    useEffect(() => {
        let cancelled = false;

        async function fetchAll() {
            setLoading(true);
            setError(null);
            setNotFound(false);
            try {
                // Fisa poate lipsi (404 = nu a fost creata inca) — nu e o eroare de pagina.
                const [recordResult, appointmentResult] = await Promise.allSettled([
                    consultationRecordApi.getByAppointmentId(appointmentId),
                    appointmentApi.getById(appointmentId),
                ]);

                if (cancelled) return;

                if (recordResult.status === 'fulfilled') {
                    setRecord(recordResult.value);
                } else {
                    setNotFound(true);
                }

                if (appointmentResult.status === 'fulfilled') {
                    setAppointment(appointmentResult.value);
                } else {
                    setError('Nu am putut incarca datele programarii.');
                }
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        fetchAll();
        return () => {
            cancelled = true;
        };
    }, [appointmentId]);

    useEffect(() => {
        const interval = setInterval(() => setTick((t) => t + 1), 60_000);
        return () => clearInterval(interval);
    }, []);

    const status = appointment?.status;
    const isOwnAppointment = appointment?.doctor?.userId === user?.id;
    const canFinalize = user?.role === 'ADMIN' || (user?.role === 'DOCTOR' && isOwnAppointment);

    const startTime = appointment ? new Date(appointment.startTime) : null;
    const endTime = appointment ? new Date(appointment.endTime) : null;

    // Fisa apartine consultatiei: se scrie din momentul in care programarea incepe si pana la
    // 30 de minute dupa ora la care ar fi trebuit sa se termine. Aceeasi regula e verificata si
    // pe server (ConsultationRecordServiceImpl.assertWithinEditWindow) — aici doar o oglindim,
    // ca UI-ul sa nu ofere un camp editabil pe care salvarea l-ar refuza.
    const editWindowEnd = endTime ? new Date(endTime.getTime() + GRACE_MINUTES * 60_000) : null;
    const now = new Date();
    const beforeWindow = !!startTime && now < startTime;
    const afterWindow = !!editWindowEnd && now > editWindowEnd;

    const graceDeadline = appointment?.completedAt
        ? new Date(new Date(appointment.completedAt).getTime() + GRACE_MINUTES * 60_000)
        : null;

    // Job-ul de pe backend pune `locked` o data pe minut, deci la minutul 30 fisa poate fi
    // inca `locked: false` desi serverul respinge deja scrierea. Calculam expirarea si local
    // (efectul de mai sus re-randeaza din minut in minut), ca UI-ul sa nu promita un camp
    // editabil pe care salvarea l-ar refuza.
    const graceExpired = status === 'COMPLETED' && (!graceDeadline || now > graceDeadline);

    const isLocked = record?.locked === true || graceExpired || beforeWindow || afterWindow;

    // Motivul blocarii nu e acelasi lucru cu blocarea: "inca nu se poate" si "nu se mai poate"
    // cer actiuni diferite de la medic, iar un mesaj unic le-ar confunda.
    let readOnlyReason = null;
    if (beforeWindow) {
        readOnlyReason = `Fișa se poate completa începând cu ora ${hourLabel(startTime)}, când începe programarea.`;
    } else if (afterWindow) {
        readOnlyReason = `Fișa se putea completa până la ora ${hourLabel(editWindowEnd)} (${GRACE_MINUTES} de minute după terminarea programării).`;
    } else if (isLocked) {
        readOnlyReason = 'Fișa este blocată (perioada de completare a expirat) — doar în citire.';
    }

    // Banner de gratie doar cat timp fisa chiar mai e editabila; cand devine blocata,
    // mesajul din formular acopera deja cazul final (nu afisam ambele).
    const showGraceBanner = status === 'COMPLETED' && !isLocked && !!graceDeadline;

    const saveRecord = useCallback(
        async (formData) => {
            setSaving(true);
            setError(null);
            try {
                const saved = record
                    ? await consultationRecordApi.update(appointmentId, formData)
                    : await consultationRecordApi.create(appointmentId, formData);
                setRecord(saved);
                setNotFound(false);
                toast.success('Fisa a fost salvata.');
                return saved;
            } catch (err) {
                const message =
                    err.response?.status === 403
                        ? 'Nu aveti drepturi asupra acestei fise.'
                        : err.response?.data?.message || 'Nu s-a putut salva fisa de consultatie.';
                setError(message);
                toast.error(message);
                return null;
            } finally {
                setSaving(false);
            }
        },
        [appointmentId, record]
    );

    async function handleFinalize(formData) {
        // Spec §3: salvam intai fisa curenta, si doar daca a mers chemam /complete —
        // altfel medicul ar finaliza consultul pierzand ce tocmai a scris.
        const saved = await saveRecord(formData);
        if (!saved) return;

        setFinalizing(true);
        try {
            const updated = await appointmentApi.complete(appointmentId);
            // Sursa de adevar pentru completedAt e serverul, nu ceasul din browser.
            setAppointment(updated);
            toast.success('Consultatia a fost finalizata.');
        } catch (err) {
            const message =
                err.response?.status === 403
                    ? 'Nu aveti drepturi sa finalizati aceasta consultatie.'
                    : err.response?.data?.message || 'Nu s-a putut finaliza consultatia.';
            toast.error(message);
        } finally {
            setFinalizing(false);
        }
    }

    async function handleExportPdf() {
        setExporting(true);
        try {
            const blob = await consultationRecordApi.exportPdf(appointmentId);
            const patient = appointment?.patient;
            const namePart = patient ? `${patient.lastName}-${patient.firstName}` : appointmentId;
            triggerDownload(blob, `fisa-consultatie_${namePart}.pdf`);
        } catch {
            toast.error('Exportul PDF a esuat. Incercati din nou.');
        } finally {
            setExporting(false);
        }
    }

    if (loading) {
        return (
            <div className="mx-auto max-w-3xl px-4 pb-12 text-sm text-muted-foreground">
                Se încarcă...
            </div>
        );
    }

    const patientName = appointment?.patient
        ? `${appointment.patient.lastName} ${appointment.patient.firstName}`
        : null;

    return (
        <div className="mx-auto max-w-3xl px-4 pb-12">
            <header className="mb-5 flex flex-wrap items-start justify-between gap-3">
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">Fișă de consultație</h1>
                    {patientName && (
                        <p className="mt-1 text-sm text-muted-foreground">{patientName}</p>
                    )}
                </div>
                <div className="flex gap-2">
                    {/* Exportul e o citire, deci ramane disponibil si pe fisa blocata — dar nu
                        are ce exporta daca fisa nu a fost creata inca. */}
                    <Button
                        variant="outline"
                        onClick={handleExportPdf}
                        disabled={exporting || notFound}
                        title={notFound ? 'Fișa nu a fost creată încă' : undefined}
                    >
                        {exporting ? 'Se generează...' : 'Export PDF'}
                    </Button>
                    <Button variant="outline" onClick={() => navigate(-1)}>
                        Înapoi
                    </Button>
                </div>
            </header>

            {appointment && (
                <Card className="mb-5">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1.5 text-sm">
                            <dt className="text-muted-foreground">Data</dt>
                            <dd className="font-medium">{DATE_LONG.format(startTime)}</dd>

                            <dt className="text-muted-foreground">Interval</dt>
                            <dd className="font-medium tabular-nums">
                                {hourLabel(startTime)} — {hourLabel(endTime)}
                            </dd>

                            {appointment.doctor && (
                                <>
                                    <dt className="text-muted-foreground">Medic</dt>
                                    <dd className="font-medium">{appointment.doctor.username}</dd>
                                </>
                            )}

                            {appointment.service && (
                                <>
                                    <dt className="text-muted-foreground">Serviciu</dt>
                                    <dd className="font-medium">{appointment.service.name}</dd>
                                </>
                            )}
                        </dl>

                        <span
                            className={`shrink-0 whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset ${
                                STATUS_STYLES[status] ?? STATUS_STYLES.SCHEDULED
                            }`}
                        >
                            {STATUS_LABELS[status] ?? status}
                        </span>
                    </div>
                </Card>
            )}

            {error && (
                <p className="mb-4 rounded-md bg-destructive/10 px-4 py-3 text-sm text-destructive">
                    {error}
                </p>
            )}

            {showGraceBanner && (
                <p className="mb-4 rounded-md border-l-4 border-primary bg-primary/5 px-4 py-3 text-sm">
                    Consultația e finalizată. Fișa rămâne editabilă până la ora{' '}
                    <strong className="tabular-nums">{hourLabel(graceDeadline)}</strong>.
                </p>
            )}

            {notFound && !isLocked && (
                <p className="mb-4 rounded-md bg-muted px-4 py-3 text-sm text-muted-foreground">
                    Nicio fișă existentă — completează formularul pentru a o crea.
                </p>
            )}

            <Card>
                <ConsultationRecordForm
                    record={record}
                    readOnly={isLocked}
                    readOnlyReason={readOnlyReason}
                    onSubmit={saveRecord}
                    saving={saving}
                    showFinalize={status === 'IN_PROGRESS' && canFinalize}
                    onFinalize={handleFinalize}
                    finalizing={finalizing}
                />
            </Card>
        </div>
    );
}
