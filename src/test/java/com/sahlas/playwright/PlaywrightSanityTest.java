package com.sahlas.playwright;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PlaywrightSanityTest {

    @Test
    public void shouldLaunchBrowserAndOpenExampleDotCom() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.navigate("https://example.com");
            String title = page.title();
            assertTrue(title != null && title.length() > 0, "Page title should be present");
            browser.close();
        }
    }
}

