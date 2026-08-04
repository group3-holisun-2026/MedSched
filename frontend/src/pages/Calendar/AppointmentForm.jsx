import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { toast } from 'sonner';
import Button from '../../components/Button';
import Input from '../../components/Input';

// Importurile catre API-uri
import { serviceApi } from '../../api/services';
import { patientApi } from '../../api/patients';
import { doctorApi } from '../../api/doctors';
import { roomApi } from '../../api/rooms';
import { appointmentApi } from '../../api/appointments';
import {
    DAY_LABELS,
    formatSchedule,
    isoDayOfWeek,
    normalizeSchedule,
    shiftsForDay,
} from '../../components/Calendar/workingHours';

// Optiuni fixe pentru selectorul de ora, intre 08:00 si 20:00 (programul clinicii), din 15 in 15
// minute — acelasi pas ca grila de calendar. Folosim <select> in loc de <input type="time" min max>
// pentru ca browserele nu filtreaza selectorul nativ de ora dupa min/max: doar valideaza la submit,
// deci userul tot putea derula prin toate cele 24h. Cu select, orele din afara programului nu exista.
const TIME_SLOT_OPTIONS = [];
for (let h = 8; h <= 20; h++) {
    for (let m = 0; m < 60; m += 15) {
        if (h === 20 && m > 0) break;
        TIME_SLOT_OPTIONS.push(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`);
    }
}

const AppointmentForm = ({ initialData, onSave, onCancel }) => {
    const [formData, setFormData] = useState({
        patientId: '',
        doctorId: '',
        roomId: '',
        serviceId: '',
        startTime: '',
        notes: ''
    });

    const [patients, setPatients] = useState([]);
    const [doctors, setDoctors] = useState([]);
    const [rooms, setRooms] = useState([]);
    const [services, setServices] = useState([]);
    const [loading, setLoading] = useState(true);

    const [patientSearch, setPatientSearch] = useState('');
    const [showNewPatientForm, setShowNewPatientForm] = useState(false);
    const [newPatientName, setNewPatientName] = useState('');
    const [newPatientPhone, setNewPatientPhone] = useState('');
    // Fara adresa aici, confirmarea pusa in coada la salvarea programarii se naste direct FAILED —
    // pacientul creat pe loc nu avea cum sa primeasca emailul.
    const [newPatientEmail, setNewPatientEmail] = useState('');

    // Listele de selectie nu depind de programarea editata — le incarcam o singura data, la
    // montare. Inainte, acest efect depindea de `initialData`, iar parintele recreeaza acel
    // obiect la fiecare randare: polling-ul de 20s al calendarului declansa un re-fetch care
    // punea formularul in "loading" si ii golea campurile in timp ce userul completa.
    useEffect(() => {
        const fetchAllData = async () => {
            try {
                setLoading(true);
                const [patientsRes, docsRes, roomsRes, servRes] = await Promise.all([
                    patientApi.getAll().catch(() => []),
                    doctorApi.getAll().catch(() => []),
                    roomApi.getAll().catch(() => []),
                    serviceApi.getAll().catch(() => [])
                ]);

                setPatients(patientsRes);
                setDoctors(docsRes);
                setRooms(roomsRes);
                setServices(servRes);
            } catch (error) {
                toast.error("A apărut o problemă la preluarea datelor. Vă rugăm să reîncercați.");
            } finally {
                setLoading(false);
            }
        };

        fetchAllData();
    }, []);

    // Hidratarea formularului se face doar cand se schimba efectiv programarea editata.
    // Cheia e o valoare primitiva (id / ora de start), nu identitatea obiectului.
    const initialDataKey = initialData?.id ?? initialData?.startTime ?? null;

    useEffect(() => {
        if (!initialData) return;
        // initialData vine in doua forme: AppointmentResponse complet la reprogramare
        // (patient/doctor/room/service ca obiecte imbricate) sau doar { startTime } la creare
        // dintr-un slot liber. De-aia citim ambele variante — nested .id si flat *Id.
        setFormData({
            patientId: initialData.patient?.id ?? initialData.patientId ?? '',
            doctorId: initialData.doctor?.id ?? initialData.doctorId ?? '',
            roomId: initialData.room?.id ?? initialData.roomId ?? '',
            serviceId: initialData.service?.id ?? initialData.serviceId ?? '',
            startTime: initialData.startTime ? initialData.startTime.substring(0, 16) : '',
            notes: initialData.notes || ''
        });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [initialDataKey]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handlePatientSelect = (e) => {
        // Filtrul si-a facut treaba odata ce pacientul e ales; il golim ca lista sa ramana
        // completa daca receptia vrea sa schimbe selectia.
        setPatientSearch('');
        setFormData(prev => ({ ...prev, patientId: e.target.value }));
    };

    const startNewPatient = () => {
        setShowNewPatientForm(true);
        setPatientSearch('');
        setFormData(prev => ({ ...prev, patientId: '' }));
    };

    const cancelNewPatient = () => {
        setShowNewPatientForm(false);
        setNewPatientName('');
        setNewPatientPhone('');
        setNewPatientEmail('');
    };

    const clearPatientSelection = () => {
        setFormData(prev => ({ ...prev, patientId: '' }));
    };

    const selectedPatient = patients.find(p => String(p.id) === String(formData.patientId));

    // Data si ora raman tinute intern ca un singur string "YYYY-MM-DDTHH:mm" (formData.startTime),
    // dar sunt afisate ca doua controale separate: un input de data si selectorul de ora de mai sus.
    const startDatePart = formData.startTime ? formData.startTime.slice(0, 10) : '';
    const startTimePart = formData.startTime ? formData.startTime.slice(11, 16) : '';

    const handleStartDateChange = (e) => {
        const date = e.target.value;
        setFormData(prev => {
            const time = prev.startTime ? prev.startTime.slice(11, 16) : '08:00';
            return { ...prev, startTime: date ? `${date}T${time}` : '' };
        });
    };

    const handleStartTimeChange = (e) => {
        const time = e.target.value;
        setFormData(prev => {
            const date = prev.startTime ? prev.startTime.slice(0, 10) : '';
            return { ...prev, startTime: date ? `${date}T${time}` : prev.startTime };
        });
    };

    const calculateEndTime = () => {
        if (!formData.startTime || !formData.serviceId) return '';

        const service = services.find(s => s.id.toString() === formData.serviceId.toString());
        if (!service || !service.defaultDurationMinutes) return '';

        const start = new Date(formData.startTime);
        const end = new Date(start.getTime() + service.defaultDurationMinutes * 60000);

        return end.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };

    // ---------------------------------------------------- programul medicului
    // Pana acum orarul medicului exista doar in backend: receptia alegea o ora la nimereala si
    // afla ca e in afara programului abia dupa submit, dintr-un 409. Acum e vizibil in formular,
    // iar orele imposibile sunt dezactivate din start.
    const selectedDoctor = doctors.find(d => String(d.id) === String(formData.doctorId));
    const doctorSchedule = useMemo(() => normalizeSchedule(selectedDoctor), [selectedDoctor]);
    const scheduleSummary = useMemo(() => formatSchedule(doctorSchedule), [doctorSchedule]);

    const selectedDayIso = startDatePart
        ? isoDayOfWeek(new Date(`${startDatePart}T00:00:00`))
        : null;
    const shiftsOnSelectedDay = useMemo(
        () => (selectedDayIso ? shiftsForDay(doctorSchedule, selectedDayIso) : []),
        [doctorSchedule, selectedDayIso]
    );

    const selectedServiceDuration = services.find(
        s => String(s.id) === String(formData.serviceId)
    )?.defaultDurationMinutes ?? 0;

    // Programarea trebuie sa incapa INTREAGA in tura, exact ca in WorkScheduleValidator: altfel
    // un consult de 40 de minute inceput la 16:45 ar trece de filtrul din formular si ar pica
    // oricum la salvare.
    const canStartAt = useCallback((time) => {
        // Fara medic sau fara orar cunoscut nu blocam nimic — necunoscutul nu e acelasi lucru
        // cu interdictia, iar backendul ramane oricum arbitrul final.
        if (!selectedDoctor || doctorSchedule.length === 0 || !startDatePart) return true;

        const [hours, minutes] = time.split(':').map(Number);
        const start = hours * 60 + minutes;
        const end = start + selectedServiceDuration;

        return shiftsOnSelectedDay.some(
            shift => start >= shift.startMinutes && start < shift.endMinutes && end <= shift.endMinutes
        );
    }, [selectedDoctor, doctorSchedule, startDatePart, shiftsOnSelectedDay, selectedServiceDuration]);

    const showScheduleHints = Boolean(selectedDoctor) && doctorSchedule.length > 0;
    const dayIsOff = showScheduleHints && Boolean(startDatePart) && shiftsOnSelectedDay.length === 0;
    const selectedTimeIsOff =
        showScheduleHints && Boolean(startTimePart) && !canStartAt(startTimePart);

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Plasa de siguranta pentru cazul in care startTime ajunge in stare din alta sursa decat
        // selectorul (ex. initialData de la o programare veche, dinainte de limitarea programului).
        if (startTimePart && (startTimePart < '08:00' || startTimePart > '20:00')) {
            toast.error('Ora selectata trebuie sa fie intre 08:00 si 20:00.');
            return;
        }

        // Aceeasi regula ca in backend, verificata inainte de request: altfel singurul feedback
        // ar fi un 409 generic, dupa ce formularul a fost completat integral.
        if (selectedTimeIsOff) {
            toast.error(
                `${selectedDoctor.fullName} nu are program in acest interval. Program: ${scheduleSummary}`
            );
            return;
        }

        try {
            let finalPatientId = formData.patientId;

            if (showNewPatientForm) {
                const newPatient = await patientApi.create({
                    name: newPatientName,
                    phone: newPatientPhone,
                    email: newPatientEmail,
                });
                finalPatientId = newPatient.id;
            }

            const payload = {
                patientId: finalPatientId,
                doctorId: formData.doctorId,
                roomId: formData.roomId,
                serviceId: formData.serviceId,
                // Backendul primeste LocalDateTime (ora de perete, fara fus), iar calendarul
                // citeste raspunsul cu new Date(...) tot ca ora locala. formData.startTime e deja
                // "YYYY-MM-DDTHH:mm" local, deci il trimitem ca atare: un toISOString() l-ar
                // converti in UTC si programarea ar aluneca cu 2-3 ore (offsetul Romaniei).
                startTime: `${formData.startTime}:00`,
                notes: formData.notes
            };

            if (initialData?.id) {
                await appointmentApi.update(initialData.id, payload);
                toast.success("Programarea a fost actualizată cu succes.");
            } else {
                await appointmentApi.create(payload);
                toast.success("Programarea a fost înregistrată cu succes în sistem.");
            }

            onSave && onSave();
        } catch (error) {
            if (error.response?.status === 409) {
                toast.error(error.response.data?.message || "Conflict de programare: Medicul sau cabinetul selectat este indisponibil în acest interval.");
            } else {
                toast.error("Înregistrarea programării a eșuat. Vă rugăm să verificați datele și să reîncercați.");
            }
        }
    };

    const filteredPatients = useMemo(() => {
        return patients.filter(p => p.name?.toLowerCase().includes(patientSearch.toLowerCase()) || p.cnp?.includes(patientSearch));
    }, [patients, patientSearch]);

    if (loading) return <div className="p-8 text-center text-gray-500 font-medium">Se preiau informațiile din sistem...</div>;

    return (
        <form onSubmit={handleSubmit} className="space-y-4 p-2">
            {/* 1. PACIENT */}
            <div className="bg-gray-50 p-4 rounded-lg border border-gray-100">
                <label className="block text-sm font-semibold text-gray-700 mb-1">
                    Pacient <span className="text-red-500">*</span>
                </label>

                {!showNewPatientForm ? (
                    <div className="space-y-2">
                        {/* Cautarea dispare odata ce pacientul e ales — la momentul ala nu mai
                            filtreaza nimic si doar ocupa spatiu in modal. */}
                        {!selectedPatient && (
                            <input
                                type="text"
                                placeholder="Căutare după nume sau CNP..."
                                value={patientSearch}
                                onChange={(e) => setPatientSearch(e.target.value)}
                                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                            />
                        )}

                        <div className="flex gap-2">
                            <select
                                required
                                value={formData.patientId}
                                onChange={handlePatientSelect}
                                className="flex-1 min-w-0 rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                            >
                                <option value="" disabled>-- Selectați pacientul --</option>
                                {filteredPatients.map(p => (
                                    <option key={p.id} value={p.id}>{p.name} {p.cnp ? `(${p.cnp})` : ''}</option>
                                ))}
                            </select>

                            {/* Buton dedicat: inainte, "pacient nou" era ultima optiune din lista
                                derulanta, deci se ascundea sub toti pacientii existenti. */}
                            <button
                                type="button"
                                onClick={startNewPatient}
                                className="shrink-0 rounded-md border border-blue-600 px-3 py-2 text-sm font-medium text-blue-600 hover:bg-blue-50 transition-colors"
                            >
                                + Pacient nou
                            </button>
                        </div>

                        {selectedPatient && (
                            <button
                                type="button"
                                onClick={clearPatientSelection}
                                className="text-sm text-blue-600 hover:text-blue-800 hover:underline transition-colors"
                            >
                                Caută alt pacient
                            </button>
                        )}
                    </div>
                ) : (
                    <div className="space-y-2">
                        <p className="text-sm text-gray-600">Pacient nou — se înregistrează la salvarea programării.</p>
                        <input
                            type="text"
                            required
                            autoFocus
                            placeholder="Numele complet al pacientului"
                            value={newPatientName}
                            onChange={(e) => setNewPatientName(e.target.value)}
                            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                        />
                        <input
                            type="tel"
                            required
                            placeholder="Număr de telefon"
                            value={newPatientPhone}
                            onChange={(e) => setNewPatientPhone(e.target.value)}
                            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                        />
                        <input
                            type="email"
                            placeholder="Email (pentru confirmare și reamintire)"
                            value={newPatientEmail}
                            onChange={(e) => setNewPatientEmail(e.target.value)}
                            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                        />
                        <p className="text-xs text-gray-500">
                            Fără email, pacientul nu primește confirmarea și reamintirea de 24h.
                        </p>
                        <button
                            type="button"
                            onClick={cancelNewPatient}
                            className="text-sm text-blue-600 hover:text-blue-800 hover:underline transition-colors"
                        >
                            Renunță și caută un pacient existent
                        </button>
                    </div>
                )}
            </div>

            <div className="grid grid-cols-2 gap-4">
                {/* 2. MEDIC */}
                <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Medic <span className="text-red-500">*</span>
                    </label>
                    <select
                        name="doctorId"
                        value={formData.doctorId}
                        onChange={handleChange}
                        required
                        className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                    >
                        <option value="" disabled>-- Selectați medicul curant --</option>
                        {/* DoctorResponse expune `fullName` + `speciality` — nu firstName/lastName,
                            care erau mereu undefined si faceau toate optiunile identice ("Dr.  "). */}
                        {doctors.map(d => (
                            <option key={d.id} value={d.id}>
                                {d.fullName}{d.speciality ? ` — ${d.speciality}` : ''}
                            </option>
                        ))}
                    </select>

                    {selectedDoctor && (
                        showScheduleHints ? (
                            <p className="mt-1 text-xs text-gray-600">
                                <span className="font-semibold">Program:</span> {scheduleSummary}
                            </p>
                        ) : (
                            <p className="mt-1 text-xs text-amber-700">
                                Acest medic nu are ore de lucru definite — orice interval va fi
                                respins la salvare.
                            </p>
                        )
                    )}
                </div>

                {/* 3. CABINET */}
                <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Cabinet <span className="text-red-500">*</span>
                    </label>
                    <select
                        name="roomId"
                        value={formData.roomId}
                        onChange={handleChange}
                        required
                        className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                    >
                        <option value="" disabled>-- Selectați cabinetul --</option>
                        {rooms.map(r => (
                            <option key={r.id} value={r.id}>{r.name}</option>
                        ))}
                    </select>
                </div>
            </div>

            {/* 4. SERVICIU */}
            <div>
                <label className="block text-sm font-semibold text-gray-700 mb-1">
                    Serviciu <span className="text-red-500">*</span>
                </label>
                <select
                    name="serviceId"
                    value={formData.serviceId}
                    onChange={handleChange}
                    required
                    className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                >
                    <option value="" disabled>-- Selectați serviciul medical --</option>
                    {services.map(s => (
                        <option key={s.id} value={s.id}>{s.name} ({s.defaultDurationMinutes} min)</option>
                    ))}
                </select>
            </div>

            {/* 5. DATA SI ORA START */}
            <div className="grid grid-cols-2 gap-4">
                <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Dată și Oră Start <span className="text-red-500">*</span>
                    </label>
                    <div className="grid grid-cols-2 gap-2">
                        <input
                            type="date"
                            value={startDatePart}
                            onChange={handleStartDateChange}
                            required
                            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                        />
                        <select
                            value={startTimePart}
                            onChange={handleStartTimeChange}
                            required
                            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                        >
                            <option value="" disabled>-- Ora --</option>
                            {/* O programare existenta poate avea o ora care nu cade pe grila de 15
                                minute (sau e din afara programului). Fara optiunea ei proprie,
                                select-ul s-ar afisa gol si am pierde ora reala la reprogramare. */}
                            {startTimePart && !TIME_SLOT_OPTIONS.includes(startTimePart) && (
                                <option value={startTimePart}>{startTimePart} (in afara grilei)</option>
                            )}
                            {TIME_SLOT_OPTIONS.map(t => {
                                const available = canStartAt(t);
                                return (
                                    <option key={t} value={t} disabled={!available}>
                                        {t}{available ? '' : ' — în afara programului'}
                                    </option>
                                );
                            })}
                        </select>
                    </div>

                    {/* Ierarhia mesajelor: intai ziua libera (nicio ora nu ajuta), apoi ora
                        invalida, si abia la final programul clinicii. */}
                    {dayIsOff ? (
                        <p className="text-xs text-amber-700 mt-1">
                            {selectedDoctor.fullName} nu lucrează
                            {selectedDayIso ? ` ${DAY_LABELS[selectedDayIso - 1].toLowerCase()}` : ''}.
                            Program: {scheduleSummary}
                        </p>
                    ) : showScheduleHints && startDatePart ? (
                        <p className={`text-xs mt-1 ${selectedTimeIsOff ? 'text-amber-700' : 'text-gray-500'}`}>
                            {selectedDayIso ? DAY_LABELS[selectedDayIso - 1] : ''}:{' '}
                            {shiftsOnSelectedDay
                                .map(s => `${s.startLabel} - ${s.endLabel}`)
                                .join(', ')}
                            {selectedServiceDuration > 0 && ` · consultația durează ${selectedServiceDuration} min`}
                        </p>
                    ) : (
                        <p className="text-xs text-gray-400 mt-1">Program: 08:00 - 20:00</p>
                    )}
                </div>

                {/* PREVIEW ORA SFARSIT */}
                <div>
                    <label className="block text-sm font-semibold text-gray-500 mb-1">
                        Ora estimată a finalizării
                    </label>
                    <input
                        type="text"
                        readOnly
                        value={calculateEndTime()}
                        placeholder="Calculată automat de sistem"
                        className="w-full rounded-md border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-500 cursor-not-allowed select-none"
                    />
                </div>
            </div>

            {/* 6. NOTITE */}
            <div>
                <label className="block text-sm font-semibold text-gray-700 mb-1">
                    Observații suplimentare <span className="text-gray-400 font-normal">(Opțional)</span>
                </label>
                <textarea
                    name="notes"
                    value={formData.notes}
                    onChange={handleChange}
                    rows="3"
                    placeholder="Introduceți orice informații relevante pentru această programare..."
                    className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                />
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t border-gray-100 mt-6">
                {onCancel && (
                    <Button type="button" variant="outline" onClick={onCancel}>
                        Renunță
                    </Button>
                )}
                <Button type="submit" variant="primary">
                    Salvare Programare
                </Button>
            </div>
        </form>
    );
};

export default AppointmentForm;