package com.zombietime.app

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CharacterInteractionTest {
    @Test fun touchExpressionsSoundSwitchAndBrandIcons() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val ctx = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        Configurator.getInstance().setWaitForIdleTimeout(100)
        ctx.getSharedPreferences("zombietime", 0).edit().putBoolean("onboarded", true)
            .putBoolean("monitor_on", false).commit()
        ctx.getSharedPreferences("character", 0).edit().putBoolean("sound", true).commit()
        device.executeShellCommand("appops set ${ctx.packageName} GET_USAGE_STATS allow")
        // AGP uninstalls the target after testing, so keep captures outside app storage.
        device.executeShellCommand("mkdir -p /data/local/tmp/zombie-ui")
        fun screenshot(name: String) {
            val path = "/data/local/tmp/zombie-ui/$name.png"
            device.executeShellCommand("screencap -p $path")
            assertTrue(device.executeShellCommand("ls -l $path").contains("$name.png"))
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertTrue(device.wait(Until.hasObject(By.text("소리 켜짐")), 15000))
            Thread.sleep(800)
            screenshot("01-home")
            val pet = By.desc("캐릭터와 놀기. 누르면 표정과 목소리로 반응해요")
            listOf("헤헤, 나 불렀어?", "앗! 깜짝이야!", "우리 잠깐 쉬어갈까?").forEachIndexed { i, reply ->
                device.findObject(pet).click()
                assertTrue(device.wait(Until.hasObject(By.text(reply)), 1500))
                Thread.sleep(200)
                screenshot("0${i + 2}-reaction")
                Thread.sleep(1300)
            }
            device.findObject(By.text("소리 켜짐")).click()
            assertTrue(device.wait(Until.hasObject(By.text("소리 꺼짐")), 2000))
            assertFalse(ctx.getSharedPreferences("character", 0).getBoolean("sound", true))
            scenario.recreate()
            assertTrue(device.wait(Until.hasObject(By.text("소리 꺼짐")), 10000))
            // Bring the real application list into the viewport and check accessible logos.
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4,
                device.displayWidth / 2, device.displayHeight / 4, 35)
            assertTrue(device.wait(Until.hasObject(By.desc("인스타그램")), 4000))
            assertTrue(device.hasObject(By.desc("스레드")))
            screenshot("05-app-logos")
            device.swipe(device.displayWidth / 2, device.displayHeight / 4,
                device.displayWidth / 2, device.displayHeight * 3 / 4, 35)
            device.executeShellCommand("settings put system font_scale 1.3")
            scenario.recreate()
            assertTrue(device.wait(Until.hasObject(By.text("소리 꺼짐")), 10000))
            screenshot("06-large-type")
            device.executeShellCommand("settings put system font_scale 1.0")
        }
    }
}
