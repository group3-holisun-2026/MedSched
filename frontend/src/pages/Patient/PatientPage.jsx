import { useState, useEffect, useMemo } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { getPatientsRequest, getPatientsWithRecordsRequest } from "../../api/patients";
import Card from "../../components/Card";
import Button from "../../components/Button";
import PatientTable from "./PatientTable";
import PatientForm from "./PatientForm";

const FILTERS = [
    { key: "all", label: "Toți" },
    { key: "complete", label: "Profil complet" },
    { key: "incomplete", label: "De completat" },
];

function SkeletonRows() {
    return (
        <div className="overflow-hidden rounded-xl border border-border">
            {Array.from({ length: 5 }).map((_, i) => (
                <div
                    key={i}
                    className="flex items-center gap-3 border-b border-border px-4 py-4 last:border-b-0"
                >
                    <div className="h-9 w-9 shrink-0 animate-pulse rounded-full bg-muted" />
                    <div className="flex-1 space-y-2">
                        <div className="h-3 w-40 animate-pulse rounded bg-muted" />
                        <div className="h-2.5 w-56 animate-pulse rounded bg-muted" />
                    </div>
                    <div className="h-6 w-20 animate-pulse rounded-full bg-muted" />
                </div>
            ))}
        </div>
    );
}

export default function PatientPage() {
    const location = useLocation();
    const navigate = useNavigate();
    const [patients, setPatients] = useState([]);
    const [search, setSearch] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [filter, setFilter] = useState("all");

    // Set gol = niciun buton de fise. Asa arata si pentru RECEPTION, unde endpoint-ul da 403:
    // lipsa accesului la continut clinic nu e o eroare de afisat, e pur si simplu mai putin ecran.
    const [patientsWithRecords, setPatientsWithRecords] = useState(() => new Set());

    useEffect(() => {
        let cancelled = false;
        getPatientsWithRecordsRequest()
            .then((ids) => { if (!cancelled) setPatientsWithRecords(new Set(ids)); })
            .catch(() => { if (!cancelled) setPatientsWithRecords(new Set()); });
        return () => { cancelled = true; };
    }, []);

    // null = formularul e ascuns; {} = formular de creare rapida; {patient obj} = formular de editare
    const [editingPatient, setEditingPatient] = useState(null);
    const [showForm, setShowForm] = useState(false);

    // Cautarea loveste serverul, deci nu la fiecare tasta: altfel un nume de 8 litere inseamna
    // 8 cereri din care conteaza doar ultima.
    const [debouncedSearch, setDebouncedSearch] = useState("");
    useEffect(() => {
        const timer = setTimeout(() => setDebouncedSearch(search), 300);
        return () => clearTimeout(timer);
    }, [search]);

    useEffect(() => {
        let cancelled = false;

        async function fetchPatients() {
            setLoading(true);
            setError(null);
            try {
                const data = await getPatientsRequest(debouncedSearch);
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
    }, [debouncedSearch]);

    // Daca s-a navigat aici cu un pacient de completat in state, deschidem formularul automat.
    // Singurul apelant era widget-ul de pe Dashboard, pagina care a fost stearsa; mecanismul
    // ramane, e punctul de intrare daca IncompletePatientsWidget se remonteaza altundeva.
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

    // Escape inchide formularul, ca la orice dialog.
    useEffect(() => {
        if (!showForm) return undefined;
        const onEscape = (e) => { if (e.key === "Escape") handleCancel(); };
        document.addEventListener("keydown", onEscape);
        return () => document.removeEventListener("keydown", onEscape);
    }, [showForm]);

    const counts = useMemo(() => {
        const complete = patients.filter((p) => p.profileComplete).length;
        return { all: patients.length, complete, incomplete: patients.length - complete };
    }, [patients]);

    const visiblePatients = useMemo(() => {
        if (filter === "complete") return patients.filter((p) => p.profileComplete);
        if (filter === "incomplete") return patients.filter((p) => !p.profileComplete);
        return patients;
    }, [patients, filter]);

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
        <div className="mx-auto max-w-6xl px-4 pb-12">
            <header className="mb-5 flex flex-wrap items-start justify-between gap-3">
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">Pacienți</h1>
                    <p className="mt-1 text-sm text-muted-foreground">
                        Fișele pacienților înregistrați. Profilurile incomplete pot fi completate oricând.
                    </p>
                </div>
                <Button type="button" onClick={handleAddNew}>
                    + Adaugă pacient
                </Button>
            </header>

            <Card className="mb-5">
                <div className="flex flex-wrap items-center gap-3">
                    <div className="relative min-w-0 flex-1">
                        <span
                            aria-hidden="true"
                            className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground"
                        >
                            ⌕
                        </span>
                        <input
                            type="search"
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            placeholder="Caută după nume, telefon sau email..."
                            aria-label="Caută pacient"
                            className="h-10 w-full rounded-md border border-input bg-input-background pl-8 pr-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                        />
                    </div>

                    <div className="flex flex-wrap gap-1.5">
                        {FILTERS.map((f) => (
                            <button
                                key={f.key}
                                type="button"
                                onClick={() => setFilter(f.key)}
                                aria-pressed={filter === f.key}
                                className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                                    filter === f.key
                                        ? "bg-secondary text-secondary-foreground"
                                        : "border border-input text-muted-foreground hover:bg-accent/10"
                                }`}
                            >
                                {f.label} ({counts[f.key]})
                            </button>
                        ))}
                    </div>
                </div>
            </Card>

            {error && (
                <p className="mb-4 rounded-md bg-destructive/10 px-4 py-3 text-sm text-destructive">
                    {error}
                </p>
            )}

            {!error && (
                <>
                    <div className="mb-3 text-sm text-muted-foreground">
                        {loading
                            ? "Se încarcă..."
                            : `${visiblePatients.length} ${visiblePatients.length === 1 ? "pacient" : "pacienți"}` +
                              (visiblePatients.length !== patients.length ? ` din ${patients.length}` : "")}
                    </div>

                    {loading ? (
                        <SkeletonRows />
                    ) : (
                        <>
                            <PatientTable
                                patients={visiblePatients}
                                onSelectPatient={handleSelectPatient}
                                patientsWithRecords={patientsWithRecords}
                                onViewRecords={(patient) => navigate(`/patients/${patient.id}/records`)}
                            />

                            {visiblePatients.length === 0 && (
                                <div className="rounded-xl border border-dashed border-border px-6 py-12 text-center">
                                    <p className="font-medium">Niciun pacient</p>
                                    <p className="mt-1 text-sm text-muted-foreground">
                                        {patients.length > 0
                                            ? "Niciun pacient nu corespunde filtrului selectat."
                                            : search
                                              ? `Nicio potrivire pentru „${search}”.`
                                              : "Încă nu este înregistrat niciun pacient."}
                                    </p>
                                    {patients.length === 0 && !search && (
                                        <Button type="button" className="mt-4" onClick={handleAddNew}>
                                            + Adaugă primul pacient
                                        </Button>
                                    )}
                                </div>
                            )}
                        </>
                    )}
                </>
            )}

            {showForm && (
                <div
                    className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-background/80 p-4 backdrop-blur-sm sm:items-center"
                    role="dialog"
                    aria-modal="true"
                    aria-label={editingPatient ? "Editare pacient" : "Adăugare pacient"}
                    onMouseDown={(e) => { if (e.target === e.currentTarget) handleCancel(); }}
                >
                    <PatientForm
                        patient={editingPatient}
                        onSaved={handleSaved}
                        onCancel={handleCancel}
                    />
                </div>
            )}
        </div>
    );
}
