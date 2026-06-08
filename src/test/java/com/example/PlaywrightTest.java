package com.example;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class PlaywrightTest {
    private Playwright playwright;
    private Browser browser;
    private Page page;

    @BeforeEach
    public void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        page = browser.newPage();
    }

    @Test
    public void testSuccessfulLogin() {
        page.navigate("https://the-internet.herokuapp.com/login");
        page.fill("input[name='username']", "tomsmith");
        page.fill("input[name='password']", "SuperSecretPassword!");
        page.click("button:has-text('Login')");
        assertThat(page).hasURL("https://the-internet.herokuapp.com/secure");
    }

    @Test
    public void testFailedLogin() {
        page.navigate("https://the-internet.herokuapp.com/login");
        page.fill("input[name='username']", "invalidUser");
        page.fill("input[name='password']", "invalidPassword");
        page.click("button:has-text('Login')");
        assertThat(page.locator("#flash")).containsText("Your username is invalid!");
    }

    @AfterEach
    public void tearDown() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}