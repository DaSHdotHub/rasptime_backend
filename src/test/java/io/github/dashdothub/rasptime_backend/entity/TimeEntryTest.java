package io.github.dashdothub.rasptime_backend.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TimeEntry Break Calculation Tests")
class TimeEntryTest {

    private TimeEntry createEntry(int hoursWorked, int minutesWorked) {
        LocalDateTime punchIn = LocalDateTime.of(2026, 2, 24, 8, 0);
        LocalDateTime punchOut = punchIn.plusHours(hoursWorked).plusMinutes(minutesWorked);
        
        return TimeEntry.builder()
                .punchIn(punchIn)
                .punchOut(punchOut)
                .workDate(LocalDate.of(2026, 2, 24))
                .breakMinutes(0)
                .build();
    }

    @Nested
    @DisplayName("Required Break Minutes Calculation")
    class RequiredBreakMinutesTests {

        @Test
        @DisplayName("No break required for exactly 6 hours")
        void noBreakFor6Hours() {
            TimeEntry entry = createEntry(6, 0);
            assertEquals(0, entry.getRequiredBreakMinutes());
        }

        @Test
        @DisplayName("No break required for less than 6 hours")
        void noBreakForLessThan6Hours() {
            TimeEntry entry = createEntry(5, 59);
            assertEquals(0, entry.getRequiredBreakMinutes());
        }

        @Test
        @DisplayName("15 min break for 6h 15min work")
        void graduatedBreak6h15m() {
            TimeEntry entry = createEntry(6, 15);
            assertEquals(15, entry.getRequiredBreakMinutes());
        }

        @Test
        @DisplayName("27 min break for 6h 27min work")
        void graduatedBreak6h27m() {
            TimeEntry entry = createEntry(6, 27);
            assertEquals(27, entry.getRequiredBreakMinutes());
        }

        @Test
        @DisplayName("30 min break for 6h 30min work")
        void break30minAt6h30m() {
            TimeEntry entry = createEntry(6, 30);
            assertEquals(30, entry.getRequiredBreakMinutes());
        }

        @Test
        @DisplayName("30 min break for 6h 45min work")
        void break30minAt6h45m() {
            TimeEntry entry = createEntry(6, 45);
            assertEquals(30, entry.getRequiredBreakMinutes());
        }

        @Test
        @DisplayName("30 min break for exactly 9 hours")
        void break30minAt9h() {
            TimeEntry entry = createEntry(9, 0);
            assertEquals(30, entry.getRequiredBreakMinutes());
        }

        @Test
        @DisplayName("31 min break for 9h 1min work")
        void graduatedBreak9h1m() {
            TimeEntry entry = createEntry(9, 1);
            assertEquals(31, entry.getRequiredBreakMinutes());
        }

        @Test
        @DisplayName("40 min break for 9h 10min work")
        void graduatedBreak9h10m() {
            TimeEntry entry = createEntry(9, 10);
            assertEquals(40, entry.getRequiredBreakMinutes());
        }

        @Test
        @DisplayName("45 min break for 9h 15min work")
        void break45minAt9h15m() {
            TimeEntry entry = createEntry(9, 15);
            assertEquals(45, entry.getRequiredBreakMinutes());
        }

        @Test
        @DisplayName("45 min break for 10 hours work")
        void break45minAt10h() {
            TimeEntry entry = createEntry(10, 0);
            assertEquals(45, entry.getRequiredBreakMinutes());
        }

        @Test
        @DisplayName("45 min break for 12 hours work")
        void break45minAt12h() {
            TimeEntry entry = createEntry(12, 0);
            assertEquals(45, entry.getRequiredBreakMinutes());
        }

        @ParameterizedTest(name = "{0}h {1}min work should require {2} min break")
        @CsvSource({
            "5, 0, 0",      // 5h = no break
            "5, 59, 0",     // 5h59m = no break
            "6, 0, 0",      // 6h = no break (exactly 6h)
            "6, 1, 1",      // 6h01m = 1 min break
            "6, 15, 15",    // 6h15m = 15 min break
            "6, 29, 29",    // 6h29m = 29 min break
            "6, 30, 30",    // 6h30m = 30 min break
            "7, 0, 30",     // 7h = 30 min break
            "8, 0, 30",     // 8h = 30 min break
            "9, 0, 30",     // 9h = 30 min break (exactly 9h)
            "9, 1, 31",     // 9h01m = 31 min break
            "9, 10, 40",    // 9h10m = 40 min break
            "9, 14, 44",    // 9h14m = 44 min break
            "9, 15, 45",    // 9h15m = 45 min break
            "9, 30, 45",    // 9h30m = 45 min break
            "10, 0, 45",    // 10h = 45 min break
            "11, 0, 45",    // 11h = 45 min break
            "12, 0, 45"     // 12h = 45 min break
        })
        void parameterizedBreakCalculation(int hours, int minutes, int expectedBreak) {
            TimeEntry entry = createEntry(hours, minutes);
            assertEquals(expectedBreak, entry.getRequiredBreakMinutes(),
                    String.format("For %dh %dm work, expected %d min break", hours, minutes, expectedBreak));
        }
    }

    @Nested
    @DisplayName("Break Sufficiency Check")
    class BreakSufficiencyTests {

        @Test
        @DisplayName("Break is sufficient when actual >= required")
        void breakIsSufficient() {
            TimeEntry entry = createEntry(8, 0);  // requires 30 min
            entry.setBreakMinutes(30);
            assertTrue(entry.isBreakSufficient());
        }

        @Test
        @DisplayName("Break is sufficient when actual > required")
        void breakIsMoreThanSufficient() {
            TimeEntry entry = createEntry(8, 0);  // requires 30 min
            entry.setBreakMinutes(45);
            assertTrue(entry.isBreakSufficient());
        }

        @Test
        @DisplayName("Break is insufficient when actual < required")
        void breakIsInsufficient() {
            TimeEntry entry = createEntry(8, 0);  // requires 30 min
            entry.setBreakMinutes(15);
            assertFalse(entry.isBreakSufficient());
        }

        @Test
        @DisplayName("No break needed for short shift")
        void noBreakNeededForShortShift() {
            TimeEntry entry = createEntry(5, 0);  // requires 0 min
            entry.setBreakMinutes(0);
            assertTrue(entry.isBreakSufficient());
        }
    }

    @Nested
    @DisplayName("Net Duration Calculation")
    class NetDurationTests {

        @Test
        @DisplayName("Net duration subtracts break time")
        void netDurationSubtractsBreak() {
            TimeEntry entry = createEntry(8, 0);  // 480 gross minutes
            entry.setBreakMinutes(30);
            assertEquals(450, entry.getNetDuration().toMinutes());
        }

        @Test
        @DisplayName("Net duration with no break")
        void netDurationWithNoBreak() {
            TimeEntry entry = createEntry(5, 0);  // 300 gross minutes
            entry.setBreakMinutes(0);
            assertEquals(300, entry.getNetDuration().toMinutes());
        }

        @Test
        @DisplayName("Net duration with 45 min break")
        void netDurationWith45minBreak() {
            TimeEntry entry = createEntry(10, 0);  // 600 gross minutes
            entry.setBreakMinutes(45);
            assertEquals(555, entry.getNetDuration().toMinutes());
        }
    }

    @Nested
    @DisplayName("Gross Duration Calculation")
    class GrossDurationTests {

        @Test
        @DisplayName("Gross duration calculated correctly")
        void grossDurationCalculation() {
            TimeEntry entry = createEntry(8, 30);
            assertEquals(510, entry.getGrossDuration().toMinutes());
        }

        @Test
        @DisplayName("Gross duration is zero when punchOut is null")
        void grossDurationZeroWhenOpenEntry() {
            LocalDateTime punchIn = LocalDateTime.of(2026, 2, 24, 8, 0);
            TimeEntry entry = TimeEntry.builder()
                    .punchIn(punchIn)
                    .punchOut(null)
                    .workDate(LocalDate.of(2026, 2, 24))
                    .build();
            assertEquals(0, entry.getGrossDuration().toMinutes());
        }
    }
}
