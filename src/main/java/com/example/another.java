package com.example;

import com.microsoft.playwright.options.WaitUntilState;

import net.datafaker.Faker;

import com.microsoft.playwright.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class another {
    private static final Random random = new Random();

    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            // // Запускаем браузер в видимом режиме (headless = false)
            // Browser browser = playwright.chromium().launch(
            // new BrowserType.LaunchOptions().setHeadless(false));
            // Page page = browser.newPage();

            // System.out.println("Открываем jspaint.app...");
            // page.navigate("https://jspaint.app/", new
            // Page.NavigateOptions().setTimeout(60000));

            // // Ждем 3 секунды, чтобы страница и скрипты холста точно загрузились
            // page.waitForTimeout(3000);

            // System.out.println("Рисуем линию...");

            // // Координаты для рисования (где-то ближе к центру холста)
            // int startX = 200;
            // int startY = 300;
            // int targetX = 700;
            // int targetY = 450;

            // // 1. Наводим курсор на стартовую точку (пока без нажатия)
            // page.mouse().move(startX, startY);

            // // 2. Зажимаем левую кнопку мыши
            // page.mouse().down();

            // // 3. Ведем мышь в целевую точку "человеческим" алгоритмом
            // moveMouseHumanLike(page, startX, startY, targetX, targetY);

            // // 4. Отпускаем кнопку мыши
            // page.mouse().up();

            // System.out.println("Готово! Окно закроется через 5 секунд.");

            // // Даем вам время посмотреть на нарисованную линию
            // page.waitForTimeout(5000);

            Faker faker = new Faker();

            Map<String, String> browserProfile = getRealisticBrowserProfile();
            String randomUA = browserProfile.get("userAgent");
            System.out.println("Используем UA: " + randomUA);
        }
    }

    /**
     * Плавная имитация движения мыши с использованием Кубической Кривой Безье
     */
    public static void moveMouseHumanLike(Page page, double startX, double startY, double targetX, double targetY) {
        // 1. Генерируем случайные контрольные точки (магниты, искривляющие линию)
        // Они находятся примерно на 30% и 70% пути, но с сильным случайным смещением
        // вбок
        double controlX1 = startX + (targetX - startX) * 0.3 + (random.nextDouble() * 200 - 100);
        double controlY1 = startY + (targetY - startY) * 0.3 + (random.nextDouble() * 200 - 100);

        double controlX2 = startX + (targetX - startX) * 0.7 + (random.nextDouble() * 200 - 100);
        double controlY2 = startY + (targetY - startY) * 0.7 + (random.nextDouble() * 200 - 100);

        // 2. Случайное количество шагов (чем больше расстояние, тем больше можно
        // ставить)
        int steps = 50 + random.nextInt(50); // от 50 до 100 микро-шагов

        // 3. Движение по кривой
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;

            // Формула кубической кривой Безье для X и Y
            double x = Math.pow(1 - t, 3) * startX +
                    3 * Math.pow(1 - t, 2) * t * controlX1 +
                    3 * (1 - t) * Math.pow(t, 2) * controlX2 +
                    Math.pow(t, 3) * targetX;

            double y = Math.pow(1 - t, 3) * startY +
                    3 * Math.pow(1 - t, 2) * t * controlY1 +
                    3 * (1 - t) * Math.pow(t, 2) * controlY2 +
                    Math.pow(t, 3) * targetY;

            // Двигаем курсор в вычисленную точку
            page.mouse().move(x, y);

            // Неравномерные микро-паузы для имитации изменения скорости руки
            try {
                // В середине движения рука идет быстрее (пауза меньше), в начале и конце -
                // медленнее
                int delay = (t < 0.2 || t > 0.8) ? (5 + random.nextInt(10)) : (2 + random.nextInt(5));
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 4. Гарантированное попадание точно в цель в самом конце
        page.mouse().move(targetX, targetY);
    }

    public static Map<String, String> getRealisticBrowserProfile() {
        Random random = new Random();
        Map<String, String> profile = new HashMap<>();

        // 1. Фиксируем свежую версию Chrome (например, от 122 до 125)
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

        // 3. Формируем саму строку User-Agent
        // Обрати внимание: минорные версии Chrome сейчас часто нули, поэтому 0.0.0
        String userAgent = String.format(
                "Mozilla/5.0 (%s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%d.0.0.0 Safari/537.36",
                osString, chromeVersion);

        // 4. Формируем Client Hints (именно они спасают от палева на современных
        // сайтах)
        String secChUa = String.format("\"Google Chrome\";v=\"%d\", \"Chromium\";v=\"%d\", \"Not.A/Brand\";v=\"24\"",
                chromeVersion, chromeVersion);

        profile.put("userAgent", userAgent);
        profile.put("secChUa", secChUa);
        profile.put("secChUaPlatform", secChUaPlatform);
        profile.put("jsPlatform", jsPlatform);

        return profile;
    }
}