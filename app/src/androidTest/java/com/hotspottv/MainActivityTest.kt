package com.hotspottv

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val intentsRule = IntentsRule()

    @Test
    fun launchShowsMainScreen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.screenTitle)).check(matches(isDisplayed()))
            onView(withId(R.id.codeEditText)).check(matches(isDisplayed()))
            onView(withId(R.id.connectButton)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun validCodeNavigatesToRenderer() {
        ActivityScenario.launch(MainActivity::class.java).use {
            Intents.intending(hasComponent(RendererActivity::class.java.name))

            onView(withId(R.id.codeEditText))
                .perform(typeText("TV2665487D"), closeSoftKeyboard())
            onView(withId(R.id.connectButton)).perform(click())

            Intents.intended(hasComponent(RendererActivity::class.java.name))
        }
    }
}
