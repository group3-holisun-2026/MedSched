import { useState } from "react";
import { useAuth } from "../../context/AuthContext";
import { getAuditLogRequest } from "../../api/auditLog";
import AuditLogTable from "./AuditLogTable";

export default function AuditLogPage() {
    const { accessToken } = useAuth();
    const [userId, setUserId] = useState("");
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [searched, setSearched] = useState(false);

    async function handleSearch(e) {
        e.preventDefault();
        setLoading(true);
        setError(null);
        setSearched(true);
        try {
            const fromIso = `${from}T00:00:00`;
            const toIso = `${to}T23:59:59`;
            const data = await getAuditLogRequest(accessToken, { userId, from: fromIso, to: toIso });
            setLogs(data);
        } catch (err) {
            setError(err.message);
            setLogs([]);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div style={{ padding: "20px" }}>
            <h1>Audit Log</h1>

            <form onSubmit={handleSearch} style={{ marginBottom: "20px" }}>
                <div style={{ marginBottom: "10px" }}>
                    <label>
                        ID Utilizator:{" "}
                        <input
                            type="text"
                            value={userId}
                            onChange={(e) => setUserId(e.target.value)}
                            placeholder="UUID utilizator"
                            required
                        />
                    </label>
                </div>
                <div style={{ marginBottom: "10px" }}>
                    <label>
                        De la:{" "}
                        <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} required />
                    </label>{" "}
                    <label>
                        Pana la:{" "}
                        <input type="date" value={to} onChange={(e) => setTo(e.target.value)} required />
                    </label>
                </div>
                <button type="submit" disabled={loading}>
                    {loading ? "Se cauta..." : "Cauta"}
                </button>
            </form>

            {error && <p style={{ color: "red" }}>{error}</p>}
            {!loading && !error && searched && <AuditLogTable logs={logs} />}
        </div>
    );
}