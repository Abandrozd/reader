package com.example;

import com.microsoft.playwright.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Random;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;

public class Reader {
    private static final Random random = new Random();

    public static void simulateReading(Page page) {
        boolean hasNextChapter = true;
        int chapterCount = 1;

        while (hasNextChapter) {
            System.out.println("Начинаем читать главу " + chapterCount);

            // 1. Ждем появления текста новой главы
            page.locator("p[data-index]").first().waitFor(new Locator.WaitForOptions().setTimeout(10000));

            // Хранилище для индексов абзацев, которые мы уже прочитали в этой главе
            Set<String> readParagraphs = new HashSet<>();

            // Счетчик пустых прокруток (чтобы понять, что мы дошли до конца текста)
            int emptyScrollCount = 0;
            int maxEmptyScrolls = 3;

            while (true) {
                // Собираем ВСЕ абзацы, которые сейчас физически есть в HTML-коде
                Locator allParagraphs = page.locator("p[data-index]");
                int count = allParagraphs.count();
                boolean foundNewContent = false;

                for (int i = 0; i < count; i++) {
                    Locator p = allParagraphs.nth(i);
                    String index = p.getAttribute("data-index");

                    // Если индекс есть и мы этот абзац еще НЕ читали
                    if (index != null && !readParagraphs.contains(index)) {

                        // Плавно скроллим к нему
                        p.evaluate("node => node.scrollIntoView({ behavior: 'smooth', block: 'center' })");
                        page.waitForTimeout(100); // Микропауза для браузера

                        String text = p.innerText();
                        int textLength = text.length();

                        // Логика чтения
                        if (textLength > 5) {
                            double charsPerSecond = 30.0 + (random.nextDouble() * 15.0);
                            int readingTimeMs = (int) ((textLength / charsPerSecond) * 1000);
                            int extraPause = random.nextInt(300, 1000);

                            System.out.printf("Читаем абзац %s (%d симв.) -> %d мс\n", index, textLength,
                                    readingTimeMs + extraPause);
                            page.waitForTimeout(readingTimeMs + extraPause);
                        } else {
                            page.waitForTimeout(200);
                        }

                        // Записываем абзац в прочитанные
                        readParagraphs.add(index);
                        foundNewContent = true;
                        emptyScrollCount = 0; // Сбрасываем счетчик пустого скролла
                    }
                }

                // Если за весь цикл мы не нашли новых абзацев — возможно, нужно проскроллить
                // вниз вручную
                if (!foundNewContent) {
                    // Крутим колесико мыши вниз на 800 пикселей
                    page.mouse().wheel(0, 800);
                    page.waitForTimeout(1500); // Ждем, пока сайт подгрузит новые абзацы по сети

                    emptyScrollCount++;
                    System.out.println("Ждем подгрузки новых абзацев... Попытка " + emptyScrollCount);

                    // Если мы несколько раз проскроллили, а текста всё нет — глава закончилась
                    if (emptyScrollCount >= maxEmptyScrolls) {
                        System.out.println("Абзацы закончились. Глава прочитана!");
                        break; // Выходим из цикла чтения главы
                    }
                }
            }

            // --- БЛОК НАВИГАЦИИ (Твой код, он отличный) ---
            Locator navBlock = page.locator(".chapter-nav").last();
            navBlock.scrollIntoViewIfNeeded();

            page.waitForTimeout(random.nextInt(1500, 3000));

            Locator nextBtn = page.locator(".chapter-nav__right")
                    .filter(new Locator.FilterOptions().setHasText("Далее")).first();

            if (nextBtn.count() > 0 && nextBtn.isVisible()) {
                System.out.println("Переходим к следующей главе...");
                nextBtn.click();
                page.waitForLoadState();
                chapterCount++;
            } else {
                System.out.println("Кнопка 'Далее' не найдена. Похоже, книга закончилась!");
                hasNextChapter = false;
            }
        }
    }
}
