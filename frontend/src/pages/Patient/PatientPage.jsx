import { useState, useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { getPatientsRequest } from "../../api/patients";
import PatientTable from "./PatientTable";
import PatientForm from "./PatientForm";

export default function PatientPage() {
    const { accessToken } = useAuth();
    const location = useLocation();
    const navigate = useNavigate();
    const [patients, setPatients] = useState([]);
    const [search, setSearch] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // null = formularul e ascuns; {} = formular de creare rapida; {patient obj} = formular de editare
    const [editingPatient, setEditingPatient] = useState(null);
    const [showForm, setShowForm] = useState(false);

    useEffect(() => {
        let cancelled = false;

        async function fetchPatients() {
            setLoading(true);
            setError(null);
            try {
                const data = await getPatientsRequest(accessToken, search);
                if (!cancelled) setPatients(data);
            } catch (err) {
                if (!cancelled) setError(err.message);
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        fetchPatients();

        return () => {
            cancelled = true;
        };
    }, [accessToken, search]);

    // Daca am venit din Dashboard cu un pacient de completat, deschidem formularul automat
    useEffect(() => {
        if (location.state?.editPatient) {
            setEditingPatient(location.state.editPatient);
            setShowForm(true);

            // Curatam state-ul din istoricul de navigare, ca sa nu se redeschida
            // formularul daca userul da refresh sau navigheaza inapoi/inainte
            navigate(location.pathname, { replace: true, state: {} });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [location.state]);

    function handleSelectPatient(patient) {
        setEditingPatient(patient);
        setShowForm(true);
    }

    function handleAddNew() {
        setEditingPatient(null);
        setShowForm(true);
    }

    function handleSaved(savedPatient) {
        setPatients((prev) => {
            const exists = prev.some((p) => p.id === savedPatient.id);
            if (exists) {
                return prev.map((p) => (p.id === savedPatient.id ? savedPatient : p));
            }
            return [...prev, savedPatient];
        });
        setShowForm(false);
        setEditingPatient(null);
    }

    function handleCancel() {
        setShowForm(false);
        setEditingPatient(null);
    }

    return (
        <div>
            <h1>Pacienti</h1>

            <input
                type="text"
                placeholder="Cauta dupa nume..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
            />

            <button onClick={handleAddNew}>Adauga pacient nou</button>

            {loading && <p>Se incarca...</p>}
            {error && <p style={{ color: "red" }}>{error}</p>}

            {!loading && !error && (
                <PatientTable patients={patients} onSelectPatient={handleSelectPatient} />
            )}

            {showForm && (
                <PatientForm patient={editingPatient} onSaved={handleSaved} onCancel={handleCancel} />
            )}
        </div>
    );
}