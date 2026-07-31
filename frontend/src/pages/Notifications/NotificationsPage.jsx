import { useCallback, useEffect, useState } from 'react';
import format from 'date-fns/format';
import { toast } from 'sonner';
import { notificationsApi } from '../../api/notifications';
import Card from '../../components/Card';
import Button from '../../components/Button';

const STATUS_COLORS = {
    SENT: '#2e8b57',
    PENDING: '#3174ad',
    FAILED: '#c0392b',
    CANCELLED: '#a0a0a0',
};

const STATUS_LABELS = {
    PENDING: 'În așteptare',
    SENT: 'Trimis',
    FAILED: 'Eșuat',
    CANCELLED: 'Anulat',
};

const TRIGGER_LABELS = {
    CONFIRMATION: 'Confirmare',
    REMINDER_24H: 'Reamintire 24h',
    RESCHEDULED: 'Reprogramare',
    CANCELLED: 'Anulare',
};

const PAGE_SIZE = 20;
const ERROR_TRUNCATE_LENGTH = 60;

function StatusBadge({ status }) {
    const color = STATUS_COLORS[status] || '#a0a0a0';
    return (
        <span
            style={{
                display: 'inline-block',
                padding: '2px 10px',
                borderRadius: '999px',
                fontSize: '0.8rem',
                fontWeight: 600,
                color: 'white',
                background: color,
                whiteSpace: 'nowrap',
            }}
        >
            {STATUS_LABELS[status] || status}
        </span>
    );
}

function truncate(text, length) {
    if (!text) return '—';
    return text.length > length ? `${text.slice(0, length)}…` : text;
}

export default function NotificationsPage() {
    const [notifications, setNotifications] = useState([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [statusFilter, setStatusFilter] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [retryingId, setRetryingId] = useState(null);

    const fetchNotifications = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await notificationsApi.list({
                status: statusFilter || undefined,
                page,
                size: PAGE_SIZE,
            });
            setNotifications(data.content ?? []);
            setTotalPages(data.totalPages ?? 0);
        } catch (err) {
            setError(err.response?.data?.message ?? 'Notificările nu au putut fi încărcate.');
            setNotifications([]);
        } finally {
            setLoading(false);
        }
    }, [statusFilter, page]);

    useEffect(() => {
        fetchNotifications();
    }, [fetchNotifications]);

    function handleStatusFilterChange(e) {
        setStatusFilter(e.target.value);
        setPage(0);
    }

    async function handleRetry(id) {
        setRetryingId(id);
        try {
            await notificationsApi.retry(id);
            toast.success('Notificarea a fost repusă în coadă.');
            fetchNotifications();
        } catch (err) {
            toast.error(err.response?.data?.message ?? 'Retrimiterea a eșuat.');
        } finally {
            setRetryingId(null);
        }
    }

    return (
        <div style={{ padding: '20px', maxWidth: '1100px', margin: '0 auto' }}>
            <h1 style={{ marginBottom: '16px' }}>Administrare notificări</h1>

            <Card className="mb-4">
                <div
                    style={{
                        display: 'flex',
                        flexWrap: 'wrap',
                        alignItems: 'flex-end',
                        gap: '16px',
                    }}
                >
                    <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                        Status
                        <select value={statusFilter} onChange={handleStatusFilterChange}>
                            <option value="">Toate</option>
                            <option value="PENDING">În așteptare</option>
                            <option value="SENT">Trimise</option>
                            <option value="FAILED">Eșuate</option>
                            <option value="CANCELLED">Anulate</option>
                        </select>
                    </label>

                    <Button
                        type="button"
                        variant="outline"
                        onClick={fetchNotifications}
                        disabled={loading}
                    >
                        {loading ? 'Se reîmprospătează...' : 'Reîmprospătează'}
                    </Button>
                </div>
            </Card>

            {loading && <p>Se încarcă notificările...</p>}
            {!loading && error && <p style={{ color: '#c0392b' }}>{error}</p>}
            {!loading && !error && notifications.length === 0 && (
                <p>Nu există notificări pentru filtrele selectate.</p>
            )}

            {!loading && !error && notifications.length > 0 && (
                <>
                    <div style={{ overflowX: 'auto' }}>
                        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                            <thead>
                                <tr>
                                    <th style={thStyle}>Data</th>
                                    <th style={thStyle}>Tip</th>
                                    <th style={thStyle}>Destinatar</th>
                                    <th style={thStyle}>Status</th>
                                    <th style={thStyle}>Încercări</th>
                                    <th style={thStyle}>Ultima eroare</th>
                                    <th style={thStyle}>Acțiune</th>
                                </tr>
                            </thead>
                            <tbody>
                                {notifications.map((n) => (
                                    <tr key={n.id}>
                                        <td style={tdStyle}>
                                            {format(new Date(n.createdAt), 'dd.MM.yyyy HH:mm')}
                                        </td>
                                        <td style={tdStyle}>{TRIGGER_LABELS[n.trigger] || n.trigger}</td>
                                        <td style={tdStyle}>{n.recipientPhone}</td>
                                        <td style={tdStyle}>
                                            <StatusBadge status={n.status} />
                                        </td>
                                        <td style={tdStyle}>{n.attempts}</td>
                                        <td style={tdStyle} title={n.lastError || ''}>
                                            {truncate(n.lastError, ERROR_TRUNCATE_LENGTH)}
                                        </td>
                                        <td style={tdStyle}>
                                            {n.status === 'FAILED' && (
                                                <Button
                                                    type="button"
                                                    variant="secondary"
                                                    onClick={() => handleRetry(n.id)}
                                                    disabled={retryingId === n.id}
                                                >
                                                    {retryingId === n.id ? 'Se retrimite...' : 'Retrimite'}
                                                </Button>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    <div
                        style={{
                            display: 'flex',
                            justifyContent: 'center',
                            alignItems: 'center',
                            gap: '12px',
                            marginTop: '16px',
                        }}
                    >
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => setPage((p) => Math.max(p - 1, 0))}
                            disabled={page === 0}
                        >
                            Înapoi
                        </Button>
                        <span>
                            Pagina {page + 1} din {Math.max(totalPages, 1)}
                        </span>
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => setPage((p) => Math.min(p + 1, totalPages - 1))}
                            disabled={page >= totalPages - 1}
                        >
                            Înainte
                        </Button>
                    </div>
                </>
            )}
        </div>
    );
}

const thStyle = {
    textAlign: 'left',
    borderBottom: '1px solid #ccc',
    padding: '8px',
    whiteSpace: 'nowrap',
};

const tdStyle = {
    padding: '8px',
    borderBottom: '1px solid #eee',
};