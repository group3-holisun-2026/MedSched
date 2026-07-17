import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import { getIncompletePatientsRequest } from "../../api/patients";

export default function IncompletePatientsWidget({ onCompletePatient }) {
    const { accessToken } = useAuth();
    const [page, setPage] = useState(null);
    const [search, setSearch] = useState("");
    const [sort, setSort] = useState("createdAt,desc");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        let cancelled = false;

        async function fetchIncomplete() {
            setLoading(true);
            setError(null);
            try {
                const data = await getIncompletePatientsRequest(accessToken, { search, sort });
                if (!cancelled) setPage(data);
            } catch (err) {
                if (!cancelled) setError(err.message);
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        fetchIncomplete();

        return () => {
            cancelled = true;
        };
    }, [accessToken, search, sort]);

    return (
        <div style={{ border: "1px solid #ccc", padding: "15px", marginBottom: "20px" }}>
            <h2>Pacienti de completat</h2>

            <input
                type="text"
                placeholder="Cauta dupa nume..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
            />

            <select value={sort} onChange={(e) => setSort(e.target.value)}>
                <option value="createdAt,desc">Cei mai recenti</option>
                <option value="createdAt,asc">Cei mai vechi</option>
                <option value="lastName,asc">Nume A-Z</option>
                <option value="lastName,desc">Nume Z-A</option>
            </select>

            {loading && <p>Se incarca...</p>}
            {error && <p style={{ color: "red" }}>{error}</p>}

            {!loading && !error && page && (
                <>
                    {page.content.length === 0 ? (
                        <p>Niciun pacient cu profil incomplet.</p>
                    ) : (
                        <ul>
                            {page.content.map((patient) => (
                                <li key={patient.id}>
                                    {patient.lastName} {patient.firstName} — {patient.phone}{" "}
                                    <button onClick={() => onCompletePatient(patient)}>
                                        Completeaza
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}
                    <p>
                        Pagina {page.number + 1} din {page.totalPages} ({page.totalElements} total)
                    </p>
                </>
            )}
        </div>
    );
}