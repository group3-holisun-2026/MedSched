import { useState, useEffect } from "react";

export default function ConsultationRecordForm({
    record,
    readOnly,
    onSubmit,
    saving,
    showFinalize = false,
    onFinalize,
    finalizing = false,
}) {
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

    function currentValues() {
        return { presentationMotive, anamnesis, clinicalExam, diagnosis, prescription };
    }

    function handleSubmit(e) {
        e.preventDefault();
        onSubmit(currentValues());
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
                <div style={{ display: "flex", gap: "8px" }}>
                    <button type="submit" disabled={saving || finalizing}>
                        {saving ? "Se salveaza..." : "Salveaza"}
                    </button>

                    {/* Finalizarea traieste tot aici ca sa poata salva intai valorile curente
                        din formular, apoi sa cheme PATCH /complete (F-402). */}
                    {showFinalize && (
                        <button
                            type="button"
                            disabled={saving || finalizing}
                            onClick={() => onFinalize(currentValues())}
                        >
                            {finalizing ? "Se finalizeaza..." : "Finalizeaza consultatia"}
                        </button>
                    )}
                </div>
            )}
        </form>
    );
}