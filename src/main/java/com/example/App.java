package com.example;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.Proxy;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.Mouse;

import net.datafaker.Faker;

import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class App {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/playwright_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "sugoma";

    public static void main(String[] args) {
        List<List<String>> proxies = List.of(
            List.of("91.188.242.71:9779", "pYEZ3d", "9mZApC"),
            List.of("91.188.243.95:9956", "pYEZ3d", "9mZApC"),
            List.of("91.188.240.60:9805", "pYEZ3d", "9mZApC"),
            List.of("91.188.243.165:9895", "pYEZ3d", "9mZApC"),
            List.of("91.188.240.67:9110", "pYEZ3d", "9mZApC"),
            List.of("91.188.241.35:9690", "pYEZ3d", "9mZApC"),
            List.of("91.188.243.185:9884", "pYEZ3d", "9mZApC"),
            List.of("91.188.240.40:9828", "pYEZ3d", "9mZApC"),
            List.of("91.188.243.156:9080", "pYEZ3d", "9mZApC"),
            List.of("91.188.241.58:9449", "pYEZ3d", "9mZApC")
        );
 

        try (Playwright playwright = Playwright.create()) {

            Proxy proxy = new Proxy("http://91.188.242.71:9779").setUsername("pYEZ3d").setPassword("9mZApC");

            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setProxy(proxy).setHeadless(false).setArgs(java.util.Arrays
                            .asList("--disable-blink-features=AutomationControlled", "--disable-infobars")));

            Faker faker = new Faker(Locale.of("ru"));
            String randomDomain = faker.options().option("@mail.ru", "@yandex.ru", "@gmail.com", "@bk.ru",
                    "@inbox.ru");

            Random random = new Random();

            Map<String, String> browserProfile = getRealisticBrowserProfile();
            String randomUA = browserProfile.get("userAgent");
            System.out.println("Используем UA: " + randomUA);

            int[] viewport = getRealisticViewport();
            int randomWidth = viewport[0];
            int randomHeight = viewport[1];

            Map<String, String> extraHeaders = new HashMap<>();
            extraHeaders.put("Sec-CH-UA", browserProfile.get("secChUa"));
            extraHeaders.put("Sec-CH-UA-Mobile", "?0");
            extraHeaders.put("Sec-CH-UA-Platform", browserProfile.get("secChUaPlatform"));

            Browser.NewContextOptions options = new Browser.NewContextOptions()
                    .setUserAgent(randomUA)
                    .setViewportSize(randomWidth, randomHeight)
                    .setExtraHTTPHeaders(extraHeaders); // <-- Добавляем заголовки

            BrowserContext context = browser.newContext(options);

            // Подменяем JS-окружение под наш сгенерированный профиль
            String initScript = String.format("""
                        Object.defineProperty(navigator, 'userAgentData', {
                            get: () => ({
                                brands: [
                                    {brand: 'Google Chrome', version: '%s'},
                                    {brand: 'Chromium', version: '%s'},
                                    {brand: 'Not.A/Brand', version: '24'}
                                ],
                                mobile: false,
                                platform: %s
                            })
                        });
                        Object.defineProperty(navigator, 'platform', { get: () => '%s' });
                    """,
                    browserProfile.get("version"),
                    browserProfile.get("version"),
                    browserProfile.get("secChUaPlatform"),
                    browserProfile.get("jsPlatform"));

            context.addInitScript(initScript);

            Page page = context.newPage();

            String fileName = "accounts.txt";
            Path filePath = Paths.get(fileName);

            long currentLines = 1;

            try {
                if (Files.exists(filePath)) {
                    try (Stream<String> lines = Files.lines(filePath)) {

                        currentLines = lines.count() + 1;
                    }
                }
            } catch (IOException err) {
                System.out.println("Не удалось прочитать файл: " + err.getMessage());
            }

            String str = "";

            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();

            int randnum = faker.number().numberBetween(100, 10000);

            String email = transliterate(firstName).toLowerCase() + transliterate(lastName).toLowerCase()
                    + randnum
                    + randomDomain;

            String password = faker.credentials().password(8, 14, true, true, true);

            // =================== //

            page.navigate("https://litmarket.ru/");

            double[] mousePos = { random.nextInt(randomWidth), random.nextInt(randomHeight) };
            page.mouse().move(mousePos[0], mousePos[1]);

            // Locator button = page.locator("button[type='submit']");
            // BoundingBox box = button.boundingBox();

            // if (box != null) {
            // double targetX = box.x + (box.width / 2);
            // double targetY = box.y + (box.height / 2);

            // page.mouse().move(targetX, targetY, new Mouse.MoveOptions().setSteps(16));

            // randomSleep(page, 300, 500);
            // page.mouse().down();
            // page.mouse().up();
            // }

            humanClick(page, page.locator(".login-btn"), mousePos);

            humanType(page, page.locator("input[name='email']"), email, mousePos);
            humanType(page, page.locator("input[name='password']"), password, mousePos);
            randomSleep(page, 200, 400);

            humanClick(page, page.locator("input[value='Регистрация']"), mousePos);

            Locator eulaAgreeCheckbox = page.locator("input[name='eula_agree']");
            System.out.println(eulaAgreeCheckbox.count());

            randomSleep(page, 90, 180);
            eulaAgreeCheckbox.evaluate("node => node.click()");
            System.out.println("hu");

            Locator eula18Checkbox = page.locator("input[name='eula_18_years']");
            randomSleep(page, 90, 180);
            eula18Checkbox.evaluate("node => node.click()");
            System.out.println("hi");

            humanClick(page, page.locator("button[type='submit']").last(), mousePos);

            boolean hasError = false;
            try {
                page.getByText("уже существует").waitFor(new Locator.WaitForOptions().setTimeout(5000));
                hasError = true;
            } catch (PlaywrightException e) {
                hasError = false;
            }

            if (hasError) {
                System.out.println("Почта уже существует");
            } else {
                System.out.println("Почта успешно создана");

                try (FileWriter fw = new FileWriter(fileName, true);
                        BufferedWriter writer = new BufferedWriter(fw)) {

                    str = currentLines + " | " + email + " | " + password + " | " + randomUA + " | "
                            + randomWidth
                            + " | " + randomHeight;

                    writer.write(str + "\n");

                } catch (IOException err) {
                    System.out.println("Ошибка: " + err.getMessage());
                }
            }

            System.out.println("Успешное завершение теста");

            browser.close();
        }
    }

    public static String transliterate(String text) {
        String cyrillic = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя";
        String[] latin = {
                "A", "B", "V", "G", "D", "E", "E", "Zh", "Z", "I", "I", "K", "L", "M", "N", "O", "P", "R", "S", "T",
                "U", "F", "Kh", "Ts", "Ch", "Sh", "Sh", "", "Y", "", "E", "Yu", "Ya",
                "a", "b", "v", "g", "d", "e", "e", "zh", "z", "i", "i", "k", "l", "m", "n", "o", "p", "r", "s", "t",
                "u", "f", "kh", "ts", "ch", "sh", "sh", "", "y", "", "e", "yu", "ya"
        };

        StringBuilder sb = new StringBuilder();

        for (char ch : text.toCharArray()) {
            int index = cyrillic.indexOf(ch);
            if (index >= 0) {
                // Если буква русская — заменяем на латинскую из массива
                sb.append(latin[index]);
            } else {
                // Если это пробел, тире или английская буква — оставляем как есть
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static void randomSleep(Page page, int minMs, int maxMs) {
        Random random = new Random();
        int randomTimeout = random.nextInt(minMs, maxMs + 1);
        page.waitForTimeout(randomTimeout);
    }

    public static Map<String, String> getRealisticBrowserProfile() {
        Random random = new Random();
        Map<String, String> profile = new HashMap<>();

        int chromeVersion = random.nextInt(122, 126);
        profile.put("version", String.valueOf(chromeVersion));

        // 2. Выбираем платформу и ОС
        int osChance = random.nextInt(100);
        String osString;
        String secChUaPlatform;
        String jsPlatform;

        if (osChance < 70) {
            // Windows 10 и 11 (У них одинаковый маркер NT 10.0)
            osString = "Windows NT 10.0; Win64; x64";
            secChUaPlatform = "\"Windows\"";
            jsPlatform = "Win32";
        } else if (osChance < 90) {
            // macOS (Apple намеренно заморозила версию на 10_15_7 для приватности)
            osString = "Macintosh; Intel Mac OS X 10_15_7";
            secChUaPlatform = "\"macOS\"";
            jsPlatform = "MacIntel";
        } else {
            // Linux
            osString = "X11; Linux x86_64";
            secChUaPlatform = "\"Linux\"";
            jsPlatform = "Linux x86_64";
        }

        String userAgent = String.format(
                "Mozilla/5.0 (%s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%d.0.0.0 Safari/537.36",
                osString, chromeVersion);

        // Client Hints
        String secChUa = String.format("\"Google Chrome\";v=\"%d\", \"Chromium\";v=\"%d\", \"Not.A/Brand\";v=\"24\"",
                chromeVersion, chromeVersion);

        profile.put("userAgent", userAgent);
        profile.put("secChUa", secChUa);
        profile.put("secChUaPlatform", secChUaPlatform);
        profile.put("jsPlatform", jsPlatform);

        return profile;
    }

    public static int[] getRealisticViewport() {
        Random random = new Random();
        int chance = random.nextInt(100);

        // 50% - Full HD, 30% - 1366x768, 20% - остальные
        if (chance < 50) {
            return new int[] { 1920, 1080 };
        } else if (chance < 80) {
            return new int[] { 1366, 768 };
        } else {
            int[][] other = { { 1536, 864 }, { 1440, 900 }, { 2560, 1440 } };
            return other[random.nextInt(other.length)];
        }
    }

    public static void moveMouse(Page page, double startX, double startY, double targetX, double targetY) {
        Random random = new Random();

        double distance = Math.hypot(targetX - startX, targetY - startY);

        // ем дальше лететь, тем быстрее рука делает рывок.
        // Берем по 1 шагу на каждые 15 пикселей, но не меньше 15 и не больше 45 шагов
        // всего.
        int steps = (int) (distance / 15);
        if (steps < 15)
            steps = 15 + random.nextInt(5);
        if (steps > 45)
            steps = 40 + random.nextInt(10);

        double controlX1 = startX + (targetX - startX) * 0.3 + (random.nextDouble() * 100 - 50);
        double controlY1 = startY + (targetY - startY) * 0.3 + (random.nextDouble() * 100 - 50);
        double controlX2 = startX + (targetX - startX) * 0.7 + (random.nextDouble() * 100 - 50);
        double controlY2 = startY + (targetY - startY) * 0.7 + (random.nextDouble() * 100 - 50);

        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;

            double x = Math.pow(1 - t, 3) * startX +
                    3 * Math.pow(1 - t, 2) * t * controlX1 +
                    3 * (1 - t) * Math.pow(t, 2) * controlX2 +
                    Math.pow(t, 3) * targetX;

            double y = Math.pow(1 - t, 3) * startY +
                    3 * Math.pow(1 - t, 2) * t * controlY1 +
                    3 * (1 - t) * Math.pow(t, 2) * controlY2 +
                    Math.pow(t, 3) * targetY;

            page.mouse().move(x, y);

            // УСКОРЕННЫЕ ПАУЗЫ: 1-2 мс в полете, 3-5 мс на прицеливании
            try {
                int delay = (t < 0.2 || t > 0.8) ? (3 + random.nextInt(3)) : (1 + random.nextInt(2));
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        page.mouse().move(targetX, targetY);
    }

    /**
     * Функция для человечного клика по любому элементу
     */
    public static void humanClick(Page page, Locator locator, double[] currentMousePos) {
        Random random = new Random();

        locator.scrollIntoViewIfNeeded();
        randomSleep(page, 150, 300);

        BoundingBox box = locator.boundingBox();

        if (box == null) {
            System.out.println("Ошибка: Не удалось найти BoundingBox элемента!");
            return;
        }

        // Выводим в консоль размеры, чтобы понять, куда мы целимся
        System.out.println(String.format("Целимся в элемент: width=%.1f, height=%.1f", box.width, box.height));

        // Случайное отклонение от центра элемента
        int offsetX = box.width > 20 ? random.nextInt(-(int) (box.width / 4), (int) (box.width / 4)) : 0;
        int offsetY = box.height > 20 ? random.nextInt(-(int) (box.height / 4), (int) (box.height / 4)) : 0;

        double targetX = box.x + (box.width / 2) + offsetX;
        double targetY = box.y + (box.height / 2) + offsetY;

        moveMouse(page, currentMousePos[0], currentMousePos[1], targetX, targetY);

        currentMousePos[0] = targetX;
        currentMousePos[1] = targetY;

        randomSleep(page, 50, 160);
        page.mouse().down();
        randomSleep(page, 30, 110);
        page.mouse().up();
    }

    /**
     * Человечный ввод текста с плавающей скоростью и редкими опечатками
     */
    public static void humanType(Page page, Locator locator, String text, double[] currentMousePos) {
        Random random = new Random();

        // 1. Сначала по-человечески кликаем в поле ввода, чтобы навестись и поставить
        // курсор
        humanClick(page, locator, currentMousePos);
        randomSleep(page, 130, 300);

        // 2. Разбиваем текст на символы и вводим по одному
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Вводим правильный символ
            page.keyboard().type(String.valueOf(c));

            // 3. Имитация ошибки (шанс 3% напечатать лишнюю букву, если это не конец
            // текста)
            if (random.nextInt(100) < 3 && i < text.length() - 1) {
                // Печатаем случайную английскую букву
                char typo = (char) (random.nextInt(26) + 'a');
                page.keyboard().type(String.valueOf(typo));

                randomSleep(page, 200, 400); // Человек замечает ошибку (задержка реакции)
                page.keyboard().press("Backspace"); // Стираем опечатку
                randomSleep(page, 100, 200); // Пауза перед продолжением
            }

            // 4. Плавающая пауза между клавишами
            int delay;
            if (random.nextInt(100) < 80) {
                delay = random.nextInt(50, 120);
            } else {
                delay = random.nextInt(150, 300);
            }

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}