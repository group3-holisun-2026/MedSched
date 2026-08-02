import { useState, useEffect, useRef } from "react";
import { useAuth } from "../../context/AuthContext";
import { doctorApi } from "../../api/doctors";
import { roomApi } from "../../api/rooms";
import { getDoctorColor } from "./doctorColors";

const ALLOWED_ROLES = ["ADMIN", "RECEPTION"];

const STORAGE_KEY = "medsched.calendar.unselectedDoctorIds";

const DEBOUNCE_MS = 400;

export default function DoctorFilterMenu({ onFilterChange, className = "" }) {
  const { user } = useAuth();
  const canSeeFilter = ALLOWED_ROLES.includes(user?.role);

  const [isOpen, setIsOpen] = useState(false);
  const [doctors, setDoctors] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedIds, setSelectedIds] = useState(() => new Set());
  const [selectedRoomId, setSelectedRoomId] = useState("");

  const panelRef = useRef(null);
  const buttonRef = useRef(null);
  const debounceRef = useRef(null);
  const isFirstNotify = useRef(true);

  useEffect(() => {
    if (!canSeeFilter) return;

    let cancelled = false;

    async function load() {
      setLoading(true);
      setError("");
      try {
        const [doctorsData, roomsData] = await Promise.all([
          doctorApi.getAll(),

          roomApi.getAll().catch(() => []),
        ]);
        if (cancelled) return;

        const activeDoctors = (doctorsData || []).filter(
          (d) => d.active !== false,
        );
        if (
          import.meta.env.DEV &&
          doctorsData?.length > 0 &&
          doctorsData.every((d) => d.active === undefined)
        ) {
          console.warn(
            '[DoctorFilterMenu] /api/doctors nu întoarce câmpul "active" — filtrul "doar medici activi" e temporar un no-op (arată toți medicii).',
          );
        }

        const activeRooms = (roomsData || []).filter((r) => r.active !== false);

        setDoctors(activeDoctors);
        setRooms(activeRooms);

        let previouslyUnchecked = new Set();
        try {
          const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
          if (Array.isArray(saved)) previouslyUnchecked = new Set(saved);
        } catch {}
        const initialIds = activeDoctors
          .map((d) => d.id)
          .filter((id) => !previouslyUnchecked.has(id));
        setSelectedIds(new Set(initialIds));
      } catch (err) {
        if (!cancelled) {
          console.error(err);
          setError("Nu s-au putut încărca medicii.");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [canSeeFilter]);

  useEffect(() => {
    if (loading || doctors.length === 0) return;
    try {
      const unchecked = doctors
        .map((d) => d.id)
        .filter((id) => !selectedIds.has(id));
      localStorage.setItem(STORAGE_KEY, JSON.stringify(unchecked));
    } catch {}
  }, [selectedIds, doctors, loading]);

  useEffect(() => {
    if (!canSeeFilter || loading) return;

    const doctorIds = Array.from(selectedIds);
    const notify = () =>
      onFilterChange?.({ doctorIds, roomId: selectedRoomId || null });

    if (isFirstNotify.current) {
      isFirstNotify.current = false;
      notify();
      return;
    }

    debounceRef.current = setTimeout(notify, DEBOUNCE_MS);
    return () => clearTimeout(debounceRef.current);
  }, [selectedIds, selectedRoomId, loading, canSeeFilter, onFilterChange]);

  useEffect(() => {
    if (!isOpen) return;

    function handleClickOutside(e) {
      if (
        panelRef.current?.contains(e.target) ||
        buttonRef.current?.contains(e.target)
      )
        return;
      setIsOpen(false);
    }
    function handleKeyDown(e) {
      if (e.key === "Escape") setIsOpen(false);
    }

    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen]);

  if (!canSeeFilter) return null;

  const toggleDoctor = (id) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const selectAll = () => setSelectedIds(new Set(doctors.map((d) => d.id)));
  const deselectAll = () => setSelectedIds(new Set());

  return (
    <div className={`relative inline-block ${className}`}>
      <button
        ref={buttonRef}
        type="button"
        onClick={() => setIsOpen((o) => !o)}
        aria-haspopup="true"
        aria-expanded={isOpen}
        aria-label="Filtru medici pe calendar"
        title="Filtru medici"
        className="inline-flex h-10 w-10 items-center justify-center rounded-md border border-input bg-transparent text-lg leading-none hover:bg-accent hover:text-accent-foreground focus:outline-none focus:ring-2 focus:ring-ring transition-colors"
      >
        <span aria-hidden="true">☰</span>
      </button>

      {isOpen && (
        <div
          ref={panelRef}
          role="dialog"
          aria-label="Selectează medicii afișați pe calendar"
          className="absolute left-0 z-40 mt-2 w-80 rounded-lg border border-border bg-popover p-4 text-popover-foreground shadow-lg"
        >
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-sm font-semibold">Medici afișați</h3>
            {!loading && !error && (
              <span className="text-xs text-muted-foreground">
                {selectedIds.size}/{doctors.length}
              </span>
            )}
          </div>

          {loading && (
            <p className="text-sm text-muted-foreground">
              Se încarcă medicii...
            </p>
          )}
          {!loading && error && (
            <p className="text-sm text-destructive">{error}</p>
          )}
          {!loading && !error && doctors.length === 0 && (
            <p className="text-sm text-muted-foreground">
              Niciun medic activ găsit.
            </p>
          )}

          {!loading && !error && doctors.length > 0 && (
            <>
              <div className="mb-2 flex items-center gap-2">
                <button
                  type="button"
                  onClick={selectAll}
                  className="text-xs font-medium text-accent hover:underline"
                >
                  Selectează tot
                </button>
                <span className="text-xs text-muted-foreground">·</span>
                <button
                  type="button"
                  onClick={deselectAll}
                  className="text-xs font-medium text-accent hover:underline"
                >
                  Deselectează tot
                </button>
              </div>

              <ul className="max-h-64 space-y-1 overflow-y-auto pr-1">
                {doctors.map((doctor) => {
                  const color = getDoctorColor(doctor.id);
                  const checked = selectedIds.has(doctor.id);
                  return (
                    <li key={doctor.id}>
                      <label className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 hover:bg-accent/10">
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={() => toggleDoctor(doctor.id)}
                          className="h-4 w-4 shrink-0 rounded border-input accent-accent"
                        />
                        <span
                          aria-hidden="true"
                          className="h-2.5 w-2.5 shrink-0 rounded-full"
                          style={{ backgroundColor: color.dot }}
                        />
                        <span className="min-w-0 flex-1 truncate text-sm">
                          {doctor.fullName}
                          <span className="ml-1 text-xs text-muted-foreground">
                            · {doctor.speciality}
                          </span>
                        </span>
                      </label>
                    </li>
                  );
                })}
              </ul>
            </>
          )}

          {}
          {rooms.length > 0 && (
            <div className="mt-3 border-t border-border pt-3">
              <label
                htmlFor="doctor-filter-room"
                className="mb-1 block text-xs font-medium text-muted-foreground"
              >
                Sau filtrează pe cabinet
              </label>
              <select
                id="doctor-filter-room"
                value={selectedRoomId}
                onChange={(e) => setSelectedRoomId(e.target.value)}
                className="flex h-9 w-full rounded-md border border-input bg-input-background px-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              >
                <option value="">
                  — niciun cabinet (folosește medicii de mai sus) —
                </option>
                {rooms.map((room) => (
                  <option key={room.id} value={room.id}>
                    {room.name}
                  </option>
                ))}
              </select>
              {selectedRoomId && (
                <p className="mt-1 text-xs text-muted-foreground">
                  Cabinetul selectat are prioritate față de medicii bifați mai
                  sus.
                </p>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
