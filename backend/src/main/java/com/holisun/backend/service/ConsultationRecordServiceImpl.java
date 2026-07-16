package com.holisun.backend.service;

import com.holisun.backend.dto.ConsultationRecordRequest;
import com.holisun.backend.dto.ConsultationRecordResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * TODO(P2): inlocuieste cu implementarea reala (entity/repository/business logic),
 * vezi documents/module2/backend_module2_tasks.md sectiunea 2. Placeholder doar
 * ca sa porneasca aplicatia cat timp domeniul ConsultationRecord nu e implementat.
 */
@Service
public class ConsultationRecordServiceImpl implements ConsultationRecordService {

    @Override
    public ConsultationRecordResponse getByAppointmentId(UUID appointmentId) {
        throw new UnsupportedOperationException("ConsultationRecordService.getByAppointmentId - nu e inca implementat de P2");
    }

    @Override
    public ConsultationRecordResponse create(UUID appointmentId, ConsultationRecordRequest dto) {
        throw new UnsupportedOperationException("ConsultationRecordService.create - nu e inca implementat de P2");
    }

    @Override
    public ConsultationRecordResponse update(UUID appointmentId, ConsultationRecordRequest dto) {
        throw new UnsupportedOperationException("ConsultationRecordService.update - nu e inca implementat de P2");
    }

    @Override
    public void lock(UUID appointmentId) {
        throw new UnsupportedOperationException("ConsultationRecordService.lock - nu e inca implementat de P2");
    }
}
