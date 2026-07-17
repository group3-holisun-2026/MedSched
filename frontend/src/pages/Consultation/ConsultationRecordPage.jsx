import { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import {
    getConsultationRecordRequest,
    createConsultationRecordRequest,
    updateConsultationRecordRequest,
} from "../../api/consultationRecord";
import ConsultationRecordForm from "./ConsultationRecordForm";

export default function ConsultationRecordPage() {
    const { appointmentId } = useParams();
    const { accessToken } = useAuth();
    const [record, setRecord] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);
    const [notFound, setNotFound] = useState(false);

    useEffect(() => {
        let cancelled = false;

        async function fetchRecord() {
            setLoading(true);
            setError(null);
            setNotFound(false);
            try {
                const data = await getConsultationRecordRequest(accessToken, appointmentId);
                if (!cancelled) setRecord(data);
            } catch (err) {
                // Presupunem ca o eroare aici (ex. 404) inseamna ca fisa nu a fost creata inca
                if (!cancelled) setNotFound(true);
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        fetchRecord();

        return () => {
            cancelled = true;
        };
    }, [accessToken, appointmentId]);

    async function handleSubmit(formData) {
        setSaving(true);
        setError(null);
        try {
            const saved = record
                ? await updateConsultationRecordRequest(accessToken, appointmentId, formData)
                : await createConsultationRecordRequest(accessToken, appointmentId, formData);
            setRecord(saved);
            setNotFound(false);
        } catch (err) {
            setError(err.message);
        } finally {
            setSaving(false);
        }
    }

    return (
        <div style={{ padding: "20px" }}>
            <h1>Fisa de Consultatie</h1>
            <p>Programare: {appointmentId}</p>

            {loading && <p>Se incarca...</p>}
            {error && <p style={{ color: "red" }}>{error}</p>}

            {!loading && (
                <>
                    {notFound && <p>Nicio fisa existenta — completeaza formularul pentru a o crea.</p>}
                    <ConsultationRecordForm
                        record={record}
                        readOnly={record?.locked || false}
                        onSubmit={handleSubmit}
                        saving={saving}
                    />
                </>
            )}
        </div>
    );
}