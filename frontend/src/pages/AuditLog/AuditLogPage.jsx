import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { getAuditLogRequest } from "../../api/auditLog";
import { getUsersRequest } from "../../api/users";
import Card from "../../components/Card";
import Button from "../../components/Button";
import AuditLogTable from "./AuditLogTable";

const ACTIONS = ["CREATE", "READ", "UPDATE", "DELETE"];

const ACTION_LABELS = {
    CREATE: "Creare",
    READ: "Consultare",
    UPDATE: "Modificare",
    DELETE: "Ștergere",
};

const ROLE_LABELS = {
    ADMIN: "Administrator",
    DOCTOR: "Medic",
    RECEPTION: "Recepție",
};

function toInputDate(date) {
    // Deliberat local, nu toISOString(): pe fusul Romaniei, ISO-ul unei date de la miezul noptii
    // cade in ziua precedenta, iar intervalul implicit ar fi decalat cu o zi.
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${date.getFullYear()}-${month}-${day}`;
}

function daysAgo(days) {
    const date = new Date();
    date.setDate(date.getDate() - days);
    return date;
}

const PRESETS = [
    { label: "Azi", from: () => new Date(), to: () => new Date() },
    { label: "Ultimele 7 zile", from: () => daysAgo(6), to: () => new Date() },
    { label: "Ultimele 30 zile", from: () => daysAgo(29), to: () => new Date() },
];

export default function AuditLogPage() {
    const [users, setUsers] = useState([]);
    const [usersError, setUsersError] = useState(false);

    const [userId, setUserId] = useState("");
    const [userQuery, setUserQuery] = useState("");
    const [pickerOpen, setPickerOpen] = useState(false);
    const pickerRef = useRef(null);

    const [from, setFrom] = useState(toInputDate(daysAgo(6)));
    const [to, setTo] = useState(toInputDate(new Date()));

    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Filtre aplicate peste rezultatul deja incarcat — nu mai lovesc reteaua.
    const [textFilter, setTextFilter] = useState("");
    const [activeActions, setActiveActions] = useState(() => new Set(ACTIONS));

    const usersById = useMemo(
        () => new Map(users.map((u) => [u.id, u])),
        [users]
    );

    useEffect(() => {
        let cancelled = false;
        getUsersRequest()
            .then((data) => { if (!cancelled) setUsers(data); })
            // Lista de utilizatori e un ajutor de navigare, nu o dependenta dura: daca pica,
            // jurnalul ramane utilizabil, doar ca pe UUID-uri.
            .catch(() => { if (!cancelled) setUsersError(true); });
        return () => { cancelled = true; };
    }, []);

    const search = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await getAuditLogRequest({
                userId: userId || undefined,
                from: `${from}T00:00:00`,
                to: `${to}T23:59:59`,
            });
            setLogs(data);
        } catch (err) {
            setError(err.message);
            setLogs([]);
        } finally {
            setLoading(false);
        }
    }, [userId, from, to]);

    // Cautarea porneste singura la montare si dupa fiecare schimbare de filtru de server.
    // Vechiul ecran cerea un UUID inainte de a arata orice, deci se deschidea mereu gol.
    useEffect(() => { search(); }, [search]);

    useEffect(() => {
        if (!pickerOpen) return undefined;
        const onClickOutside = (e) => {
            if (!pickerRef.current?.contains(e.target)) setPickerOpen(false);
        };
        const onEscape = (e) => { if (e.key === "Escape") setPickerOpen(false); };
        document.addEventListener("mousedown", onClickOutside);
        document.addEventListener("keydown", onEscape);
        return () => {
            document.removeEventListener("mousedown", onClickOutside);
            document.removeEventListener("keydown", onEscape);
        };
    }, [pickerOpen]);

    const selectedUser = usersById.get(userId);

    const matchingUsers = useMemo(() => {
        const needle = userQuery.trim().toLowerCase();
        if (!needle) return users;
        return users.filter((u) =>
            `${u.fullName} ${u.email} ${ROLE_LABELS[u.role] ?? u.role}`
                .toLowerCase()
                .includes(needle)
        );
    }, [users, userQuery]);

    const actionCounts = useMemo(() => {
        const counts = Object.fromEntries(ACTIONS.map((a) => [a, 0]));
        for (const log of logs) {
            if (counts[log.action] !== undefined) counts[log.action] += 1;
        }
        return counts;
    }, [logs]);

    const visibleLogs = useMemo(() => {
        const needle = textFilter.trim().toLowerCase();
        return logs.filter((log) => {
            if (!activeActions.has(log.action)) return false;
            if (!needle) return true;
            const user = usersById.get(log.userId);
            // Cautarea acopera si numele persoanei, nu doar campurile brute: altfel ar trebui
            // sa stii dinainte UUID-ul, adica exact problema pe care o rezolvam.
            return [log.entityName, log.entityId, log.action, user?.fullName, user?.email]
                .filter(Boolean)
                .some((field) => String(field).toLowerCase().includes(needle));
        });
    }, [logs, activeActions, textFilter, usersById]);

    const toggleAction = (action) => {
        setActiveActions((prev) => {
            const next = new Set(prev);
            if (next.has(action)) next.delete(action);
            else next.add(action);
            return next;
        });
    };

    const applyPreset = (preset) => {
        setFrom(toInputDate(preset.from()));
        setTo(toInputDate(preset.to()));
    };

    const activePreset = PRESETS.find(
        (p) => toInputDate(p.from()) === from && toInputDate(p.to()) === to
    );

    const rangeInvalid = Boolean(from && to && from > to);

    return (
        <div className="mx-auto max-w-6xl px-4 pb-12">
            <header className="mb-5">
                <h1 className="text-2xl font-semibold tracking-tight">Jurnal de audit</h1>
                <p className="mt-1 text-sm text-muted-foreground">
                    Cine ce a făcut în sistem. Înregistrările nu pot fi modificate sau șterse.
                </p>
            </header>

            <Card className="mb-5">
                <div className="grid gap-4 md:grid-cols-2">
                    {/* UTILIZATOR — camp de cautare, nu UUID */}
                    <div ref={pickerRef} className="relative">
                        <label htmlFor="audit-user" className="mb-1 block text-sm font-semibold">
                            Utilizator
                        </label>

                        {selectedUser ? (
                            <div className="flex h-10 items-center gap-2 rounded-md border border-input bg-input-background px-3">
                                <span className="min-w-0 flex-1 truncate text-sm">
                                    <span className="font-medium">{selectedUser.fullName}</span>
                                    <span className="ml-1 text-xs text-muted-foreground">
                                        · {ROLE_LABELS[selectedUser.role] ?? selectedUser.role}
                                    </span>
                                </span>
                                <button
                                    type="button"
                                    onClick={() => { setUserId(""); setUserQuery(""); }}
                                    className="shrink-0 rounded px-1 text-sm text-muted-foreground hover:text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                                    aria-label="Șterge filtrul de utilizator"
                                >
                                    ✕
                                </button>
                            </div>
                        ) : (
                            <input
                                id="audit-user"
                                type="text"
                                role="combobox"
                                aria-expanded={pickerOpen}
                                aria-controls="audit-user-list"
                                autoComplete="off"
                                value={userQuery}
                                onChange={(e) => { setUserQuery(e.target.value); setPickerOpen(true); }}
                                onFocus={() => setPickerOpen(true)}
                                placeholder={usersError ? "Lista indisponibilă" : "Toți utilizatorii — scrie un nume"}
                                disabled={usersError}
                                className="h-10 w-full rounded-md border border-input bg-input-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring disabled:opacity-50"
                            />
                        )}

                        {pickerOpen && !selectedUser && !usersError && (
                            <ul
                                id="audit-user-list"
                                role="listbox"
                                className="absolute z-30 mt-1 max-h-64 w-full overflow-y-auto rounded-md border border-border bg-popover p-1 shadow-lg"
                            >
                                <li>
                                    <button
                                        type="button"
                                        onClick={() => { setUserId(""); setUserQuery(""); setPickerOpen(false); }}
                                        className="w-full rounded px-2 py-1.5 text-left text-sm hover:bg-accent/10"
                                    >
                                        Toți utilizatorii
                                    </button>
                                </li>
                                {matchingUsers.map((user) => (
                                    <li key={user.id}>
                                        <button
                                            type="button"
                                            onClick={() => { setUserId(user.id); setPickerOpen(false); }}
                                            className="w-full rounded px-2 py-1.5 text-left hover:bg-accent/10"
                                        >
                                            <span className="block truncate text-sm font-medium">{user.fullName}</span>
                                            <span className="block truncate text-xs text-muted-foreground">
                                                {ROLE_LABELS[user.role] ?? user.role} · {user.email}
                                            </span>
                                        </button>
                                    </li>
                                ))}
                                {matchingUsers.length === 0 && (
                                    <li className="px-2 py-1.5 text-sm text-muted-foreground">
                                        Niciun utilizator găsit.
                                    </li>
                                )}
                            </ul>
                        )}
                    </div>

                    {/* PERIOADA */}
                    <div>
                        <span className="mb-1 block text-sm font-semibold">Perioadă</span>
                        <div className="flex items-center gap-2">
                            <input
                                type="date"
                                value={from}
                                max={to || undefined}
                                onChange={(e) => setFrom(e.target.value)}
                                aria-label="De la"
                                className="h-10 w-full rounded-md border border-input bg-input-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                            />
                            <span className="text-muted-foreground">–</span>
                            <input
                                type="date"
                                value={to}
                                min={from || undefined}
                                onChange={(e) => setTo(e.target.value)}
                                aria-label="Până la"
                                className="h-10 w-full rounded-md border border-input bg-input-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                            />
                        </div>
                        <div className="mt-2 flex flex-wrap gap-1.5">
                            {PRESETS.map((preset) => (
                                <button
                                    key={preset.label}
                                    type="button"
                                    onClick={() => applyPreset(preset)}
                                    className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                                        activePreset?.label === preset.label
                                            ? "bg-primary text-primary-foreground"
                                            : "border border-input hover:bg-accent/10"
                                    }`}
                                >
                                    {preset.label}
                                </button>
                            ))}
                        </div>
                    </div>
                </div>

                {rangeInvalid && (
                    <p className="mt-3 text-sm text-destructive">
                        Data de început trebuie să fie înaintea celei de sfârșit.
                    </p>
                )}

                <div className="mt-4 flex flex-wrap items-center gap-3 border-t border-border pt-4">
                    <input
                        type="search"
                        value={textFilter}
                        onChange={(e) => setTextFilter(e.target.value)}
                        placeholder="Caută în rezultate: nume, entitate, ID..."
                        className="h-10 min-w-0 flex-1 rounded-md border border-input bg-input-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                    />
                    <Button type="button" variant="outline" onClick={search} disabled={loading || rangeInvalid}>
                        {loading ? "Se încarcă..." : "Reîmprospătează"}
                    </Button>
                </div>

                <div className="mt-3 flex flex-wrap gap-1.5">
                    {ACTIONS.map((action) => {
                        const on = activeActions.has(action);
                        return (
                            <button
                                key={action}
                                type="button"
                                onClick={() => toggleAction(action)}
                                aria-pressed={on}
                                className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                                    on ? "bg-secondary text-secondary-foreground" : "border border-input text-muted-foreground hover:bg-accent/10"
                                }`}
                            >
                                {ACTION_LABELS[action]} ({actionCounts[action]})
                            </button>
                        );
                    })}
                </div>
            </Card>

            {error && (
                <p className="mb-4 rounded-md bg-destructive/10 px-4 py-3 text-sm text-destructive">{error}</p>
            )}

            {!error && (
                <>
                    <div className="mb-3 flex items-center justify-between text-sm text-muted-foreground">
                        <span>
                            {loading
                                ? "Se încarcă..."
                                : `${visibleLogs.length} ${visibleLogs.length === 1 ? "înregistrare" : "înregistrări"}` +
                                  (visibleLogs.length !== logs.length ? ` din ${logs.length}` : "")}
                        </span>
                        {usersError && (
                            <span className="text-amber-700">
                                Lista de utilizatori nu s-a încărcat — se afișează identificatori.
                            </span>
                        )}
                    </div>

                    <AuditLogTable logs={visibleLogs} usersById={usersById} />

                    {!loading && visibleLogs.length === 0 && (
                        <div className="rounded-xl border border-dashed border-border px-6 py-12 text-center">
                            <p className="font-medium">Nicio înregistrare</p>
                            <p className="mt-1 text-sm text-muted-foreground">
                                {logs.length > 0
                                    ? "Niciun rezultat pentru căutarea sau acțiunile selectate."
                                    : "Nicio activitate înregistrată în perioada aleasă."}
                            </p>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}
