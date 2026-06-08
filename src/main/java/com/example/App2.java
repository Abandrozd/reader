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

import java.nio.file.Paths;

import net.datafaker.Faker;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.HashMap;
import java.util.Map;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class App2 {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/playwright_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "sugoma";

    public static void main(String[] args) {
        int targetId = 2;

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Успешное подключение");

            // Знаком вопроса '?' мы помечаем место, куда Java подставит ID
            String sql = "SELECT id, email, password, user_agent, width, height FROM accounts WHERE id = ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

                // targetId вместо первого знака вопроса в запросе
                preparedStatement.setInt(1, targetId);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {

                    // Используем IF вместо WHILE. Нам нужна только одна строка!
                    if (resultSet.next()) {

                        // Вытаскиваем данные этого конкретного аккаунта
                        String email = resultSet.getString("email");
                        String password = resultSet.getString("password");
                        String randomUA = resultSet.getString("user_agent");
                        int randomWidth = resultSet.getInt("width");
                        int randomHeight = resultSet.getInt("height");

                        System.out.println("\nID " + targetId + " (" + email + ")");

                        // Инициализируем Playwright
                        try (Playwright playwright = Playwright.create()) {
                            Proxy proxy = new Proxy("http://91.188.241.58:9449").setUsername("pYEZ3d")
                                    .setPassword("9mZApC");
                            Browser browser = playwright.chromium().launch(
                                    new BrowserType.LaunchOptions()
                                            .setProxy(proxy)
                                            .setHeadless(false)
                                            .setArgs(java.util.Arrays.asList(
                                                    "--disable-blink-features=AutomationControlled",
                                                    "--disable-infobars")));

                            Random random = new Random();
                            Map<String, String> extraHeaders = new HashMap<>();
                            extraHeaders.put("Sec-CH-UA-Mobile", "?0");
                            extraHeaders.put("Sec-CH-UA-Platform", "\"Windows\"");

                            Browser.NewContextOptions options = new Browser.NewContextOptions()
                                    .setUserAgent(randomUA)
                                    .setViewportSize(randomWidth, randomHeight)
                                    .setExtraHTTPHeaders(extraHeaders);

                            try (BrowserContext context = browser.newContext(options)) {
                                Page page = context.newPage();

                                Reader reader = new Reader();

                                // сам сценарий Playwright
                                page.navigate("https://litmarket.ru/");

                                double[] mousePos = { random.nextInt(randomWidth), random.nextInt(randomHeight) };
                                page.mouse().move(mousePos[0], mousePos[1]);

                                humanClick(page, page.locator(".login-btn"), mousePos);
                                humanType(page, page.locator("input[name='email']"), email, mousePos);
                                humanType(page, page.locator("input[name='password']"), password, mousePos);
                                randomSleep(page, 200, 400);

                                humanClick(page, page.locator(".authLoginButton"), mousePos);

                                Locator avatarBtn = page.locator("a[href='#userPages'] .user-avatar").first();

                                // ожидание прогрузки аватарки
                                avatarBtn.waitFor(new Locator.WaitForOptions()
                                        .setState(WaitForSelectorState.VISIBLE)
                                        .setTimeout(10000));

                                page.navigate(
                                        "https://litmarket.ru/books/selskaya-celitelnica-ivi");

                                page.locator(".like-button").first()
                                        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));

                                Locator likeBtn = page.locator(".like-button:visible").first();

                                String isLiked = likeBtn.getAttribute("aria-pressed");
                                System.out.println(isLiked);

                                if (likeBtn.getAttribute("aria-label").equals("Поставить лайк")) {
                                    humanClick(page, likeBtn, mousePos);
                                    randomSleep(page, 300, 600);
                                    humanClick(page, page.getByLabel("Закрыть окно"), mousePos);
                                } else {
                                    System.out.println("Лайк уже стоит");
                                }

                                Locator libraryBtn = page.getByLabel("Добавить книгу в библиотеку").first();

                                if (libraryBtn.isVisible()) {
                                    humanClick(page, libraryBtn, mousePos);
                                    randomSleep(page, 300, 500);
                                    humanClick(page, page.locator("button[data-shelf-name='Читаю']"), mousePos);
                                    randomSleep(page, 300, 500);
                                } else {
                                    System.out.println("Книга уже стоит");
                                }

                                humanClick(page, page.locator(".btn-reader a"), mousePos);

                                reader.simulateReading(page);

                                randomSleep(page, 300, 600);
                                // humanClick(page, avatarBtn, mousePos);

                                System.out.println("Успешное завершение теста - " + email);
                            }
                            browser.close();
                        }

                    } else {
                        System.out.println("Аккаунт с ID " + targetId + " не существует");
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("БД вылетело");
            e.printStackTrace();
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