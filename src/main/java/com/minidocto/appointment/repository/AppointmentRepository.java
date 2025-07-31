package com.minidocto.appointment.repository;

import com.minidocto.appointment.model.Appointment;
import com.minidocto.appointment.model.AppointmentStatus;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends MongoRepository<Appointment, String> {
    
    // Find appointments by patient ID
    List<Appointment> findByPatientIdOrderByCreatedAtDesc(ObjectId patientId);
    
    // Find upcoming appointments for a patient
    @Query("{ 'patientId': ?0, 'status': { $in: ['PENDING', 'CONFIRMED'] } }")
    List<Appointment> findUpcomingAppointmentsByPatient(ObjectId patientId);
    
    // Find upcoming appointments for a patient with pagination
    @Query("{ 'patientId': ?0, 'status': { $in: ['PENDING', 'CONFIRMED'] } }")
    Page<Appointment> findUpcomingAppointmentsByPatient(ObjectId patientId, Pageable pageable);
    
    // Find all appointments by patient ID with pagination
    Page<Appointment> findByPatientIdOrderByCreatedAtDesc(ObjectId patientId, Pageable pageable);
    
    // Find appointments by doctor ID
    List<Appointment> findByDoctorIdOrderByCreatedAtDesc(ObjectId doctorId);
    
    // Find appointments by doctor ID with pagination
    Page<Appointment> findByDoctorIdOrderByCreatedAtDesc(ObjectId doctorId, Pageable pageable);
    
    // Check if patient has appointment on specific date
    @Query("{ 'patientId': ?0, 'createdAt': { $gte: ?1, $lt: ?2 }, 'status': { $ne: 'CANCELLED' } }")
    List<Appointment> findPatientAppointmentsOnDate(ObjectId patientId, LocalDateTime startOfDay, LocalDateTime endOfDay);
    
    // Find existing appointment between patient and doctor (not completed or cancelled)
    @Query("{ 'patientId': ?0, 'doctorId': ?1, 'status': { $in: ['PENDING', 'CONFIRMED'] } }")
    Optional<Appointment> findActiveAppointmentBetweenPatientAndDoctor(ObjectId patientId, ObjectId doctorId);
    
    // Find appointment by slot ID
    Optional<Appointment> findBySlotId(ObjectId slotId);
    
    // Find appointments by status
    List<Appointment> findByStatus(AppointmentStatus status);
    
    // Find appointment by ID and patient ID (for security)
    Optional<Appointment> findByIdAndPatientId(String appointmentId, ObjectId patientId);
}
