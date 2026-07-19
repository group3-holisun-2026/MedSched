import { useState, useEffect } from "react";

export default function ConsultationRecordForm({ record, readOnly, onSubmit, saving }) {
    const [presentationMotive, setPresentationMotive] = useState("");
    const [anamnesis, setAnamnesis] = useState("");
    const [clinicalExam, setClinicalExam] = useState("");
    const [diagnosis, setDiagnosis] = useState("");
    const [prescription, setPrescription] = useState("");

    useEffect(() => {
        if (record) {
            setPresentationMotive(record.presentationMotive || "");
            setAnamnesis(record.anamnesis || "");
            setClinicalExam(record.clinicalExam || "");
            setDiagnosis(record.diagnosis || "");
            setPrescription(record.prescription || "");
        }
    }, [record]);

    function handleSubmit(e) {
        e.preventDefault();
        onSubmit({ presentationMotive, anamnesis, clinicalExam, diagnosis, prescription });
    }

    return (
        <form onSubmit={handleSubmit}>
            <div style={{ marginBottom: "12px" }}>
                <label>
                    Motiv:
                    <br />
                    <textarea
                        value={presentationMotive}
                        onChange={(e) => setPresentationMotive(e.target.value)}
                        disabled={readOnly}
                        rows={2}
                        style={{ width: "100%" }}
                    />
                </label>
            </div>

            <div style={{ marginBottom: "12px" }}>
                <label>
                    Anamneza:
                    <br />
                    <textarea
                        value={anamnesis}
                        onChange={(e) => setAnamnesis(e.target.value)}
                        disabled={readOnly}
                        rows={3}
                        style={{ width: "100%" }}
                    />
                </label>
            </div>

            <div style={{ marginBottom: "12px" }}>
                <label>
                    Examen clinic:
                    <br />
                    <textarea
                        value={clinicalExam}
                        onChange={(e) => setClinicalExam(e.target.value)}
                        disabled={readOnly}
                        rows={3}
                        style={{ width: "100%" }}
                    />
                </label>
            </div>

            <div style={{ marginBottom: "12px" }}>
                <label>
                    Diagnostic:
                    <br />
                    <textarea
                        value={diagnosis}
                        onChange={(e) => setDiagnosis(e.target.value)}
                        disabled={readOnly}
                        rows={2}
                        style={{ width: "100%" }}
                    />
                </label>
            </div>

            <div style={{ marginBottom: "12px" }}>
                <label>
                    Reteta:
                    <br />
                    <textarea
                        value={prescription}
                        onChange={(e) => setPrescription(e.target.value)}
                        disabled={readOnly}
                        rows={3}
                        style={{ width: "100%" }}
                    />
                </label>
            </div>

            {readOnly ? (
                <p style={{ fontStyle: "italic", color: "#666" }}>
                    Fisa este blocata (programare finalizata) — doar in citire.
                </p>
            ) : (
                <button type="submit" disabled={saving}>
                    {saving ? "Se salveaza..." : "Salveaza"}
                </button>
            )}
        </form>
    );
}