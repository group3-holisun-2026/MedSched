import { useState, useEffect } from "react";
import Button from "../../components/Button";

const TEXTAREA = "w-full resize-y rounded-md border border-input bg-input-background px-3 py-2 text-sm leading-relaxed placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring disabled:cursor-not-allowed disabled:opacity-70";

// Ordinea e cea a consultatiei reale — de la ce a adus pacientul aici pana la ce pleaca cu el.
const FIELDS = [
    {
        key: "presentationMotive",
        label: "Motivul prezentării",
        rows: 2,
        placeholder: "Ce l-a adus pe pacient astăzi",
    },
    {
        key: "anamnesis",
        label: "Anamneză",
        rows: 4,
        placeholder: "Istoric, debut, evoluție, tratamente anterioare",
    },
    {
        key: "clinicalExam",
        label: "Examen clinic",
        rows: 4,
        placeholder: "Constante, examen obiectiv pe aparate și sisteme",
    },
    {
        key: "diagnosis",
        label: "Diagnostic",
        rows: 2,
        placeholder: "Diagnostic principal și secundare",
    },
    {
        key: "prescription",
        label: "Recomandări / rețetă",
        rows: 4,
        placeholder: "Tratament, doze, durata, data reevaluării",
    },
];

export default function ConsultationRecordForm({
    record,
    readOnly,
    readOnlyReason,
    onSubmit,
    saving,
    showFinalize = false,
    onFinalize,
    finalizing = false,
}) {
    const [values, setValues] = useState({
        presentationMotive: "",
        anamnesis: "",
        clinicalExam: "",
        diagnosis: "",
        prescription: "",
    });

    useEffect(() => {
        if (record) {
            setValues({
                presentationMotive: record.presentationMotive || "",
                anamnesis: record.anamnesis || "",
                clinicalExam: record.clinicalExam || "",
                diagnosis: record.diagnosis || "",
                prescription: record.prescription || "",
            });
        }
    }, [record]);

    function handleSubmit(e) {
        e.preventDefault();
        onSubmit(values);
    }

    return (
        <form onSubmit={handleSubmit}>
            <div className="space-y-4">
                {FIELDS.map((field) => (
                    <div key={field.key}>
                        <label
                            htmlFor={`record-${field.key}`}
                            className="mb-1 block text-sm font-semibold"
                        >
                            {field.label}
                        </label>
                        <textarea
                            id={`record-${field.key}`}
                            className={TEXTAREA}
                            rows={field.rows}
                            value={values[field.key]}
                            onChange={(e) =>
                                setValues((prev) => ({ ...prev, [field.key]: e.target.value }))
                            }
                            disabled={readOnly}
                            // Placeholder-ul nu are ce cauta pe un camp blocat: acolo golul e un fapt
                            // consemnat ("nu s-a scris nimic"), nu o invitatie de completare.
                            placeholder={readOnly ? "" : field.placeholder}
                        />
                    </div>
                ))}
            </div>

            {readOnly ? (
                <p className="mt-5 rounded-md bg-muted px-4 py-3 text-sm text-muted-foreground">
                    {readOnlyReason ?? "Fișa este blocată — doar în citire."}
                </p>
            ) : (
                <div className="mt-5 flex flex-wrap gap-2 border-t border-border pt-4">
                    <Button type="submit" disabled={saving || finalizing}>
                        {saving ? "Se salvează..." : "Salvează"}
                    </Button>

                    {/* Finalizarea traieste tot aici ca sa poata salva intai valorile curente
                        din formular, apoi sa cheme PATCH /complete (F-402). */}
                    {showFinalize && (
                        <Button
                            type="button"
                            variant="secondary"
                            disabled={saving || finalizing}
                            onClick={() => onFinalize(values)}
                        >
                            {finalizing ? "Se finalizează..." : "Finalizează consultația"}
                        </Button>
                    )}
                </div>
            )}
        </form>
    );
}
