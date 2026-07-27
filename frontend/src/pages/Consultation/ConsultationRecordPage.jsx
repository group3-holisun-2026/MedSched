import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { useAuth } from '../../context/AuthContext';
import { appointmentApi } from '../../api/appointments';
import { consultationRecordApi } from '../../api/consultationRecord';
import ConsultationRecordForm from './ConsultationRecordForm';
import Button from '../../components/Button';

const GRACE_MINUTES = 30;

export default function ConsultationRecordPage() {
    const { appointmentId } = useParams();
    const navigate = useNavigate();
    const { user } = useAuth();

    const [record, setRecord] = useState(null);
    const [appointment, setAppointment] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [finalizing, setFinalizing] = useState(false);
    const [error, setError] = useState(null);
    const [notFound, setNotFound] = useState(false);

    // Recalcul la fiecare minut doar ca sa reimprospatam eticheta orei limita din banner
    // (contorul de gratie e derivat local din completedAt, fara polling dedicat).
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

    const isLocked = record?.locked === true;
    const status = appointment?.status;
    const isOwnAppointment = appointment?.doctor?.userId === user?.id;
    const canFinalize = user?.role === 'ADMIN' || (user?.role === 'DOCTOR' && isOwnAppointment);

    // Banner de gratie doar cat timp fisa chiar mai e editabila; cand `locked` devine true,
    // mesajul din formular acopera deja cazul final (nu afisam ambele).
    const showGraceBanner = status === 'COMPLETED' && !isLocked && !!appointment?.completedAt;

    const graceLimitLabel = appointment?.completedAt
        ? new Date(new Date(appointment.completedAt).getTime() + GRACE_MINUTES * 60_000)
              .toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' })
        : '';

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

    if (loading) return <div style={{ padding: '20px' }}>Se incarca...</div>;

    return (
        <div style={{ padding: '20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h1>Fisa de Consultatie</h1>
                <Button variant="outline" onClick={() => navigate(-1)}>
                    Inapoi
                </Button>
            </div>

            <p>Programare: {appointmentId}</p>

            {error && <p style={{ color: '#c0392b' }}>{error}</p>}

            {showGraceBanner && (
                <p
                    style={{
                        borderLeft: '4px solid #2f6fed',
                        background: '#eef3fd',
                        padding: '10px 12px',
                        margin: '12px 0',
                        fontSize: '0.9rem',
                    }}
                >
                    Consultatia e finalizata. Fisa ramane editabila pana la ora {graceLimitLabel}.
                </p>
            )}

            {notFound && <p>Nicio fisa existenta — completeaza formularul pentru a o crea.</p>}

            <ConsultationRecordForm
                record={record}
                readOnly={isLocked}
                onSubmit={saveRecord}
                saving={saving}
                showFinalize={status === 'IN_PROGRESS' && canFinalize}
                onFinalize={handleFinalize}
                finalizing={finalizing}
            />
        </div>
    );
}
