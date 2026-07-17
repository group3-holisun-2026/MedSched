export default function PatientTable({ patients, onSelectPatient }) {
    if (!patients || patients.length === 0) {
        return <p>Niciun pacient gasit.</p>;
    }

    return (
        <table>
            <thead>
            <tr>
                <th>Nume</th>
                <th>Prenume</th>
                <th>Telefon</th>
                <th>Status profil</th>
                <th></th>
            </tr>
            </thead>
            <tbody>
            {patients.map((patient) => (
                <tr key={patient.id}>
                    <td>{patient.lastName}</td>
                    <td>{patient.firstName}</td>
                    <td>{patient.phone}</td>
                    <td>
                        {patient.profileComplete ? (
                            <span style={{ color: "green" }}>Complet</span>
                        ) : (
                            <span style={{ color: "orange" }}>Incomplet</span>
                        )}
                    </td>
                    <td>
                        <button onClick={() => onSelectPatient(patient)}>
                            {patient.profileComplete ? "Editeaza" : "Completeaza"}
                        </button>
                    </td>
                </tr>
            ))}
            </tbody>
        </table>
    );
}