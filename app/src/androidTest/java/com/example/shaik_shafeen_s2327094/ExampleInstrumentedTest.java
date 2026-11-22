package com.example.shaik_shafeen_s2327094;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test that runs on an Android device.
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Test
    public void useAppContext() {
        // Gets the context of the app being tested.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Checks that the app is using the correct package name.
        assertEquals("com.example.shaik_shafeen_s2327094", appContext.getPackageName());
    }
}