package com.errorpurifier.global.common;

import java.util.List;

public record CursorSlice<T>(List<T> items, String nextCursor, boolean hasNext) {

    public static <T> CursorSlice<T> of(List<T> items, String nextCursor) {
        return new CursorSlice<>(items, nextCursor, nextCursor != null);
    }
}
