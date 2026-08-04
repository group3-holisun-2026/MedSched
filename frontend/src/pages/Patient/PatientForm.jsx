import { useState } from "react";
import { quickCreatePatientRequest, updatePatientRequest } from "../../api/patients";
import Button from "../../components/Button";

const FIELD = "h-10 w-full rounded-md border border-input bg-input-background px-3 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring";
const TEXTAREA = "w-full resize-y rounded-md border border-input bg-input-background px-3 py-2 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring";
const LABEL = "mb-1 block text-sm font-semibold";

export default function PatientForm({ patient, onSaved, onCancel }) {
    const isEditing = !!patient;

    const [firstName, setFirstName] = useState(patient?.firstName ?? "");
    const [lastName, setLastName] = useState(patient?.lastName ?? "");
    const [phone, setPhone] = useState(patient?.phone ?? "");
    const [cnp, setCnp] = useState(patient?.cnp ?? "");
    const [dateOfBirth, setDateOfBirth] = useState(patient?.dateOfBirth ?? "");
    const [email, setEmail] = useState(patient?.email ?? "");
    const [allergies, setAllergies] = useState(patient?.allergies ?? "");
    const [medicalHistory, setMedicalHistory] = useState(patient?.medicalHistory ?? "");

    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);

    async function handleSubmit(e) {
        e.preventDefault();
        setSaving(true);
        setError(null);

        try {
            if (isEditing) {
                const updated = await updatePatientRequest(patient.id, {
                    firstName,
                    lastName,
                    phone,
                    cnp: cnp || undefined,
                    dateOfBirth: dateOfBirth || undefined,
                    email: email || undefined,
                    allergies: allergies || undefined,
                    medicalHistory: medicalHistory || undefined,
                });
                onSaved(updated);
            } else {
                const created = await quickCreatePatientRequest({
                    firstName,
                    lastName,
                    phone,
                    email,
                });
                onSaved(created);
            }
        } catch (err) {
            setError(err.message);
        } finally {
            setSaving(false);
        }
    }

    return (
        <form
            onSubmit={handleSubmit}
            className="w-full max-w-2xl rounded-xl border border-border bg-card text-card-foreground shadow-lg"
        >
            <div className="border-b border-border px-6 py-4">
                <h2 className="text-lg font-semibold tracking-tight">
                    {isEditing ? "Completează / editează pacient" : "Adaugă pacient nou"}
                </h2>
                <p className="mt-0.5 text-sm text-muted-foreground">
                    {isEditing
                        ? "Câmpurile medicale sunt opționale, dar profilul rămâne incomplet fără ele."
                        : "Datele minime pentru o programare. Restul fișei se completează ulterior."}
                </p>
            </div>

            <div className="space-y-4 px-6 py-5">
                <div className="grid gap-4 sm:grid-cols-2">
                    <div>
                        <label htmlFor="patient-last-name" className={LABEL}>Nume</label>
                        <input
                            id="patient-last-name"
                            className={FIELD}
                            value={lastName}
                            onChange={(e) => setLastName(e.target.value)}
                            required
                        />
                    </div>

                    <div>
                        <label htmlFor="patient-first-name" className={LABEL}>Prenume</label>
                        <input
                            id="patient-first-name"
                            className={FIELD}
                            value={firstName}
                            onChange={(e) => setFirstName(e.target.value)}
                            required
                        />
                    </div>

                    <div>
                        <label htmlFor="patient-phone" className={LABEL}>Telefon</label>
                        <input
                            id="patient-phone"
                            type="tel"
                            className={FIELD}
                            value={phone}
                            onChange={(e) => setPhone(e.target.value)}
                            placeholder="07xx xxx xxx"
                            required
                        />
                    </div>

                    {/* Emailul se cere si la creare, nu doar la completarea profilului: confirmarea programarii
                        pleaca imediat ce se salveaza programarea, deci o adresa adaugata ulterior ajunge prea
                        tarziu pentru ea. Ramane optional — recepția inregistreaza si pacienti fara email. */}
                    <div>
                        <label htmlFor="patient-email" className={LABEL}>
                            Email <span className="font-normal text-muted-foreground">(opțional)</span>
                        </label>
                        <input
                            id="patient-email"
                            type="email"
                            className={FIELD}
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="pentru confirmări și reamintiri"
                        />
                    </div>
                </div>

                {isEditing && (
                    <div className="space-y-4 border-t border-border pt-4">
                        <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                            Date de identificare
                        </p>

                        <div className="grid gap-4 sm:grid-cols-2">
                            <div>
                                <label htmlFor="patient-cnp" className={LABEL}>CNP</label>
                                <input
                                    id="patient-cnp"
                                    className={`${FIELD} tabular-nums`}
                                    value={cnp}
                                    onChange={(e) => setCnp(e.target.value)}
                                    maxLength={13}
                                    inputMode="numeric"
                                    placeholder="13 cifre"
                                />
                            </div>

                            <div>
                                <label htmlFor="patient-dob" className={LABEL}>Data nașterii</label>
                                <input
                                    id="patient-dob"
                                    type="date"
                                    className={FIELD}
                                    value={dateOfBirth}
                                    onChange={(e) => setDateOfBirth(e.target.value)}
                                />
                            </div>
                        </div>

                        <p className="pt-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                            Informații medicale
                        </p>

                        <div>
                            <label htmlFor="patient-allergies" className={LABEL}>Alergii</label>
                            <textarea
                                id="patient-allergies"
                                rows={2}
                                className={TEXTAREA}
                                value={allergies}
                                onChange={(e) => setAllergies(e.target.value)}
                                placeholder="Penicilină, latex..."
                            />
                        </div>

                        <div>
                            <label htmlFor="patient-history" className={LABEL}>Istoric medical</label>
                            <textarea
                                id="patient-history"
                                rows={3}
                                className={TEXTAREA}
                                value={medicalHistory}
                                onChange={(e) => setMedicalHistory(e.target.value)}
                                placeholder="Afecțiuni cronice, intervenții anterioare..."
                            />
                        </div>
                    </div>
                )}

                {error && (
                    <p className="rounded-md bg-destructive/10 px-4 py-3 text-sm text-destructive">
                        {error}
                    </p>
                )}
            </div>

            <div className="flex justify-end gap-2 border-t border-border px-6 py-4">
                <Button type="button" variant="outline" onClick={onCancel} disabled={saving}>
                    Anulează
                </Button>
                <Button type="submit" disabled={saving}>
                    {saving ? "Se salvează..." : "Salvează"}
                </Button>
            </div>
        </form>
    );
}
