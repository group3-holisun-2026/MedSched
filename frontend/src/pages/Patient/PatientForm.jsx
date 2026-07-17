import { useState } from "react";
import { useAuth } from "../../context/AuthContext";
import { quickCreatePatientRequest, updatePatientRequest } from "../../api/patients";

export default function PatientForm({ patient, onSaved, onCancel }) {
    const { accessToken } = useAuth();
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
                const updated = await updatePatientRequest(accessToken, patient.id, {
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
                const created = await quickCreatePatientRequest(accessToken, { firstName, lastName, phone });
                onSaved(created);
            }
        } catch (err) {
            setError(err.message);
        } finally {
            setSaving(false);
        }
    }

    return (
        <form onSubmit={handleSubmit}>
            <h2>{isEditing ? "Completeaza / Editeaza pacient" : "Adauga pacient nou"}</h2>

            <div>
                <label>Nume</label>
                <input value={lastName} onChange={(e) => setLastName(e.target.value)} required />
            </div>

            <div>
                <label>Prenume</label>
                <input value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
            </div>

            <div>
                <label>Telefon</label>
                <input value={phone} onChange={(e) => setPhone(e.target.value)} required />
            </div>

            {isEditing && (
                <>
                    <div>
                        <label>CNP</label>
                        <input value={cnp} onChange={(e) => setCnp(e.target.value)} maxLength={13} />
                    </div>

                    <div>
                        <label>Data nasterii</label>
                        <input
                            type="date"
                            value={dateOfBirth}
                            onChange={(e) => setDateOfBirth(e.target.value)}
                        />
                    </div>

                    <div>
                        <label>Email</label>
                        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
                    </div>

                    <div>
                        <label>Alergii</label>
                        <textarea value={allergies} onChange={(e) => setAllergies(e.target.value)} />
                    </div>

                    <div>
                        <label>Istoric medical</label>
                        <textarea
                            value={medicalHistory}
                            onChange={(e) => setMedicalHistory(e.target.value)}
                        />
                    </div>
                </>
            )}

            {error && <p style={{ color: "red" }}>{error}</p>}

            <button type="submit" disabled={saving}>
                {saving ? "Se salveaza..." : "Salveaza"}
            </button>
            <button type="button" onClick={onCancel} disabled={saving}>
                Anuleaza
            </button>
        </form>
    );
}