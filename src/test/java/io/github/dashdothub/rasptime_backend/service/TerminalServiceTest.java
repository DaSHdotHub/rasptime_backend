package io.github.dashdothub.rasptime_backend.service;

import io.github.dashdothub.rasptime_backend.dto.PunchRequest;
import io.github.dashdothub.rasptime_backend.dto.PunchResponse;
import io.github.dashdothub.rasptime_backend.entity.Role;
import io.github.dashdothub.rasptime_backend.entity.TimeEntry;
import io.github.dashdothub.rasptime_backend.entity.User;
import io.github.dashdothub.rasptime_backend.repository.TimeEntryRepository;
import io.github.dashdothub.rasptime_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TerminalService Punch Tests")
class TerminalServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TimeEntryRepository timeEntryRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private TerminalService terminalService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L)
                .rfidTag("TEST123")
                .displayName("Test User")
                .role(Role.USER)
                .clockedIn(false)
                .active(true)
                .contractedMinutesPerWeek(2400)
                .build();
    }

    @Nested
    @DisplayName("Unknown RFID Tag")
    class UnknownRfidTests {

        @Test
        @DisplayName("Should return null for unknown RFID tag")
        void unknownRfidReturnsNull() {
            // Given
            PunchRequest request = new PunchRequest();
            request.setRfid("UNKNOWN_TAG");

            when(userRepository.findByRfidTagAndActiveTrue("UNKNOWN_TAG"))
                    .thenReturn(Optional.empty());

            // When
            PunchResponse response = terminalService.punch(request);

            // Then
            assertNull(response);
            verify(auditService).log(any(), isNull(), eq("UNKNOWN_TAG"), anyString());
        }

        @Test
        @DisplayName("Should return null for inactive user's RFID tag")
        void inactiveUserRfidReturnsNull() {
            // Given
            PunchRequest request = new PunchRequest();
            request.setRfid("INACTIVE_USER_TAG");

            // findByRfidTagAndActiveTrue returns empty for inactive users
            when(userRepository.findByRfidTagAndActiveTrue("INACTIVE_USER_TAG"))
                    .thenReturn(Optional.empty());

            // When
            PunchResponse response = terminalService.punch(request);

            // Then
            assertNull(response);
        }
    }

    @Nested
    @DisplayName("Clock In")
    class ClockInTests {

        @Test
        @DisplayName("Should clock in user successfully")
        void clockInSuccess() {
            // Given
            PunchRequest request = new PunchRequest();
            request.setRfid("TEST123");

            activeUser.setClockedIn(false);
            when(userRepository.findByRfidTagAndActiveTrue("TEST123"))
                    .thenReturn(Optional.of(activeUser));
            when(timeEntryRepository.save(any(TimeEntry.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PunchResponse response = terminalService.punch(request);

            // Then
            assertNotNull(response);
            assertEquals("CLOCK_IN", response.getAction());
            assertEquals("Test User", response.getDisplayName());
            assertTrue(activeUser.isClockedIn());
            verify(timeEntryRepository).save(any(TimeEntry.class));
        }
    }

    @Nested
    @DisplayName("Clock Out - Break Time Calculation")
    class ClockOutBreakTests {

        private TimeEntry createOpenEntry(int hoursAgo, int minutesAgo) {
            LocalDateTime punchIn = LocalDateTime.now()
                    .minusHours(hoursAgo)
                    .minusMinutes(minutesAgo);
            
            return TimeEntry.builder()
                    .id(1L)
                    .user(activeUser)
                    .punchIn(punchIn)
                    .punchOut(null)
                    .workDate(LocalDate.now())
                    .breakMinutes(0)
                    .autoClosedOut(false)
                    .build();
        }

        @Test
        @DisplayName("Should apply no break for 5h shift")
        void noBreakFor5hShift() {
            // Given
            PunchRequest request = new PunchRequest();
            request.setRfid("TEST123");

            activeUser.setClockedIn(true);
            TimeEntry openEntry = createOpenEntry(5, 0);

            when(userRepository.findByRfidTagAndActiveTrue("TEST123"))
                    .thenReturn(Optional.of(activeUser));
            when(timeEntryRepository.findByUserAndPunchOutIsNull(activeUser))
                    .thenReturn(Optional.of(openEntry));
            when(timeEntryRepository.save(any(TimeEntry.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PunchResponse response = terminalService.punch(request);

            // Then
            assertNotNull(response);
            assertEquals("CLOCK_OUT", response.getAction());
            
            ArgumentCaptor<TimeEntry> entryCaptor = ArgumentCaptor.forClass(TimeEntry.class);
            verify(timeEntryRepository).save(entryCaptor.capture());
            assertEquals(0, entryCaptor.getValue().getBreakMinutes());
        }

        @Test
        @DisplayName("Should apply graduated 15min break for 6h 15min shift")
        void graduatedBreak6h15m() {
            // Given
            PunchRequest request = new PunchRequest();
            request.setRfid("TEST123");

            activeUser.setClockedIn(true);
            TimeEntry openEntry = createOpenEntry(6, 15);

            when(userRepository.findByRfidTagAndActiveTrue("TEST123"))
                    .thenReturn(Optional.of(activeUser));
            when(timeEntryRepository.findByUserAndPunchOutIsNull(activeUser))
                    .thenReturn(Optional.of(openEntry));
            when(timeEntryRepository.save(any(TimeEntry.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PunchResponse response = terminalService.punch(request);

            // Then
            ArgumentCaptor<TimeEntry> entryCaptor = ArgumentCaptor.forClass(TimeEntry.class);
            verify(timeEntryRepository).save(entryCaptor.capture());
            assertEquals(15, entryCaptor.getValue().getBreakMinutes());
        }

        @Test
        @DisplayName("Should apply 30min break for 8h shift")
        void break30minFor8hShift() {
            // Given
            PunchRequest request = new PunchRequest();
            request.setRfid("TEST123");

            activeUser.setClockedIn(true);
            TimeEntry openEntry = createOpenEntry(8, 0);

            when(userRepository.findByRfidTagAndActiveTrue("TEST123"))
                    .thenReturn(Optional.of(activeUser));
            when(timeEntryRepository.findByUserAndPunchOutIsNull(activeUser))
                    .thenReturn(Optional.of(openEntry));
            when(timeEntryRepository.save(any(TimeEntry.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PunchResponse response = terminalService.punch(request);

            // Then
            ArgumentCaptor<TimeEntry> entryCaptor = ArgumentCaptor.forClass(TimeEntry.class);
            verify(timeEntryRepository).save(entryCaptor.capture());
            assertEquals(30, entryCaptor.getValue().getBreakMinutes());
        }

        @Test
        @DisplayName("Should apply graduated 40min break for 9h 10min shift")
        void graduatedBreak9h10m() {
            // Given
            PunchRequest request = new PunchRequest();
            request.setRfid("TEST123");

            activeUser.setClockedIn(true);
            TimeEntry openEntry = createOpenEntry(9, 10);

            when(userRepository.findByRfidTagAndActiveTrue("TEST123"))
                    .thenReturn(Optional.of(activeUser));
            when(timeEntryRepository.findByUserAndPunchOutIsNull(activeUser))
                    .thenReturn(Optional.of(openEntry));
            when(timeEntryRepository.save(any(TimeEntry.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PunchResponse response = terminalService.punch(request);

            // Then
            ArgumentCaptor<TimeEntry> entryCaptor = ArgumentCaptor.forClass(TimeEntry.class);
            verify(timeEntryRepository).save(entryCaptor.capture());
            assertEquals(40, entryCaptor.getValue().getBreakMinutes());
        }

        @Test
        @DisplayName("Should apply 45min break for 10h shift")
        void break45minFor10hShift() {
            // Given
            PunchRequest request = new PunchRequest();
            request.setRfid("TEST123");

            activeUser.setClockedIn(true);
            TimeEntry openEntry = createOpenEntry(10, 0);

            when(userRepository.findByRfidTagAndActiveTrue("TEST123"))
                    .thenReturn(Optional.of(activeUser));
            when(timeEntryRepository.findByUserAndPunchOutIsNull(activeUser))
                    .thenReturn(Optional.of(openEntry));
            when(timeEntryRepository.save(any(TimeEntry.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PunchResponse response = terminalService.punch(request);

            // Then
            ArgumentCaptor<TimeEntry> entryCaptor = ArgumentCaptor.forClass(TimeEntry.class);
            verify(timeEntryRepository).save(entryCaptor.capture());
            assertEquals(45, entryCaptor.getValue().getBreakMinutes());
        }
    }

    @Nested
    @DisplayName("Clock Out - Manual Break Override")
    class ManualBreakOverrideTests {

        private TimeEntry createOpenEntry(int hoursAgo) {
            LocalDateTime punchIn = LocalDateTime.now().minusHours(hoursAgo);
            
            return TimeEntry.builder()
                    .id(1L)
                    .user(activeUser)
                    .punchIn(punchIn)
                    .punchOut(null)
                    .workDate(LocalDate.now())
                    .breakMinutes(0)
                    .autoClosedOut(false)
                    .build();
        }

        @Test
        @DisplayName("Should use manual break when higher than required")
        void useManualBreakWhenHigher() {
            // Given
            PunchRequest request = new PunchRequest();
            request.setRfid("TEST123");
            request.setBreakMinutes(60);  // User took 60 min break

            activeUser.setClockedIn(true);
            TimeEntry openEntry = createOpenEntry(8);  // requires 30 min

            when(userRepository.findByRfidTagAndActiveTrue("TEST123"))
                    .thenReturn(Optional.of(activeUser));
            when(timeEntryRepository.findByUserAndPunchOutIsNull(activeUser))
                    .thenReturn(Optional.of(openEntry));
            when(timeEntryRepository.save(any(TimeEntry.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PunchResponse response = terminalService.punch(request);

            // Then
            ArgumentCaptor<TimeEntry> entryCaptor = ArgumentCaptor.forClass(TimeEntry.class);
            verify(timeEntryRepository).save(entryCaptor.capture());
            assertEquals(60, entryCaptor.getValue().getBreakMinutes());
        }

        @Test
        @DisplayName("Should use required break when manual is lower than required")
        void useRequiredBreakWhenManualIsLower() {
            // Given
            PunchRequest request = new PunchRequest();
            request.setRfid("TEST123");
            request.setBreakMinutes(15);  // User says 15 min, but required is 30

            activeUser.setClockedIn(true);
            TimeEntry openEntry = createOpenEntry(8);  // requires 30 min

            when(userRepository.findByRfidTagAndActiveTrue("TEST123"))
                    .thenReturn(Optional.of(activeUser));
            when(timeEntryRepository.findByUserAndPunchOutIsNull(activeUser))
                    .thenReturn(Optional.of(openEntry));
            when(timeEntryRepository.save(any(TimeEntry.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PunchResponse response = terminalService.punch(request);

            // Then
            ArgumentCaptor<TimeEntry> entryCaptor = ArgumentCaptor.forClass(TimeEntry.class);
            verify(timeEntryRepository).save(entryCaptor.capture());
            // Should use required minimum (30), not manual (15)
            assertEquals(30, entryCaptor.getValue().getBreakMinutes());
        }

        @Test
        @DisplayName("Should use zero break when provided as zero for short shift")
        void useZeroBreakForShortShift() {
            // Given
            PunchRequest request = new PunchRequest();
            request.setRfid("TEST123");
            request.setBreakMinutes(0);

            activeUser.setClockedIn(true);
            TimeEntry openEntry = createOpenEntry(5);  // requires 0 min

            when(userRepository.findByRfidTagAndActiveTrue("TEST123"))
                    .thenReturn(Optional.of(activeUser));
            when(timeEntryRepository.findByUserAndPunchOutIsNull(activeUser))
                    .thenReturn(Optional.of(openEntry));
            when(timeEntryRepository.save(any(TimeEntry.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PunchResponse response = terminalService.punch(request);

            // Then
            ArgumentCaptor<TimeEntry> entryCaptor = ArgumentCaptor.forClass(TimeEntry.class);
            verify(timeEntryRepository).save(entryCaptor.capture());
            assertEquals(0, entryCaptor.getValue().getBreakMinutes());
        }
    }

    @Nested
    @DisplayName("Clock Out - Edge Cases")
    class ClockOutEdgeCases {

        @Test
        @DisplayName("Should handle missing open entry gracefully")
        void handleMissingOpenEntry() {
            // Given
            PunchRequest request = new PunchRequest();
            request.setRfid("TEST123");

            activeUser.setClockedIn(true);  // User says clocked in, but no entry

            when(userRepository.findByRfidTagAndActiveTrue("TEST123"))
                    .thenReturn(Optional.of(activeUser));
            when(timeEntryRepository.findByUserAndPunchOutIsNull(activeUser))
                    .thenReturn(Optional.empty());
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PunchResponse response = terminalService.punch(request);

            // Then
            assertNotNull(response);
            assertEquals("CLOCK_OUT", response.getAction());
            assertFalse(activeUser.isClockedIn());  // Should fix the state
            verify(timeEntryRepository, never()).save(any(TimeEntry.class));
        }
    }
}
