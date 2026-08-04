// Avatarul e doar initialele pe fundal colorat. Culoarea vine dintr-un hash stabil al numelui,
// nu din pozitia in lista: acelasi pacient are aceeasi pastila si dupa o cautare care reordoneaza.
const AVATAR_STYLES = [
    "bg-sky-100 text-sky-700",
    "bg-emerald-100 text-emerald-700",
    "bg-violet-100 text-violet-700",
    "bg-amber-100 text-amber-700",
    "bg-rose-100 text-rose-700",
    "bg-teal-100 text-teal-700",
];

function avatarStyle(seed) {
    let hash = 0;
    for (let i = 0; i < seed.length; i += 1) {
        hash = (hash * 31 + seed.charCodeAt(i)) | 0;
    }
    return AVATAR_STYLES[Math.abs(hash) % AVATAR_STYLES.length];
}

function initials(firstName = "", lastName = "") {
    return `${lastName.charAt(0)}${firstName.charAt(0)}`.toUpperCase() || "?";
}

/** Numerele de telefon se citesc in grupuri, nu ca un sir de 10 cifre. */
function formatPhone(phone) {
    if (!phone) return "—";
    const digits = phone.replace(/\s+/g, "");
    if (/^07\d{8}$/.test(digits)) {
        return `${digits.slice(0, 4)} ${digits.slice(4, 7)} ${digits.slice(7)}`;
    }
    return phone;
}

function Avatar({ patient }) {
    return (
        <span
            aria-hidden="true"
            className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${avatarStyle(patient.id ?? patient.lastName)}`}
        >
            {initials(patient.firstName, patient.lastName)}
        </span>
    );
}

function ProfileBadge({ complete }) {
    return complete ? (
        <span className="inline-flex whitespace-nowrap items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700 ring-1 ring-inset ring-emerald-600/20">
            Complet
        </span>
    ) : (
        <span className="inline-flex whitespace-nowrap items-center gap-1 rounded-full bg-amber-50 px-2.5 py-0.5 text-xs font-semibold text-amber-700 ring-1 ring-inset ring-amber-600/20">
            Incomplet
        </span>
    );
}

export default function PatientTable({ patients, onSelectPatient, patientsWithRecords, onViewRecords }) {
    if (!patients || patients.length === 0) return null;

    return (
        <div className="overflow-x-auto rounded-xl border border-border">
            <table className="w-full border-collapse text-sm">
                <thead>
                    <tr className="bg-muted/50 text-left">
                        <th className="whitespace-nowrap px-4 py-3 font-semibold">Pacient</th>
                        <th className="whitespace-nowrap px-4 py-3 font-semibold">Telefon</th>
                        <th className="whitespace-nowrap px-4 py-3 font-semibold">Profil</th>
                        <th className="px-4 py-3">
                            <span className="sr-only">Acțiuni</span>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {patients.map((patient) => (
                        <tr
                            key={patient.id}
                            className="border-t border-border transition-colors hover:bg-muted/40"
                        >
                            <td className="px-4 py-3">
                                <div className="flex items-center gap-3">
                                    <Avatar patient={patient} />
                                    <div className="min-w-0">
                                        <div className="truncate font-medium">
                                            {patient.lastName} {patient.firstName}
                                        </div>
                                        <div className="truncate text-xs text-muted-foreground">
                                            {patient.email || "fără email"}
                                        </div>
                                    </div>
                                </div>
                            </td>
                            <td className="whitespace-nowrap px-4 py-3 tabular-nums">
                                {formatPhone(patient.phone)}
                            </td>
                            <td className="px-4 py-3">
                                <ProfileBadge complete={patient.profileComplete} />
                            </td>
                            <td className="px-4 py-3 text-right">
                                <div className="flex justify-end gap-2">
                                    {/* Butonul apare doar unde chiar exista istoric clinic: altfel
                                        fiecare rand ar promite un ecran gol. Setul lipseste cu totul
                                        pentru RECEPTION, care nu are acces la fise. */}
                                    {patientsWithRecords?.has(patient.id) && (
                                        <button
                                            type="button"
                                            onClick={() => onViewRecords(patient)}
                                            className="inline-flex whitespace-nowrap items-center rounded-md border border-input px-3 py-1.5 text-xs font-medium transition-colors hover:bg-accent hover:text-accent-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                                        >
                                            Fișe
                                        </button>
                                    )}
                                    <button
                                        type="button"
                                        onClick={() => onSelectPatient(patient)}
                                        className="inline-flex whitespace-nowrap items-center rounded-md border border-input px-3 py-1.5 text-xs font-medium transition-colors hover:bg-accent hover:text-accent-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                                    >
                                        {patient.profileComplete ? "Editează" : "Completează"}
                                    </button>
                                </div>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
