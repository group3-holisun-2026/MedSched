import { useNavigate } from 'react-router-dom';
import IncompletePatientsWidget from './Patient/IncompletePatientsWidget';

export default function DashboardPage() {
    const navigate = useNavigate();

    const handleCompletePatient = (patient) => {
        // Navigam spre /patients si deschidem formularul pentru acest pacient
        navigate('/patients', { state: { editPatient: patient } });
    };

    return (
        <div style={{ padding: '20px' }}>
            <h1>Dashboard Principal</h1>
            <p>Aici medicii vor vedea calendarul cu programări.</p>

            <IncompletePatientsWidget onCompletePatient={handleCompletePatient} />
        </div>
    );
}