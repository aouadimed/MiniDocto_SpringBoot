package com.minidocto.availability.service;

import com.minidocto.availability.dto.AvailableDoctorDTO;
import com.minidocto.availability.dto.AvailableDoctorsResponse;
import com.minidocto.availability.dto.ScheduleGroupDTO;
import com.minidocto.availability.dto.ScheduleGroupsResponse;
import com.minidocto.availability.dto.PaginationDTO;
import com.minidocto.availability.dto.SlotWithUserDTO;
import com.minidocto.availability.model.AvailabilitySlot;
import com.minidocto.availability.model.SlotStatus;
import com.minidocto.availability.repository.AvailabilitySlotRepository;
import com.minidocto.user.model.Role;
import com.minidocto.user.model.User;
import com.minidocto.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing doctor's availability slots in a separate collection.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {
    private final AvailabilitySlotRepository slotRepository;
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(AvailabilityService.class);

    public List<AvailabilitySlot> getDoctorSlots(String doctorEmailOrId) {
        // Convert email to ID if necessary, then convert to ObjectId
        String doctorId = doctorEmailOrId.contains("@") ? getUserIdByEmail(doctorEmailOrId) : doctorEmailOrId;
        return slotRepository.findByDoctorId(new ObjectId(doctorId));
    }

    public List<AvailabilitySlot> getDoctorSlotsByDate(String doctorEmailOrId, String date) {
        // Convert email to ID if necessary, then convert to ObjectId
        String doctorId = doctorEmailOrId.contains("@") ? getUserIdByEmail(doctorEmailOrId) : doctorEmailOrId;
        // date is in format YYYY-MM-DD
        LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
        String startOfDay = localDate.atStartOfDay(ZoneOffset.UTC).toString(); // e.g., 2025-07-23T00:00Z
        String endOfDay = localDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toString(); // next day 00:00Z
        return slotRepository.findByDoctorIdAndStartTimeBetween(new ObjectId(doctorId), startOfDay, endOfDay);
    }

    @Transactional
    public void addSlots(String doctorEmailOrId, List<AvailabilitySlot> slots) {
        // Convert email to ID if necessary, then convert to ObjectId
        String doctorId = doctorEmailOrId.contains("@") ? getUserIdByEmail(doctorEmailOrId) : doctorEmailOrId;
        ObjectId doctorObjectId = new ObjectId(doctorId);
        for (AvailabilitySlot slot : slots) {
            slot.setDoctorId(doctorObjectId);
            slotRepository.save(slot);
        }
        logger.info("Added slots for doctor {}: {}", doctorId, slots);
    }

    @Transactional
    public void removeSlots(String doctorEmailOrId, List<AvailabilitySlot> slotsToRemove) {
        // Convert email to ID if necessary (though this might not be needed for removal)
        String doctorId = doctorEmailOrId.contains("@") ? getUserIdByEmail(doctorEmailOrId) : doctorEmailOrId;
        for (AvailabilitySlot slot : slotsToRemove) {
            slotRepository.deleteById(slot.getId());
        }
        logger.info("Removed slots for doctor {}: {}", doctorId, slotsToRemove);
    }

    public AvailableDoctorsResponse getAvailableDoctors(int page, int size) {
        // Get current datetime as string
        String currentDateTime = LocalDateTime.now(ZoneOffset.UTC).toString();
        
        // Find all slots with future available status
        List<AvailabilitySlot> availableSlots = slotRepository.findDistinctDoctorIdsWithFutureAvailableSlots(SlotStatus.AVAILABLE, currentDateTime);
        
        // Get unique doctor IDs (convert ObjectId to String)
        List<String> doctorIds = availableSlots.stream()
                .map(slot -> slot.getDoctorId().toString())
                .distinct()
                .collect(Collectors.toList());
        
        if (doctorIds.isEmpty()) {
            return AvailableDoctorsResponse.builder()
                    .availableDoctors(new ArrayList<>())
                    .currentPage(page)
                    .totalPages(0)
                    .build();
        }
        
        // Find all doctors that are PRO (doctors) and have available slots
        List<User> availableDoctors = userRepository.findAllById(doctorIds).stream()
                .filter(user -> user.getRole() == Role.PRO)
                .sorted(Comparator.comparing(User::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        
        // Calculate pagination
        int totalDoctors = availableDoctors.size();
        int totalPages = (int) Math.ceil((double) totalDoctors / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalDoctors);
        
        if (startIndex >= totalDoctors) {
            return AvailableDoctorsResponse.builder()
                    .availableDoctors(new ArrayList<>())
                    .currentPage(page)
                    .totalPages(totalPages)
                    .build();
        }
        
        // Get doctors for current page
        List<User> pagedDoctors = availableDoctors.subList(startIndex, endIndex);
        
        // Convert to DTOs
        List<AvailableDoctorDTO> doctorDTOs = new ArrayList<>();
        for (User doctor : pagedDoctors) {
            // Find the earliest available slot for this doctor
            List<AvailabilitySlot> doctorSlots = slotRepository.findAvailableSlotsByDoctorIdAndAfterTime(
                    new ObjectId(doctor.getId()), SlotStatus.AVAILABLE, currentDateTime);
            
            if (!doctorSlots.isEmpty()) {
                // Sort by startTime to get the earliest
                AvailabilitySlot earliestSlot = doctorSlots.stream()
                        .min(Comparator.comparing(AvailabilitySlot::getStartTime))
                        .orElse(doctorSlots.get(0));
                
                AvailableDoctorDTO dto = AvailableDoctorDTO.builder()
                        .id(doctor.getId()) // Add doctor ID
                        .name(doctor.getName())
                        .category(doctor.getSpecialty() != null ? doctor.getSpecialty() : "General Medicine")
                        .image("https://as2.ftcdn.net/v2/jpg/06/14/96/05/1000_F_614960515_mQsF7nS1r3qZ9eCHzqJ5cyCxmjsfJOCQ.webp") // Static placeholder
                        .experience("5+ years") // Static placeholder
                        .datetime(formatDateTime(earliestSlot.getStartTime()))
                        .score(doctor.getScore() != null ? doctor.getScore().doubleValue() : 4.5) // Convert Integer to Double with default
                        .build();
                
                doctorDTOs.add(dto);
            }
        }
        
        return AvailableDoctorsResponse.builder()
                .availableDoctors(doctorDTOs)
                .currentPage(page)
                .totalPages(totalPages)
                .build();
    }
    
    /**
     * Get schedule groups for a specific doctor with single day groupings
     */
    public ScheduleGroupsResponse getDoctorScheduleGroups(String doctorId, int page, int size, String currentUserEmail) {
        try {
            // Get current datetime as string
            String currentDateTime = LocalDateTime.now(ZoneOffset.UTC).toString();
            
            // Fetch all available slots for the doctor
            List<AvailabilitySlot> availableSlots = slotRepository.findByDoctorIdAndStatusAndStartTimeAfter(
                    new ObjectId(doctorId), SlotStatus.AVAILABLE, currentDateTime);
            
            // Also fetch user's booked slots with this doctor if authenticated
            List<AvailabilitySlot> userBookedSlots = new ArrayList<>();
            String currentUserId = null;
            if (currentUserEmail != null) {
                try {
                    currentUserId = getUserIdByEmail(currentUserEmail);
                    ObjectId currentUserObjectId = new ObjectId(currentUserId);
                    
                    // Get user's booked slots with this doctor
                    userBookedSlots = slotRepository.findByDoctorIdAndBookedByAndStartTimeAfter(
                            new ObjectId(doctorId), currentUserObjectId, currentDateTime);
                } catch (Exception e) {
                    logger.warn("Could not fetch user booked slots for user: {}", currentUserEmail, e);
                }
            }
            
            // Combine available slots and user's booked slots
            List<AvailabilitySlot> allSlots = new ArrayList<>();
            allSlots.addAll(availableSlots);
            allSlots.addAll(userBookedSlots);
            
            if (allSlots.isEmpty()) {
                return createEmptyScheduleGroupsResponse(page, size);
            }
            
            // Get current user's booked slots if authenticated
            Map<String, Boolean> userBookingsByDate = new HashMap<>();
            if (currentUserEmail != null && currentUserId != null) {
                try {
                    userBookingsByDate = getUserBookingsByDate(currentUserId, allSlots);
                } catch (Exception e) {
                    logger.warn("Could not fetch user bookings for {}: {}", currentUserEmail, e.getMessage());
                }
            }
            
            // Group slots by single day (now includes both available and user's booked slots)
            List<ScheduleGroupDTO> scheduleGroups = groupSlotsByDay(allSlots, userBookingsByDate, currentUserEmail);
            
            // Apply pagination
            int totalGroups = scheduleGroups.size();
            int totalPages = (int) Math.ceil((double) totalGroups / size);
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalGroups);
            
            List<ScheduleGroupDTO> pagedGroups = startIndex < totalGroups ? 
                    scheduleGroups.subList(startIndex, endIndex) : new ArrayList<>();
            
            // Build pagination info
            PaginationDTO pagination = PaginationDTO.builder()
                    .currentPage(page)
                    .pageSize(size)
                    .totalPages(totalPages)
                    .totalItems(totalGroups)
                    .hasNextPage(page < totalPages - 1)
                    .hasPreviousPage(page > 0)
                    .build();
            
            // Build response
            ScheduleGroupsResponse.ScheduleGroupsData data = ScheduleGroupsResponse.ScheduleGroupsData.builder()
                    .scheduleGroups(pagedGroups)
                    .pagination(pagination)
                    .build();
            
            return ScheduleGroupsResponse.builder()
                    .success(true)
                    .message("Schedule groups retrieved successfully")
                    .data(data)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error fetching schedule groups for doctor {}: {}", doctorId, e.getMessage(), e);
            return ScheduleGroupsResponse.builder()
                    .success(false)
                    .message("Error retrieving schedule groups: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }
    
    /**
     * Get user's bookings mapped by date
     */
    private Map<String, Boolean> getUserBookingsByDate(String userId, List<AvailabilitySlot> availableSlots) {
        Map<String, Boolean> userBookingsByDate = new HashMap<>();
        
        // Get all unique dates from available slots
        Set<String> dates = new HashSet<>();
        for (AvailabilitySlot slot : availableSlots) {
            try {
                LocalDateTime slotDateTime = LocalDateTime.parse(slot.getStartTime().replace("Z", ""));
                LocalDate slotDate = slotDateTime.toLocalDate();
                dates.add(slotDate.toString());
            } catch (Exception e) {
                logger.warn("Error parsing slot date for user booking check: {}", slot.getStartTime(), e);
            }
        }
        
        // Check each date for user bookings
        for (String dateStr : dates) {
            LocalDate date = LocalDate.parse(dateStr);
            
            // Convert to datetime strings for MongoDB query (whole day range)
            String startDateTime = date.atStartOfDay(ZoneOffset.UTC).toString();
            String endDateTime = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toString();
            
            // Check if user has any bookings on this date
            List<AvailabilitySlot> userBookings = slotRepository.findBookedSlotsByUserInDateRange(
                    new ObjectId(userId), startDateTime, endDateTime);
            
            userBookingsByDate.put(dateStr, !userBookings.isEmpty());
        }
        
        return userBookingsByDate;
    }
    
    /**
     * Helper method to group slots by single day
     */
    private List<ScheduleGroupDTO> groupSlotsByDay(List<AvailabilitySlot> slots, Map<String, Boolean> userBookingsByDate, String currentUserEmail) {
        // Sort slots by start time
        slots.sort(Comparator.comparing(AvailabilitySlot::getStartTime));
        
        Map<String, List<SlotWithUserDTO>> groupedSlots = new LinkedHashMap<>();
        
        for (AvailabilitySlot slot : slots) {
            try {
                // Parse the slot start time to get the date
                LocalDateTime slotDateTime = LocalDateTime.parse(slot.getStartTime().replace("Z", ""));
                LocalDate slotDate = slotDateTime.toLocalDate();
                String dateKey = slotDate.toString();
                
                // Convert AvailabilitySlot to SlotWithUserDTO and fetch user email if booked
                SlotWithUserDTO slotWithUser = convertToSlotWithUserDTO(slot);
                
                groupedSlots.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(slotWithUser);
            } catch (Exception e) {
                logger.warn("Error parsing slot start time: {}", slot.getStartTime(), e);
            }
        }
        
        // Convert to ScheduleGroupDTO
        List<ScheduleGroupDTO> scheduleGroups = new ArrayList<>();
        int groupIndex = 1;
        
        for (Map.Entry<String, List<SlotWithUserDTO>> entry : groupedSlots.entrySet()) {
            String dateStr = entry.getKey();
            
            // Get the actual availability slots for this date
            List<SlotWithUserDTO> timeSlots = entry.getValue();
            
            // Check if user has bookings on this date
            boolean hasUserBooking = userBookingsByDate.getOrDefault(dateStr, false);
            
            ScheduleGroupDTO group = ScheduleGroupDTO.builder()
                    .id("day_" + String.format("%03d", groupIndex++))
                    .date(dateStr)
                    .availableSlots(timeSlots.size())
                    .timeSlots(timeSlots)
                    .hasUserBookingInGroup(hasUserBooking)
                    .build();
            
            scheduleGroups.add(group);
        }
        
        return scheduleGroups;
    }
    
    /**
     * Create empty response for schedule groups
     */
    private ScheduleGroupsResponse createEmptyScheduleGroupsResponse(int page, int size) {
        PaginationDTO pagination = PaginationDTO.builder()
                .currentPage(page)
                .pageSize(size)
                .totalPages(0)
                .totalItems(0)
                .hasNextPage(false)
                .hasPreviousPage(false)
                .build();
                
        ScheduleGroupsResponse.ScheduleGroupsData data = ScheduleGroupsResponse.ScheduleGroupsData.builder()
                .scheduleGroups(new ArrayList<>())
                .pagination(pagination)
                .build();
        
        return ScheduleGroupsResponse.builder()
                .success(true)
                .message("No available schedule groups found")
                .data(data)
                .build();
    }
    
    private String formatDateTime(String isoDateTime) {
        try {
            // Parse the ISO datetime and format it for display
            LocalDateTime dateTime = LocalDateTime.parse(isoDateTime.replace("Z", ""));
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e) {
            logger.warn("Error formatting datetime: {}", isoDateTime, e);
            return isoDateTime; // Return original if parsing fails
        }
    }
    
    /**
     * Helper method to get User ID from email.
     * This is needed because UserDetails.getUsername() returns email, not ID.
     */
    private String getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }
    
    /**
     * Convert AvailabilitySlot to SlotWithUserDTO and fetch user email if booked
     */
    private SlotWithUserDTO convertToSlotWithUserDTO(AvailabilitySlot slot) {
        SlotWithUserDTO.SlotWithUserDTOBuilder builder = SlotWithUserDTO.builder()
                .id(slot.getId())
                .doctorId(slot.getDoctorId())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .status(slot.getStatus())
                .bookedBy(slot.getBookedBy());
        
        // If slot is booked, fetch the user email
        if (slot.getBookedBy() != null && slot.getStatus() == SlotStatus.BOOKED) {
            try {
                Optional<User> bookedUser = userRepository.findById(slot.getBookedBy().toString());
                if (bookedUser.isPresent()) {
                    builder.bookedByEmail(bookedUser.get().getEmail());
                } else {
                    builder.bookedByEmail("Unknown User");
                }
            } catch (Exception e) {
                logger.warn("Error fetching user email for slot {}: {}", slot.getId(), e.getMessage());
                builder.bookedByEmail("Unknown User");
            }
        }
        
        return builder.build();
    }
} 