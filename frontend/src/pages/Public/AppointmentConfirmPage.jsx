import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { publicAppointmentApi } from '../../api/publicAppointment';
import format from 'date-fns/format';

const AppointmentConfirmPage = () => {
    const { token } = useParams();

    // Stări pentru date și interfață
    const [appointment, setAppointment] = useState(null);
    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState(false);

    // Stări pentru rezultate
    const [error, setError] = useState(null);
    const [networkError, setNetworkError] = useState(false);
    const [successMessage, setSuccessMessage] = useState(null);
    const [showCancelConfirm, setShowCancelConfirm] = useState(false); // Fără alert() nativ

    const fetchDetails = async () => {
        setLoading(true);
        setError(null);
        setNetworkError(false);
        try {
            const data = await publicAppointmentApi.getDetails(token);
            setAppointment(data);
        } catch (err) {
            handleError(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchDetails();
    }, [token]);

    // Maparea erorilor conform tabelului de la PM
    const handleError = (err) => {
        if (!err.response) {
            setError("Nu ne putem conecta. Încercați din nou.");
            setNetworkError(true);
            return;
        }

        const status = err.response.status;
        const clinicaTelefon = "0700 000 000"; // Aici poți pune telefonul real al clinicii

        if (status === 404) {
            setError(`Link invalid sau expirat. Contactați clinica la ${clinicaTelefon}.`);
        } else if (status === 410) {
            setError(`Programarea a trecut deja. Pentru o programare nouă, sunați la ${clinicaTelefon}.`);
        } else if (status === 409) {
            setError("Programarea a fost deja confirmată/anulată.");
            fetchDetails(); // Reîncărcăm datele proaspete de pe server
        } else {
            setError("A apărut o eroare neașteptată. Vă rugăm să ne contactați.");
        }
    };

    const handleConfirm = async () => {
        setActionLoading(true);
        try {
            await publicAppointmentApi.confirm(token);
            setSuccessMessage("Programarea dumneavoastră a fost confirmată cu succes. Vă așteptăm!");
            setAppointment(null); // Ascundem restul ecranului
        } catch (err) {
            handleError(err);
        } finally {
            setActionLoading(false);
        }
    };

    const handleCancel = async () => {
        setActionLoading(true);
        try {
            await publicAppointmentApi.cancel(token);
            setSuccessMessage("Programarea dumneavoastră a fost anulată cu succes.");
            setAppointment(null);
        } catch (err) {
            handleError(err);
        } finally {
            setActionLoading(false);
            setShowCancelConfirm(false);
        }
    };

    // STILURI INLINE pentru Layout Mobile-First (regula PM)
    const containerStyle = {
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        padding: '16px',
        backgroundColor: '#f3f4f6'
    };

    const cardStyle = {
        width: '100%',
        maxWidth: '480px', // Restricția de lățime cerută
        padding: '24px',
        backgroundColor: '#ffffff',
        borderRadius: '8px',
        boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
        textAlign: 'center'
    };

    const buttonStyle = {
        width: '100%',
        minHeight: '44px', // Minim 44px (touch-friendly)
        fontSize: '16px',  // Minim 16px (legibility)
        marginBottom: '12px',
        borderRadius: '6px',
        border: 'none',
        cursor: 'pointer',
        fontWeight: '500'
    };

    if (loading) return <div style={containerStyle}><h2>Se încarcă...</h2></div>;

    return (
        <div style={containerStyle}>
            <div style={cardStyle}>

                {/* 1. Starea de eroare */}
                {error && (
                    <div style={{ color: '#c0392b', fontSize: '16px', marginBottom: '16px' }}>
                        <p>{error}</p>
                        {networkError && (
                            <button
                                onClick={fetchDetails}
                                style={{ ...buttonStyle, backgroundColor: '#e2e8f0', color: '#333', marginTop: '12px' }}
                            >
                                Reîncearcă
                            </button>
                        )}
                    </div>
                )}

                {/* 2. Starea de succes vizibilă pe ecran */}
                {successMessage && (
                    <div style={{ color: '#2e8b57', fontSize: '18px', fontWeight: 'bold' }}>
                        {successMessage}
                    </div>
                )}

                {/* 3. Datele programării și butoanele */}
                {!error && !successMessage && appointment && (
                    <>
                        <h2 style={{ fontSize: '20px', marginBottom: '16px', color: '#1f2937' }}>
                            Bună, {appointment.patientFirstName}!
                        </h2>
                        <div style={{ fontSize: '16px', color: '#4b5563', marginBottom: '24px', lineHeight: '1.5' }}>
                            <p>Aveți o programare la <strong>{appointment.clinicName}</strong></p>
                            <p>Data: <strong>{format(new Date(appointment.date), 'dd.MM.yyyy')}</strong></p>
                            <p>Ora: <strong>{appointment.time}</strong></p>
                            <p>Medic: <strong>{appointment.doctorName}</strong></p>
                        </div>

                        {!showCancelConfirm ? (
                            <>
                                {appointment.canConfirm && (
                                    <button
                                        onClick={handleConfirm}
                                        disabled={actionLoading}
                                        style={{ ...buttonStyle, backgroundColor: '#2e8b57', color: 'white' }}
                                    >
                                        {actionLoading ? "Se procesează..." : "Confirm prezența"}
                                    </button>
                                )}
                                {appointment.canCancel && (
                                    <button
                                        onClick={() => setShowCancelConfirm(true)}
                                        disabled={actionLoading}
                                        style={{ ...buttonStyle, backgroundColor: 'transparent', color: '#c0392b', border: '1px solid #c0392b' }}
                                    >
                                        Anulez programarea
                                    </button>
                                )}
                            </>
                        ) : (
                            <div style={{ marginTop: '16px', padding: '16px', backgroundColor: '#fef2f2', borderRadius: '8px' }}>
                                <p style={{ fontSize: '16px', marginBottom: '16px', color: '#991b1b', fontWeight: '500' }}>
                                    Sigur doriți să anulați această programare?
                                </p>
                                <button
                                    onClick={handleCancel}
                                    disabled={actionLoading}
                                    style={{ ...buttonStyle, backgroundColor: '#c0392b', color: 'white' }}
                                >
                                    {actionLoading ? "Se anulează..." : "Da, anulează definitiv"}
                                </button>
                                <button
                                    onClick={() => setShowCancelConfirm(false)}
                                    disabled={actionLoading}
                                    style={{ ...buttonStyle, backgroundColor: 'transparent', color: '#4b5563' }}
                                >
                                    M-am răzgândit
                                </button>
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
};

export default AppointmentConfirmPage;