export default function AuditLogTable({ logs }) {
    if (logs.length === 0) {
        return <p>Nicio inregistrare gasita pentru filtrele selectate.</p>;
    }

    return (
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
            <tr>
                <th style={{ textAlign: "left", borderBottom: "1px solid #ccc" }}>Data/Ora</th>
                <th style={{ textAlign: "left", borderBottom: "1px solid #ccc" }}>Utilizator</th>
                <th style={{ textAlign: "left", borderBottom: "1px solid #ccc" }}>Actiune</th>
                <th style={{ textAlign: "left", borderBottom: "1px solid #ccc" }}>Entitate</th>
                <th style={{ textAlign: "left", borderBottom: "1px solid #ccc" }}>ID Entitate</th>
            </tr>
            </thead>
            <tbody>
            {logs.map((log) => (
                <tr key={log.id}>
                    <td>{new Date(log.timestamp).toLocaleString("ro-RO")}</td>
                    <td>{log.userId}</td>
                    <td>{log.action}</td>
                    <td>{log.entityName}</td>
                    <td>{log.entityId}</td>
                </tr>
            ))}
            </tbody>
        </table>
    );
}