package com.fincoach.core.healthv2.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LogLimiterTest {

    @Test
    public void truncateHandlesNullAndBounds() {
        assertNull(LogLimiter.truncate(null, 10));
        assertEquals("", LogLimiter.truncate("abc", 0));
        assertEquals("abc", LogLimiter.truncate("abc", 10));
        assertEquals("cdef", LogLimiter.truncate("abcdef", 4));
        assertEquals("ef", LogLimiter.appendAndTruncate("abcd", "ef", 2));
    }
}
