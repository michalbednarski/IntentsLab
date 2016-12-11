package com.github.michalbednarski.intentslab;


import android.graphics.Color;
import android.support.annotation.NonNull;
import android.test.AndroidTestCase;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

/**
 * Tests for {@link FormattedTextBuilder}'s optimized custom Spannable
 */
public class FormattedTextBuilderTest extends AndroidTestCase {

    private static final int PERF_TEST_SPAN_COUNT = 30000;

    public void testWithSystemSpannable() throws Exception {
        Spannable spannable = createFtbToTest().getTextAsSystemSpannable();
        assertSame(SpannableString.class, spannable.getClass());
        testSpannableFromFtb(spannable);
    }

    public void testWithMySpannable() throws Exception {
        Spannable spannable = createFtbToTest().getTextAsMySpannable();
        assertNotSame(SpannableString.class, spannable.getClass());
        testSpannableFromFtb(spannable);
    }

    @NonNull
    private FormattedTextBuilder createFtbToTest() {
        FormattedTextBuilder ftb = new FormattedTextBuilder();
        ftb.appendColoured("Hello", Color.RED);
        ftb.appendColoured("World", Color.GREEN);
        ftb.appendRaw("12345");
        ftb.appendColoured("abcde", Color.BLUE);
        return ftb;
    }

    /**
     * Test spannable generated through {@link #createFtbToTest()}
     */
    private void testSpannableFromFtb(Spannable spannable) {
        // getSpans should return any spans that overlap given range
        assertEquals(1, spannable.getSpans(0, 2, Object.class).length);
        assertEquals(1, spannable.getSpans(4, 5, Object.class).length);
        assertEquals(1, spannable.getSpans(5, 6, Object.class).length);
        assertEquals(1, spannable.getSpans(9, 10, Object.class).length);

        assertEquals(2, spannable.getSpans(4, 6, Object.class).length);

        // Test properties of "World" span
        Object worldSpan = spannable.getSpans(6, 7, Object.class)[0];

        assertSame(ForegroundColorSpan.class, worldSpan.getClass());
        assertEquals(Color.GREEN, ((ForegroundColorSpan) worldSpan).getForegroundColor());

        assertEquals(5, spannable.getSpanStart(worldSpan));
        assertEquals(10, spannable.getSpanEnd(worldSpan));
        assertEquals(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE, spannable.getSpanFlags(worldSpan));

        // nextSpanTransition should return position of next start or end of span
        // or limit if there isn't any
        assertEquals(5, spannable.nextSpanTransition(3, 7, Object.class));
        assertEquals(4, spannable.nextSpanTransition(0, 4, Object.class));
        assertEquals(10, spannable.nextSpanTransition(7, 11, Object.class));
        assertEquals(10, spannable.nextSpanTransition(7, 18, Object.class));
        assertEquals(15, spannable.nextSpanTransition(10, 18, Object.class));

        // Added span should live along "optimized" spans
        AddedSpan addedSpan = new AddedSpan();
        spannable.setSpan(addedSpan, 1, 3, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        assertEquals(2, spannable.getSpans(0, 2, Object.class).length);
        assertEquals(1, spannable.getSpans(0, 2, AddedSpan.class).length);
        assertEquals(1, spannable.nextSpanTransition(0, 10, Object.class));
        assertEquals(3, spannable.nextSpanTransition(1, 10, Object.class));
        assertEquals(5, spannable.nextSpanTransition(3, 10, Object.class));

        // Test properties of added span
        assertEquals(1, spannable.getSpanStart(addedSpan));
        assertEquals(3, spannable.getSpanEnd(addedSpan));
        assertEquals(Spanned.SPAN_EXCLUSIVE_INCLUSIVE, spannable.getSpanFlags(addedSpan));

        // Once span is removed it's not visible through getSpans nor nextSpanTransition
        spannable.removeSpan(addedSpan);
        assertEquals(1, spannable.getSpans(0, 2, Object.class).length);
        assertEquals(0, spannable.getSpans(0, 2, AddedSpan.class).length);
        assertEquals(5, spannable.nextSpanTransition(0, 10, Object.class));

        // Test properties of removed span
        assertEquals(-1, spannable.getSpanStart(addedSpan));
        assertEquals(-1, spannable.getSpanEnd(addedSpan));
        assertEquals(0, spannable.getSpanFlags(addedSpan));

    }

    public void testPerformance() throws Exception {
        FormattedTextBuilder ftb = new FormattedTextBuilder();
        for (int i = 0; i < PERF_TEST_SPAN_COUNT; i++) {
            ftb.appendColoured("A", Color.RED);
        }

        Spannable spannable = ftb.getText();

        for (int i = 0; i < PERF_TEST_SPAN_COUNT - 5; i++) {
            assertEquals(5, spannable.getSpans(i, i + 5, Object.class).length);
            assertEquals(i + 1, spannable.nextSpanTransition(i, i + 5, Object.class));
        }
    }

    private static class AddedSpan {}
}