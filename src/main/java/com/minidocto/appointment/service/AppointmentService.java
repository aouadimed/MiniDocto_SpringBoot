package com.minidocto.appointment.service;

import com.minidocto.appointment.dto.*;
import com.minidocto.appointment.model.Appointment;
import com.minidocto.appointment.model.AppointmentStatus;
import com.minidocto.appointment.repository.AppointmentRepository;
import com.minidocto.availability.model.AvailabilitySlot;
import com.minidocto.availability.model.SlotStatus;
import com.minidocto.availability.repository.AvailabilitySlotRepository;
import com.minidocto.user.model.User;
import com.minidocto.user.repository.UserRepository;
import com.minidocto.shared.exception.ResourceNotFoundException;
import com.minidocto.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.minidocto.availability.dto.PaginationDTO;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final UserRepository userRepository;

    @Transactional
    public AppointmentResponse bookAppointment(ObjectId patientId, String doctorIdStr, String slotIdStr) {
        logger.info("Attempting to book appointment - PatientId: {}, DoctorId: {}, SlotId: {}", 
                   patientId, doctorIdStr, slotIdStr);
        
        try {
            // Convert string IDs to ObjectIds
            ObjectId doctorId = new ObjectId(doctorIdStr);
            ObjectId slotId = new ObjectId(slotIdStr);
            
            logger.debug("Converted ObjectIds - DoctorId: {}, SlotId: {}", doctorId, slotId);

            // Validate the availability slot exists and is available
            AvailabilitySlot slot = availabilitySlotRepository.findById(slotIdStr)
                    .orElseThrow(() -> new ResourceNotFoundException("Availability slot not found with ID: " + slotIdStr));

            logger.debug("Found slot: {} with status: {}", slot.getId(), slot.getStatus());

            // Check if slot is available
            if (slot.getStatus() != SlotStatus.AVAILABLE) {
                String message = "This time slot is no longer available. Current status: " + slot.getStatus();
                if (slot.getBookedBy() != null) {
                    if (slot.getBookedBy().equals(patientId)) {
                        message += " (This slot is currently booked by you)";
                    } else {
                        message += " (This slot is booked by another patient)";
                    }
                }
                logger.warn("Slot booking failed - SlotId: {}, Status: {}, BookedBy: {}, RequestedBy: {}", 
                           slotIdStr, slot.getStatus(), slot.getBookedBy(), patientId);
                           
                return AppointmentResponse.builder()
                        .success(false)
                        .message(message)
                        .build();
            }

            // Validate doctor ID matches slot's doctor ID
            if (!slot.getDoctorId().equals(doctorId)) {
                return AppointmentResponse.builder()
                        .success(false)
                        .message("Invalid doctor for this time slot")
                        .build();
            }

            // Check if patient already has an active appointment with this doctor
            Optional<Appointment> existingAppointmentOpt = appointmentRepository
                    .findActiveAppointmentBetweenPatientAndDoctor(patientId, doctorId);
            
            boolean isRebooking = existingAppointmentOpt.isPresent();
            Appointment appointmentToSave;
            
            if (isRebooking) {
                // UPDATE existing appointment with new slot
                Appointment existingAppointment = existingAppointmentOpt.get();
                
                // Free up the old slot if it exists
                if (existingAppointment.getSlotId() != null) {
                    Optional<AvailabilitySlot> oldSlotOpt = availabilitySlotRepository
                            .findById(existingAppointment.getSlotId().toString());
                    if (oldSlotOpt.isPresent()) {
                        AvailabilitySlot oldSlot = oldSlotOpt.get();
                        oldSlot.setStatus(SlotStatus.AVAILABLE);
                        oldSlot.setBookedBy(null);
                        availabilitySlotRepository.save(oldSlot);
                    }
                }
                
                // Update existing appointment with new slot
                existingAppointment.setSlotId(slotId);
                existingAppointment.setStatus(AppointmentStatus.PENDING);
                existingAppointment.setUpdatedAt(LocalDateTime.now());
                appointmentToSave = existingAppointment;
                
                logger.info("Rebooking appointment for patient {} with doctor {}", patientId, doctorId);
            } else {
                // CREATE new appointment
                appointmentToSave = Appointment.builder()
                        .doctorId(doctorId)
                        .patientId(patientId)
                        .slotId(slotId)
                        .status(AppointmentStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                
                logger.info("Creating new appointment for patient {} with doctor {}", patientId, doctorId);
            }

            // Save appointment (either new or updated)
            Appointment savedAppointment = appointmentRepository.save(appointmentToSave);

            // Update new slot status to BOOKED and set bookedBy
            slot.setStatus(SlotStatus.BOOKED);
            slot.setBookedBy(patientId);
            availabilitySlotRepository.save(slot);

            // Get doctor details (name and specialty) for response
            Optional<User> doctorOpt = userRepository.findById(doctorIdStr);
            String doctorName = "Unknown Doctor";
            String doctorSpecialty = "Unknown Specialty";
            
            if (doctorOpt.isPresent()) {
                User doctor = doctorOpt.get();
                doctorName = doctor.getName() != null ? doctor.getName() : "Unknown Doctor";
                doctorSpecialty = doctor.getSpecialty() != null ? doctor.getSpecialty() : "General Practice";
            }

            // Get patient details for response
            Optional<User> patientOpt = userRepository.findById(patientId.toString());
            String patientName = "Unknown Patient";
            String patientEmail = "Unknown Email";
            
            if (patientOpt.isPresent()) {
                User patient = patientOpt.get();
                patientName = patient.getName() != null ? patient.getName() : "Unknown Patient";
                patientEmail = patient.getEmail() != null ? patient.getEmail() : "Unknown Email";
            }

            // Build response DTO
            AppointmentDTO appointmentDTO = AppointmentDTO.builder()
                    .id(savedAppointment.getId())
                    .doctorId(doctorIdStr)
                    .patientId(patientId.toString())
                    .slotId(slotIdStr)
                    .status(savedAppointment.getStatus())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .doctorName(doctorName)
                    .doctorSpecialty(doctorSpecialty)
                    .patientName(patientName)
                    .patientEmail(patientEmail)
                    .createdAt(savedAppointment.getCreatedAt())
                    .updatedAt(savedAppointment.getUpdatedAt())
                    .build();

            logger.info("Appointment {} successfully: {}", isRebooking ? "rebooked" : "booked", savedAppointment.getId());

            String successMessage = isRebooking ? 
                    "Appointment rescheduled successfully" : 
                    "Appointment booked successfully";

            return AppointmentResponse.builder()
                    .success(true)
                    .message(successMessage)
                    .appointment(appointmentDTO)
                    .build();

        } catch (IllegalArgumentException e) {
            logger.error("Invalid ObjectId format - PatientId: {}, DoctorId: {}, SlotId: {}", 
                        patientId, doctorIdStr, slotIdStr, e);
            return AppointmentResponse.builder()
                    .success(false)
                    .message("Invalid ID format: " + e.getMessage())
                    .build();
        } catch (ResourceNotFoundException e) {
            logger.error("Resource not found: {}", e.getMessage(), e);
            return AppointmentResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error booking appointment - PatientId: {}, DoctorId: {}, SlotId: {}", 
                        patientId, doctorIdStr, slotIdStr, e);
            return AppointmentResponse.builder()
                    .success(false)
                    .message("Failed to book appointment: " + e.getMessage())
                    .build();
        }
    }

    public AppointmentListResponse getPatientAppointments(ObjectId patientId, int page, int size) {
        try {
            // Create pageable with sorting by creation date (newest first)
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            
            // Get paginated appointments
            Page<Appointment> appointmentPage = appointmentRepository.findByPatientIdOrderByCreatedAtDesc(patientId, pageable);
            
            List<AppointmentDTO> appointmentDTOs = appointmentPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            // Create pagination DTO
            PaginationDTO pagination = PaginationDTO.builder()
                    .currentPage(page)
                    .pageSize(size)
                    .totalPages(appointmentPage.getTotalPages())
                    .totalItems((int) appointmentPage.getTotalElements())
                    .hasNextPage(appointmentPage.hasNext())
                    .hasPreviousPage(appointmentPage.hasPrevious())
                    .build();

            return AppointmentListResponse.builder()
                    .success(true)
                    .message("Appointments retrieved successfully")
                    .appointments(appointmentDTOs)
                    .totalCount((int) appointmentPage.getTotalElements())
                    .pagination(pagination)
                    .build();

        } catch (Exception e) {
            logger.error("Error retrieving patient appointments", e);
            
            // Create empty pagination for error case
            PaginationDTO emptyPagination = PaginationDTO.builder()
                    .currentPage(page)
                    .pageSize(size)
                    .totalPages(0)
                    .totalItems(0)
                    .hasNextPage(false)
                    .hasPreviousPage(false)
                    .build();
                    
            return AppointmentListResponse.builder()
                    .success(false)
                    .message("Failed to retrieve appointments")
                    .appointments(List.of())
                    .totalCount(0)
                    .pagination(emptyPagination)
                    .build();
        }
    }

    // Keep the old method for backward compatibility
    public AppointmentListResponse getPatientAppointments(ObjectId patientId) {
        return getPatientAppointments(patientId, 0, 10); // Default: page 0, size 10
    }

    /**
     * Get appointments for a doctor (paginated)
     */
    public AppointmentListResponse getDoctorAppointments(ObjectId doctorId, int page, int size) {
        try {
            // Create pageable with sorting by creation date (newest first)
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            
            // Get paginated appointments
            Page<Appointment> appointmentPage = appointmentRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId, pageable);
            
            List<AppointmentDTO> appointmentDTOs = appointmentPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            // Create pagination DTO
            PaginationDTO pagination = PaginationDTO.builder()
                    .currentPage(page)
                    .pageSize(size)
                    .totalPages(appointmentPage.getTotalPages())
                    .totalItems((int) appointmentPage.getTotalElements())
                    .hasNextPage(appointmentPage.hasNext())
                    .hasPreviousPage(appointmentPage.hasPrevious())
                    .build();

            return AppointmentListResponse.builder()
                    .success(true)
                    .message("Doctor appointments retrieved successfully")
                    .appointments(appointmentDTOs)
                    .totalCount((int) appointmentPage.getTotalElements())
                    .pagination(pagination)
                    .build();

        } catch (Exception e) {
            logger.error("Error retrieving doctor appointments", e);
            
            // Create empty pagination for error case
            PaginationDTO emptyPagination = PaginationDTO.builder()
                    .currentPage(page)
                    .pageSize(size)
                    .totalPages(0)
                    .totalItems(0)
                    .hasNextPage(false)
                    .hasPreviousPage(false)
                    .build();
                    
            return AppointmentListResponse.builder()
                    .success(false)
                    .message("Failed to retrieve doctor appointments")
                    .appointments(List.of())
                    .totalCount(0)
                    .pagination(emptyPagination)
                    .build();
        }
    }

    // Keep the old method for backward compatibility
    public AppointmentListResponse getDoctorAppointments(ObjectId doctorId) {
        return getDoctorAppointments(doctorId, 0, 10); // Default: page 0, size 10
    }

    @Transactional
    public AppointmentResponse cancelAppointment(String appointmentId, ObjectId patientId) {
        try {
            // Find appointment by ID and patient ID for security
            Optional<Appointment> appointmentOpt = appointmentRepository.findByIdAndPatientId(appointmentId, patientId);
            
            if (appointmentOpt.isEmpty()) {
                return AppointmentResponse.builder()
                        .success(false)
                        .message("Appointment not found or you don't have permission to cancel it")
                        .build();
            }

            Appointment appointment = appointmentOpt.get();

            // Check if appointment can be cancelled
            if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                return AppointmentResponse.builder()
                        .success(false)
                        .message("Appointment is already cancelled")
                        .build();
            }

            if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
                return AppointmentResponse.builder()
                        .success(false)
                        .message("Cannot cancel a completed appointment")
                        .build();
            }

            // Get the associated slot
            Optional<AvailabilitySlot> slotOpt = availabilitySlotRepository.findById(appointment.getSlotId().toString());
            
            if (slotOpt.isPresent()) {
                AvailabilitySlot slot = slotOpt.get();
                
                // Check if appointment is not in the past
                LocalDateTime slotDateTime = LocalDateTime.parse(slot.getStartTime().replace("Z", ""));
                if (slotDateTime.isBefore(LocalDateTime.now())) {
                    return AppointmentResponse.builder()
                            .success(false)
                            .message("Cannot cancel appointments in the past")
                            .build();
                }

                // Free up the slot
                slot.setStatus(SlotStatus.AVAILABLE);
                slot.setBookedBy(null);
                availabilitySlotRepository.save(slot);
            }

            // Update appointment status
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setUpdatedAt(LocalDateTime.now());
            Appointment updatedAppointment = appointmentRepository.save(appointment);

            AppointmentDTO appointmentDTO = convertToDTO(updatedAppointment);

            logger.info("Appointment cancelled successfully: {}", appointmentId);

            return AppointmentResponse.builder()
                    .success(true)
                    .message("Appointment cancelled successfully")
                    .appointment(appointmentDTO)
                    .build();

        } catch (Exception e) {
            logger.error("Error cancelling appointment", e);
            return AppointmentResponse.builder()
                    .success(false)
                    .message("Failed to cancel appointment. Please try again.")
                    .build();
        }
    }

    private AppointmentDTO convertToDTO(Appointment appointment) {
        // Get slot details
        Optional<AvailabilitySlot> slotOpt = availabilitySlotRepository.findById(appointment.getSlotId().toString());
        String startTime = "";
        String endTime = "";
        
        if (slotOpt.isPresent()) {
            AvailabilitySlot slot = slotOpt.get();
            startTime = slot.getStartTime();
            endTime = slot.getEndTime();
        }

        // Get doctor details (name and specialty)
        Optional<User> doctorOpt = userRepository.findById(appointment.getDoctorId().toString());
        String doctorName = "Unknown Doctor";
        String doctorSpecialty = "Unknown Specialty";
        
        if (doctorOpt.isPresent()) {
            User doctor = doctorOpt.get();
            doctorName = doctor.getName() != null ? doctor.getName() : "Unknown Doctor";
            doctorSpecialty = doctor.getSpecialty() != null ? doctor.getSpecialty() : "General Practice";
        }

        // Get patient details (name and email)
        Optional<User> patientOpt = userRepository.findById(appointment.getPatientId().toString());
        String patientName = "Unknown Patient";
        String patientEmail = "Unknown Email";
        
        if (patientOpt.isPresent()) {
            User patient = patientOpt.get();
            patientName = patient.getName() != null ? patient.getName() : "Unknown Patient";
            patientEmail = patient.getEmail() != null ? patient.getEmail() : "Unknown Email";
        }

        return AppointmentDTO.builder()
                .id(appointment.getId())
                .doctorId(appointment.getDoctorId().toString())
                .patientId(appointment.getPatientId().toString())
                .slotId(appointment.getSlotId().toString())
                .status(appointment.getStatus())
                .startTime(startTime)
                .endTime(endTime)
                .doctorName(doctorName)
                .doctorSpecialty(doctorSpecialty)
                .patientName(patientName)
                .patientEmail(patientEmail)
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }
    
    // Debug method to check slot status
    public Object debugSlot(String slotId) {
        try {
            logger.info("Debug checking slot: {}", slotId);
            
            Optional<AvailabilitySlot> slotOpt = availabilitySlotRepository.findById(slotId);
            
            if (slotOpt.isEmpty()) {
                return "Slot not found with ID: " + slotId;
            }
            
            AvailabilitySlot slot = slotOpt.get();
            return "Slot found - ID: " + slot.getId() + 
                   ", Status: " + slot.getStatus() + 
                   ", DoctorId: " + slot.getDoctorId() + 
                   ", StartTime: " + slot.getStartTime() +
                   ", BookedBy: " + slot.getBookedBy();
                   
        } catch (Exception e) {
            logger.error("Error in debug slot", e);
            return "Error: " + e.getMessage();
        }
    }
}
