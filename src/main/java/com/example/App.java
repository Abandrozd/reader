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
import java.util.Scanner; // Добавлен импорт Сканнера

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
        Path proxiesFile = Paths.get("proxies.txt");
        List<List<String>> proxies = new ArrayList<>();

        // ==========================================
        // ЧТЕНИЕ ПРОКСИ ИЗ ФАЙЛА
        // ==========================================
        if (!Files.exists(proxiesFile)) {
            System.err.println("Ошибка / proxies.txt не найден в корне проекта");
            return;
        }

        try {
            List<String> proxyLines = Files.readAllLines(proxiesFile);
            for (String line : proxyLines) {
                if (line.trim().isEmpty())
                    continue;

                // Ожидаемый формат: ip:port:user:password
                String[] parts = line.split(":");
                if (parts.length == 4) {
                    String ipPort = parts[0] + ":" + parts[1]; // Склеиваем IP и порт
                    String user = parts[2];
                    String password = parts[3];
                    proxies.add(List.of(ipPort, user, password));
                } else {
                    System.err.println(
                            "Пропущена строка прокси " + line + " / неверный формат (ожидается ip:port:user:password)");
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка при чтении файла proxies.txt");
            e.printStackTrace();
            return;
        }

        if (proxies.isEmpty()) {
            System.out.println("Ошибка / Список прокси пуст или имеет неверный формат.");
            return;
        }

        // ==========================================
        // ВВОД ГРАНИЦ ИЗ КОНСОЛИ
        // ==========================================
        Scanner scanner = new Scanner(System.in);
        System.out.print("С какого аккаунта (строки прокси) начать регистрацию? (например, 1): ");
        int startAccount = scanner.nextInt();

        System.out.print("По какой аккаунт включительно регистрировать? (например, 10): ");
        int endAccount = scanner.nextInt();

        if (startAccount > endAccount) {
            System.out.println("Ошибка: начальный номер больше конечного!");
            return;
        }
        if (startAccount < 1) {
            System.out.println("Ошибка: нумерация должна начинаться с 1 или больше.");
            return;
        }

        System.out.println("Ожидайте, запускаем браузер...");

        try (Playwright playwright = Playwright.create()) {

            for (int num = startAccount; num <= endAccount; num++) {
                int i = num - 1; // Индекс в списке (на 1 меньше номера аккаунта)

                // Проверяем, есть ли в файле proxies.txt достаточное количество строк
                if (i >= proxies.size()) {
                    System.out.println("Прокси на строке " + num + " нет в файле proxies.txt! Прерываем цикл.");
                    break;
                }

                List<String> proxyData = proxies.get(i);
                String proxyIp = proxyData.get(0);
                String proxyUser = proxyData.get(1);
                String proxyPass = proxyData.get(2);

                System.out.println("\n==============================================");
                System.out.println("Запуск аккаунта " + num + " | Прокси: " + proxyIp);
                System.out.println("==============================================");

                // Заворачиваем итерацию в try-catch, чтобы при падении одной реги цикл не
                // прервался
                try {
                    Proxy proxy = new Proxy("http://" + proxyIp)
                            .setUsername(proxyUser)
                            .setPassword(proxyPass);

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
                            .setExtraHTTPHeaders(extraHeaders);

                    BrowserContext context = browser.newContext(options);

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

                    page.navigate("https://litmarket.ru/");

                    double[] mousePos = { random.nextInt(randomWidth), random.nextInt(randomHeight) };
                    page.mouse().move(mousePos[0], mousePos[1]);

                    humanClick(page, page.locator(".login-btn"), mousePos);

                    humanType(page, page.locator("input[name='email']"), email, mousePos);
                    humanType(page, page.locator("input[name='password']"), password, mousePos);
                    randomSleep(page, 200, 400);

                    humanClick(page, page.locator("input[value='Регистрация']"), mousePos);

                    Locator eulaAgreeCheckbox = page.locator("input[name='eula_agree']");

                    randomSleep(page, 90, 180);
                    eulaAgreeCheckbox.evaluate("node => node.click()");

                    Locator eula18Checkbox = page.locator("input[name='eula_18_years']");
                    randomSleep(page, 90, 180);
                    eula18Checkbox.evaluate("node => node.click()");

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
                                    + randomWidth + " | " + randomHeight;

                            writer.write(str + "\n");

                        } catch (IOException err) {
                            System.out.println("Ошибка записи файла: " + err.getMessage());
                        }
                    }

                    System.out.println("Успешное завершение теста для " + email);

                    // Обязательно закрываем браузер после каждой итерации, чтобы не перегрузить ОЗУ
                    browser.close();

                } catch (Exception e) {
                    System.err.println("Ошибка при работе с прокси " + proxyIp + " : " + e.getMessage());
                    e.printStackTrace();
                }
            }
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
                sb.append(latin[index]);
            } else {
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

        int osChance = random.nextInt(100);
        String osString;
        String secChUaPlatform;
        String jsPlatform;

        if (osChance < 70) {
            osString = "Windows NT 10.0; Win64; x64";
            secChUaPlatform = "\"Windows\"";
            jsPlatform = "Win32";
        } else if (osChance < 90) {
            osString = "Macintosh; Intel Mac OS X 10_15_7";
            secChUaPlatform = "\"macOS\"";
            jsPlatform = "MacIntel";
        } else {
            osString = "X11; Linux x86_64";
            secChUaPlatform = "\"Linux\"";
            jsPlatform = "Linux x86_64";
        }

        String userAgent = String.format(
                "Mozilla/5.0 (%s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%d.0.0.0 Safari/537.36",
                osString, chromeVersion);

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

            try {
                int delay = (t < 0.2 || t > 0.8) ? (3 + random.nextInt(3)) : (1 + random.nextInt(2));
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        page.mouse().move(targetX, targetY);
    }

    public static void humanClick(Page page, Locator locator, double[] currentMousePos) {
        Random random = new Random();

        locator.scrollIntoViewIfNeeded();
        randomSleep(page, 150, 300);

        BoundingBox box = locator.boundingBox();

        if (box == null) {
            System.out.println("Ошибка: Не удалось найти BoundingBox элемента!");
            return;
        }

        System.out.println(String.format("Целимся в элемент: width=%.1f, height=%.1f", box.width, box.height));

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

    public static void humanType(Page page, Locator locator, String text, double[] currentMousePos) {
        Random random = new Random();

        humanClick(page, locator, currentMousePos);
        randomSleep(page, 130, 300);

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            page.keyboard().type(String.valueOf(c));

            if (random.nextInt(100) < 3 && i < text.length() - 1) {
                char typo = (char) (random.nextInt(26) + 'a');
                page.keyboard().type(String.valueOf(typo));

                randomSleep(page, 200, 400);
                page.keyboard().press("Backspace");
                randomSleep(page, 100, 200);
            }

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