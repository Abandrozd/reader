package com.example;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Proxy;
import com.microsoft.playwright.options.WaitForSelectorState;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class App2 {

    public static void main(String[] args) {
        Path accountsFile = Paths.get("accounts.txt");
        Path proxiesFile = Paths.get("proxies.txt"); // Файл с прокси

        // ==========================================
        // ЧТЕНИЕ ПРОКСИ ИЗ ФАЙЛА
        // ==========================================
        List<List<String>> proxies = new ArrayList<>();
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

        Scanner scanner = new Scanner(System.in);
        System.out.print("ID аккаунтов на чтение: ");
        String input = scanner.nextLine();

        List<Integer> targetIds = new ArrayList<>();
        String[] inputParts = input.split("[,\\s]+");
        for (String part : inputParts) {
            String trim = part.trim();
            if (!trim.isEmpty()) {
                try {
                    targetIds.add(Integer.parseInt(trim));
                } catch (Exception e) {
                    System.err.println(trim + " не является числом");
                }
            }
        }

        if (targetIds.isEmpty()) {
            System.out.println("Все ID неверны");
            return;
        }

        if (!Files.exists(accountsFile)) {
            System.err.println("Ошибка / accounts.txt не найден в корне проекта");
            return;
        }

        List<String[]> validAccounts = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(accountsFile);
            for (String line : lines) {
                if (line.trim().isEmpty())
                    continue;
                String[] parts = line.split("\\s*\\|\\s*");
                if (parts.length == 6) {
                    validAccounts.add(parts);
                } else {
                    System.err.println("Пропущена строка " + line + " / формат неточен");
                }
            }

        } catch (IOException e) {
            System.out.println("Ошибка при чтении файла accounts.txt");
            e.printStackTrace();
            return;
        }

        List<String[]> accountsToRun = new ArrayList<>();
        List<Integer> proxyIndexes = new ArrayList<>();

        for (int i = 0; i < validAccounts.size(); i++) {
            String[] acc = validAccounts.get(i);
            try {
                // Берем ID из первой колонки (до первого "|")
                int accountId = Integer.parseInt(acc[0].trim());
                if (targetIds.contains(accountId)) {
                    accountsToRun.add(acc);
                    proxyIndexes.add(i); // Запоминаем номер строки для привязки правильного прокси
                }
            } catch (NumberFormatException e) {
                System.err.println("Ошибка парсинга ID аккаунта: " + acc[0]);
            }
        }

        int threadCount = accountsToRun.size();
        if (threadCount == 0) {
            System.out.println("Аккаунты с указанными ID не найдены.");
            return;
        }

        System.out.println("Запущено " + threadCount + " потоков");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int num = 0; num < threadCount; num++) {
            final int taskId = num;
            final int originalIndex = proxyIndexes.get(taskId);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String[] accountData = accountsToRun.get(taskId);
                    String email = accountData[1];
                    String password = accountData[2];
                    String randomUA = accountData[3];
                    int randomWidth = Integer.parseInt(accountData[4]);
                    int randomHeight = Integer.parseInt(accountData[5]);

                    System.out.println("\nПоток " + taskId + " начал работу с " + email);

                    try (Playwright playwright = Playwright.create()) {
                        List<String> proxyData = proxies.get(originalIndex % proxies.size());
                        String proxyIp = proxyData.get(0);
                        String proxyUser = proxyData.get(1);
                        String proxyPass = proxyData.get(2);

                        Proxy proxy = new Proxy("http://" + proxyIp)
                                .setUsername(proxyUser)
                                .setPassword(proxyPass);

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

                            // Предполагается, что класс Reader существует в вашем проекте
                            Reader reader = new Reader();

                            // Сам сценарий Playwright
                            page.navigate("https://litmarket.ru/");

                            double[] mousePos = { random.nextInt(randomWidth), random.nextInt(randomHeight) };
                            page.mouse().move(mousePos[0], mousePos[1]);

                            humanClick(page, page.locator(".login-btn"), mousePos);
                            humanType(page, page.locator("input[name='email']"), email, mousePos);
                            humanType(page, page.locator("input[name='password']"), password, mousePos);
                            randomSleep(page, 200, 400);

                            humanClick(page, page.locator(".authLoginButton"), mousePos);

                            try {
                                page.locator("a[href='#userPages'] .user-avatar").first()
                                        .waitFor(new Locator.WaitForOptions().setTimeout(10000));
                                System.out.println("Успешный вход: аватарка найдена.");
                            } catch (TimeoutError e) {
                                System.out.println("ERROR / Аватарка не появилась");
                                throw e;
                            }

                            page.navigate(
                                    "https://litmarket.ru/books/izgnannaya-luna-alfy-ochag-dlya-ego-poteryannyh-volchat");

                            page.locator(".like-button").first()
                                    .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));

                            // ==========================================
                            // 1. БЛОК ЛАЙКА
                            // ==========================================
                            System.out.println("--- Проверка лайка ---");
                            Locator likeBtn = page
                                    .locator(".card-actions:not(.card-actions-book-mobile) .like-button:visible")
                                    .first();

                            if (likeBtn.isVisible()) {
                                String ariaPressed = likeBtn.getAttribute("aria-pressed");

                                if ("true".equals(ariaPressed)) {
                                    System.out.println("Лайк уже стоит");
                                } else {
                                    System.out.println("Попытка поставить лайк...");
                                    humanClick(page, likeBtn, mousePos);

                                    try {
                                        assertThat(likeBtn).hasAttribute("aria-pressed", "true",
                                                new LocatorAssertions.HasAttributeOptions().setTimeout(5000));
                                        System.out.println("Лайк успешно поставлен!");
                                    } catch (AssertionError e) {
                                        System.out.println(
                                                "ОШИБКА: Клик по лайку прошел, но сайт не засчитал его (aria-pressed остался false).");
                                    }
                                }
                            } else {
                                System.out.println("ОШИБКА: Кнопка лайка вообще не найдена или невидима.");
                            }

                            Locator closeBtn = page.locator("button[aria-label='Закрыть окно']:visible").first();
                            if (closeBtn.count() > 0) {
                                System.out.println("Найдено окно, закрываем...");
                                humanClick(page, closeBtn, mousePos);
                            }

                            // ==========================================
                            // 2. БЛОК ДОБАВЛЕНИЯ НА ПОЛКУ
                            // ==========================================
                            System.out.println("--- Проверка библиотеки ---");
                            Locator libraryBtn = page.getByLabel("Добавить книгу в библиотеку").first();
                            Locator libraryContainer = page.locator(".library-button").first();

                            if (!libraryContainer.isVisible()) {
                                System.out.println("ERROR / Блок библиотеки вообще не найден на странице!");
                            } else {
                                Locator loginRequiredBtn = libraryContainer
                                        .locator("[aria-label='Добавить в библиотеку (требуется вход)']");
                                Locator addBtn = libraryContainer.getByLabel("Добавить книгу в библиотеку");

                                if (loginRequiredBtn.isVisible()) {
                                    System.out.println(
                                            "ERROR / Невозможно добавить книгу — слетела авторизация (требуется вход).");
                                } else if (addBtn.isVisible()) {
                                    System.out.println("Открывается меню полок...");
                                    humanClick(page, addBtn, mousePos);

                                    List<String> names = List.of("Читаю", "Прочитать", "Прочитано", "Избранное");
                                    String name = names.get(random.nextInt(names.size()));
                                    Locator shelfButton = page.locator("button[data-shelf-name='" + name + "']:visible")
                                            .first();

                                    try {
                                        shelfButton.waitFor(new Locator.WaitForOptions()
                                                .setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
                                        System.out.println("Меню открылось. Выбираем полку: [" + name + "]");

                                        humanClick(page, shelfButton, mousePos);

                                        shelfButton.waitFor(new Locator.WaitForOptions()
                                                .setState(WaitForSelectorState.HIDDEN).setTimeout(5000));
                                        System.out.println("УСПЕХ: Книга добавлена на полку: " + name);

                                    } catch (TimeoutError e) {
                                        if (!shelfButton.isVisible()) {
                                            System.out.println(
                                                    "ERROR / Кликнули 'Добавить', но меню полок не появилось.");
                                        } else {
                                            System.out.println(
                                                    "ERROR / Кликнули по полке, но меню не закрылось (сервер не ответил).");
                                        }
                                    }
                                } else {
                                    String currentStatus = libraryContainer.textContent().trim();
                                    System.out.println(
                                            "ПРОПУСК: Книга УЖЕ находится в библиотеке. Текущий статус кнопки: ["
                                                    + currentStatus + "]");
                                }
                            }

                            humanClick(page, page.locator(".btn-reader a"), mousePos);

                            reader.simulateReading(page);

                            humanClick(page, page.locator("a.book-title"), mousePos);

                            randomSleep(page, 1300, 1600);

                            page.waitForLoadState(LoadState.NETWORKIDLE);

                            System.out.println("Успешное завершение теста - " + email);

                        }
                        browser.close();
                    }

                } catch (Exception e) {
                    System.err.println("Ошибка потока " + taskId);
                    e.printStackTrace();
                }
            }, executor);

            futures.add(future);

            try {
                Random random = new Random();
                int time = random.nextInt(1000, 3000);
                Thread.sleep(time);
            } catch (InterruptedException e) {
                System.out.println("Главный поток был прерван");
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        System.out.println("Скрипт закончился :D");
    }

    // =====================================================================
    // ВСЕ МЕТОДЫ НИЖЕ ОСТАЛИСЬ БЕЗ ИЗМЕНЕНИЙ
    // =====================================================================

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

        locator.evaluate("el => el.scrollIntoView({ behavior: 'smooth', block: 'center' })");

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