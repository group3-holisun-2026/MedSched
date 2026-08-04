import { useState } from "react";

// Verbe, nu substantive: randul se citeste "Ana Popescu a creat Appointment".
const ACTION_LABELS = {
    CREATE: "A creat",
    READ: "A consultat",
    UPDATE: "A modificat",
    DELETE: "A șters",
};

// Semantica obisnuita: verde = a aparut ceva, rosu = a disparut ceva, chihlimbar = s-a schimbat,
// gri = doar s-a uitat. READ e cel mai frecvent si trebuie sa fie cel mai discret, altfel
// jurnalul devine un zid de culoare in care nu se mai vad modificarile reale.
const ACTION_STYLES = {
    CREATE: "bg-emerald-50 text-emerald-700 ring-emerald-600/20",
    READ: "bg-slate-100 text-slate-600 ring-slate-500/20",
    UPDATE: "bg-amber-50 text-amber-700 ring-amber-600/20",
    DELETE: "bg-red-50 text-red-700 ring-red-600/20",
};

const ROLE_LABELS = {
    ADMIN: "Administrator",
    DOCTOR: "Medic",
    RECEPTION: "Recepție",
};

const DATE_TIME = new Intl.DateTimeFormat("ro-RO", {
    day: "2-digit", month: "2-digit", year: "numeric",
    hour: "2-digit", minute: "2-digit", second: "2-digit",
});

/** "acum 3 ore" e mai usor de raportat la prezent decat un timestamp absolut. */
function relativeTime(date) {
    const seconds = Math.round((Date.now() - date.getTime()) / 1000);
    if (seconds < 60) return "chiar acum";
    const minutes = Math.round(seconds / 60);
    if (minutes < 60) return `acum ${minutes} min`;
    const hours = Math.round(minutes / 60);
    if (hours < 24) return `acum ${hours} ${hours === 1 ? "oră" : "ore"}`;
    const days = Math.round(hours / 24);
    if (days < 30) return `acum ${days} ${days === 1 ? "zi" : "zile"}`;
    return "";
}

function ActionBadge({ action }) {
    const style = ACTION_STYLES[action] ?? ACTION_STYLES.READ;
    return (
        <span className={`inline-flex whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset ${style}`}>
            {ACTION_LABELS[action] ?? action}
        </span>
    );
}

/**
 * UUID-ul entitatii ramane necesar (e singura legatura cu inregistrarea atinsa), dar ocupa
 * jumatate de tabel. Il aratam scurtat, cu valoarea intreaga in tooltip si un buton de copiere —
 * pe UUID-uri, "copiaza" e practic singura operatie pe care o face cineva oricum.
 */
function EntityId({ value }) {
    const [copied, setCopied] = useState(false);

    if (!value) return <span className="text-muted-foreground">—</span>;

    const copy = async () => {
        try {
            await navigator.clipboard.writeText(value);
            setCopied(true);
            setTimeout(() => setCopied(false), 1500);
        } catch {
            // Clipboard-ul e blocat (http fara localhost, permisiune refuzata): tooltip-ul cu
            // valoarea completa ramane, deci userul poate selecta manual.
        }
    };

    return (
        <button
            type="button"
            onClick={copy}
            title={`${value} (click pentru a copia)`}
            className="group inline-flex items-center gap-1.5 rounded font-mono text-xs text-muted-foreground hover:text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
        >
            <span>{value.slice(0, 8)}…</span>
            <span aria-hidden="true" className="opacity-0 transition-opacity group-hover:opacity-100">
                {copied ? "✓" : "⧉"}
            </span>
            <span className="sr-only">{copied ? "Copiat" : "Copiază identificatorul"}</span>
        </button>
    );
}

export default function AuditLogTable({ logs, usersById }) {
    if (logs.length === 0) return null;

    return (
        <div className="overflow-x-auto rounded-xl border border-border">
            <table className="w-full border-collapse text-sm">
                <thead>
                    <tr className="bg-muted/50 text-left">
                        <th className="whitespace-nowrap px-4 py-3 font-semibold">Data / Ora</th>
                        <th className="whitespace-nowrap px-4 py-3 font-semibold">Utilizator</th>
                        <th className="whitespace-nowrap px-4 py-3 font-semibold">Acțiune</th>
                        <th className="whitespace-nowrap px-4 py-3 font-semibold">Entitate</th>
                        <th className="whitespace-nowrap px-4 py-3 font-semibold">ID entitate</th>
                    </tr>
                </thead>
                <tbody>
                    {logs.map((log) => {
                        const when = new Date(log.timestamp);
                        const relative = relativeTime(when);
                        // Un utilizator sters din baza lasa in urma intrari de audit — jurnalul e
                        // append-only tocmai ca sa nu dispara. Aratam UUID-ul ca ultima resursa.
                        const user = usersById?.get(log.userId);

                        return (
                            <tr key={log.id} className="border-t border-border transition-colors hover:bg-muted/40">
                                <td className="whitespace-nowrap px-4 py-3 align-top">
                                    <div className="tabular-nums">{DATE_TIME.format(when)}</div>
                                    {relative && (
                                        <div className="text-xs text-muted-foreground">{relative}</div>
                                    )}
                                </td>
                                <td className="px-4 py-3 align-top">
                                    {user ? (
                                        <>
                                            <div className="font-medium">{user.fullName}</div>
                                            <div className="text-xs text-muted-foreground">
                                                {ROLE_LABELS[user.role] ?? user.role} · {user.email}
                                            </div>
                                        </>
                                    ) : (
                                        <span className="font-mono text-xs text-muted-foreground" title={log.userId}>
                                            {log.userId ? `${log.userId.slice(0, 8)}… (cont șters)` : "—"}
                                        </span>
                                    )}
                                </td>
                                <td className="px-4 py-3 align-top">
                                    <ActionBadge action={log.action} />
                                </td>
                                <td className="whitespace-nowrap px-4 py-3 align-top font-medium">
                                    {log.entityName ?? "—"}
                                </td>
                                <td className="px-4 py-3 align-top">
                                    <EntityId value={log.entityId} />
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}
