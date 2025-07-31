package com.minidocto.availability.repository;

import com.minidocto.availability.model.AvailabilitySlot;
import com.minidocto.availability.model.SlotStatus;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface AvailabilitySlotRepository extends MongoRepository<AvailabilitySlot, String> {
    List<AvailabilitySlot> findByDoctorId(ObjectId doctorId);
    List<AvailabilitySlot> findByDoctorIdAndStartTimeBetween(ObjectId doctorId, String start, String end);
    
    // Find available slots for a specific doctor
    List<AvailabilitySlot> findByDoctorIdAndStatus(ObjectId doctorId, SlotStatus status);
    
    // Find available slots for a specific doctor after a certain time
    @Query("{ 'doctorId': ?0, 'status': ?1, 'startTime': { $gt: ?2 } }")
    List<AvailabilitySlot> findByDoctorIdAndStatusAndStartTimeAfter(ObjectId doctorId, SlotStatus status, String startTime);
    
    // Find all distinct doctor IDs that have at least one available slot with future startTime
    @Query(value = "{ 'status': ?0, 'startTime': { $gt: ?1 } }", fields = "{ 'doctorId': 1 }")
    List<AvailabilitySlot> findDistinctDoctorIdsWithFutureAvailableSlots(SlotStatus status, String currentDateTime);
    
    // Find available slots for a doctor after current time (for available doctors endpoint)
    @Query("{ 'doctorId': ?0, 'status': ?1, 'startTime': { $gt: ?2 } }")
    List<AvailabilitySlot> findAvailableSlotsByDoctorIdAndAfterTime(ObjectId doctorId, SlotStatus status, String currentDateTime);
    
    // Find booked slots by a specific user (patient) in a date range for any doctor
    @Query("{ 'bookedBy': ?0, 'status': 'BOOKED', 'startTime': { $gte: ?1, $lte: ?2 } }")
    List<AvailabilitySlot> findBookedSlotsByUserInDateRange(ObjectId userId, String startDate, String endDate);
    
    // Find user's booked slots with a specific doctor after a certain time
    @Query("{ 'doctorId': ?0, 'bookedBy': ?1, 'status': 'BOOKED', 'startTime': { $gt: ?2 } }")
    List<AvailabilitySlot> findByDoctorIdAndBookedByAndStartTimeAfter(ObjectId doctorId, ObjectId bookedBy, String startTime);
} 