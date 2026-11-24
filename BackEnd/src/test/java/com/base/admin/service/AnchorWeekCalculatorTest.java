package com.base.admin.service;

import com.base.admin.service.AnchorWeekCalculator.Attribution;
import com.base.admin.service.AnchorWeekCalculator.FanUse;
import com.base.admin.service.AnchorWeekCalculator.Session;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorWeekCalculatorTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 21, 19, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 11, 21, 22, 0);
    private static final Session EARLY = new Session(1, 8, "hour", START.minusHours(3), START, null, null);
    private static final Session LIVE = new Session(2, 9, "hour", START, END, null, null);
    private static final Session LATER = new Session(3, 10, "flagship", START.plusMinutes(30), END, null, null);

    @Test
    void talentIdWinsOverSchedule() {
        Attribution hit = AnchorWeekCalculator.attribute("DY-1", 5L, "hour", START.plusHours(1), List.of(LIVE));
        assertEquals(5L, hit.anchorId());
        assertEquals("talent", hit.source());
    }

    @Test
    void unknownTalentStaysUnmatched() {
        Attribution miss = AnchorWeekCalculator.attribute("DY-404", null, null, START.plusHours(1), List.of(LIVE));
        assertEquals(0L, miss.anchorId());
        assertEquals("none", miss.source());
    }

    @Test
    void emptyTalentUsesLaterOverlappingSession() {
        Attribution hit = AnchorWeekCalculator.attribute("", null, null, START.plusHours(1), List.of(EARLY, LIVE, LATER));
        assertEquals(10L, hit.anchorId());
        assertEquals("flagship", hit.channelCode());
        assertEquals("schedule", hit.source());
    }

    @Test
    void payOutsideScheduleIsUnmatched() {
        Attribution miss = AnchorWeekCalculator.attribute(null, null, null, END.plusMinutes(1), List.of(LIVE));
        assertEquals("none", miss.source());
    }

    @Test
    void excludedDouyinIsNotAStreamer() {
        assertTrue(AnchorWeekCalculator.excludedFan("49136593613"));
        assertFalse(AnchorWeekCalculator.excludedFan("1001"));
    }

    @Test
    void fanMismatchWithoutConfirmPaysZero() {
        FanUse use = AnchorWeekCalculator.fansForSession(3, 5, null);
        assertTrue(use.mismatch());
        assertTrue(use.pending());
        assertEquals(0, use.used());
    }

    @Test
    void confirmedCountIsWhatGetsPaid() {
        FanUse use = AnchorWeekCalculator.fansForSession(3, 5, 4);
        assertTrue(use.mismatch());
        assertFalse(use.pending());
        assertEquals(4, use.used());
    }

    @Test
    void rawCountIsUsedWhenThereIsNoSummary() {
        FanUse use = AnchorWeekCalculator.fansForSession(3, null, null);
        assertFalse(use.mismatch());
        assertEquals(3, use.used());
    }
}
