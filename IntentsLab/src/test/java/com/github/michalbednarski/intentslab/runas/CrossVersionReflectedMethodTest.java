package com.github.michalbednarski.intentslab.runas;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

public class CrossVersionReflectedMethodTest {
    @Test
    public void testInexactMissingMiddle() throws InvocationTargetException {

        CrossVersionReflectedMethod method = new CrossVersionReflectedMethod(TestClass.class)
                .tryMethodVariantInexact(
                        "testMethod1",
                        int.class,    "a", 1,
                        long.class,   "c", 3
                );

        String result;
        // Check with one provided arg
        result = (String) method.invoke(new TestClass(), "c", 5);
        Assert.assertEquals("a=1 b=null c=5", result);

        // Check with no provided args
        result = (String) method.invoke(new TestClass());
        Assert.assertEquals("a=1 b=null c=3", result);
    }

    @Test
    public void testInexactMiddlePresent() throws InvocationTargetException {

        CrossVersionReflectedMethod method = new CrossVersionReflectedMethod(TestClass.class)
                .tryMethodVariantInexact(
                        "testMethod1",
                        String.class, "b", "b_default"
                );

        // Check with no provided args
        String result = (String) method.invoke(new TestClass());
        Assert.assertEquals("a=0 b=b_default c=0", result);
    }

    public static class TestClass {
        public String testMethod1(int a, String b, long c) {
            return "a=" + a + " b=" + b + " c=" + c;
        }
    }
}