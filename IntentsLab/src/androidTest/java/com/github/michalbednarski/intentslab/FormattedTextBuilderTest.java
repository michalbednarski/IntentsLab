package com.github.michalbednarski.intentslab;


import android.graphics.Color;
import android.support.annotation.NonNull;
import android.test.AndroidTestCase;
import android.text.Spannable;

import junit.framework.Assert;


/**
 * Tests for {@link FormattedTextBuilder}'s optimized custom Spannable
 */
public class FormattedTextBuilderTest extends AndroidTestCase {

    private static final int PERF_TEST_SPAN_COUNT = 30000;

    public void testWithSystemSpannable() throws Exception {
        testSpannableFromFtb(createFtbToTest().getTextAsSystemSpannable());
    }

    public void testWithMySpannable() throws Exception {
        testSpannableFromFtb(createFtbToTest().getTextAsMySpannable());
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
        Assert.assertEquals(1, spannable.getSpans(0, 2, Object.class).length);
        Assert.assertEquals(1, spannable.getSpans(4, 5, Object.class).length);
        Assert.assertEquals(1, spannable.getSpans(5, 6, Object.class).length);
        Assert.assertEquals(1, spannable.getSpans(9, 10, Object.class).length);

        Assert.assertEquals(2, spannable.getSpans(4, 6, Object.class).length);

        Assert.assertEquals(5, spannable.nextSpanTransition(3, 7, Object.class));
        Assert.assertEquals(4, spannable.nextSpanTransition(0, 4, Object.class));
        Assert.assertEquals(10, spannable.nextSpanTransition(7, 11, Object.class));
        Assert.assertEquals(10, spannable.nextSpanTransition(7, 18, Object.class));
        Assert.assertEquals(15, spannable.nextSpanTransition(10, 18, Object.class));
    }

    public void testPerformance() throws Exception {
        FormattedTextBuilder ftb = new FormattedTextBuilder();
        for (int i = 0; i < PERF_TEST_SPAN_COUNT; i++) {
            ftb.appendColoured("A", Color.RED);
        }

        Spannable spannable = (Spannable) ftb.getText();

        for (int i = 0; i < PERF_TEST_SPAN_COUNT - 5; i++) {
            Assert.assertEquals(5, spannable.getSpans(i, i + 5, Object.class).length);
            Assert.assertEquals(i + 1, spannable.nextSpanTransition(i, i + 5, Object.class));
        }
    }
}