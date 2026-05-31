package com.chat.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroupAssistantInvocationManagerTest {

    private GroupAssistantInvocationManager mgr;

    @BeforeEach
    void setUp() {
        mgr = new GroupAssistantInvocationManager();
    }

    @Test
    void firstAcquireReturnsOK() {
        assertEquals(GroupAssistantInvocationManager.AcquireResult.OK,
                mgr.tryAcquire(1L, 100L, 1000L));
    }

    @Test
    void secondAcquireWhileInFlightReturnsInFlight() {
        mgr.tryAcquire(1L, 100L, 1000L);
        assertEquals(GroupAssistantInvocationManager.AcquireResult.IN_FLIGHT,
                mgr.tryAcquire(1L, 100L, 1500L));
    }

    @Test
    void afterReleaseWithinDebounceWindowReturnsDebounced() {
        mgr.tryAcquire(1L, 100L, 1000L);
        mgr.release(1L, 100L);
        assertEquals(GroupAssistantInvocationManager.AcquireResult.DEBOUNCED,
                mgr.tryAcquire(1L, 100L, 1500L));
    }

    @Test
    void afterReleaseAndDebounceWindowReturnsOK() {
        mgr.tryAcquire(1L, 100L, 1000L);
        mgr.release(1L, 100L);
        assertEquals(GroupAssistantInvocationManager.AcquireResult.OK,
                mgr.tryAcquire(1L, 100L, 3500L));
    }

    @Test
    void differentAssistantsAreIndependent() {
        mgr.tryAcquire(1L, 100L, 1000L);
        assertEquals(GroupAssistantInvocationManager.AcquireResult.OK,
                mgr.tryAcquire(1L, 200L, 1000L));
    }

    @Test
    void differentRoomsAreIndependent() {
        mgr.tryAcquire(1L, 100L, 1000L);
        assertEquals(GroupAssistantInvocationManager.AcquireResult.OK,
                mgr.tryAcquire(2L, 100L, 1000L));
    }
}
