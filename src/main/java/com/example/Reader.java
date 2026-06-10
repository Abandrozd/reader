package com.example;

import com.microsoft.playwright.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Random;
import java.util.List;

public class Reader {
    private static final Random random = new Random();

    public static void simulateReading(Page page) {
        boolean hasNextChapter = true;
        int chapterCount = 1;

        while (hasNextChapter) {
            System.out.println("Начинаем читать главу " + chapterCount);

            // Ждем появления текста новой главы
            page.locator("p[data-index]").first().waitFor(new Locator.WaitForOptions().setTimeout(10000));

            // Хранилище для индексов абзацев, которые мы уже прочитали в этой главе
            Set<String> readParagraphs = new HashSet<>();

            int emptyScrollCount = 0;
            int maxEmptyScrolls = 3;

            while (true) {
                // 5вытаскиваем только текстовые значения data-index,
                // которые есть в DOM прямо сейчас
                @SuppressWarnings("unchecked")
                List<String> availableIndices = (List<String>) page.evalOnSelectorAll(
                        "p[data-index]",
                        "elements => elements.map(el => el.getAttribute('data-index'))");

                boolean foundNewContent = false;

                for (String index : availableIndices) {
                    // Если индекс валидный и мы его еще не читали
                    if (index != null && !readParagraphs.contains(index)) {

                        // Создаем точечный локатор именно под этот уникальный ID абзаца
                        Locator p = page.locator("p[data-index='" + index + "']").first();

                        try {
                            // Плавно скроллим к конкретному абзацу
                            p.evaluate("node => node.scrollIntoView({ behavior: 'smooth', block: 'center' })");
                            page.waitForTimeout(100); // Микропауза для стабилизации интерфейса

                            String text = p.innerText();
                            int textLength = text.length();

                            // Логика симуляции чтения
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

                            // Отмечаем как прочитанный
                            readParagraphs.add(index);
                            foundNewContent = true;
                            emptyScrollCount = 0;

                            // ВАЖНО: прерываем внутренний FOR и обновляем список доступных индексов,
                            // так как из-за скролла DOM-дерево могло измениться
                            break;

                        } catch (Exception e) {
                            // На случай, если элемент исчез из DOM прямо во время взаимодействия
                            System.out.println("Абзац " + index + " временно недоступен, обновляем поиск...");
                            break;
                        }
                    }
                }

                // Если за весь проход по видимым элементам не нашлось новых
                if (!foundNewContent) {
                    page.mouse().wheel(0, 800);
                    page.waitForTimeout(1500); // Ждем подгрузку по сети

                    emptyScrollCount++;
                    System.out.println("Ждем подгрузки новых абзацев... Попытка " + emptyScrollCount);

                    if (emptyScrollCount >= maxEmptyScrolls) {
                        System.out.println("Абзацы закончились. Глава прочитана!");
                        break;
                    }
                }
            }

            // --- БЛОК НАВИГАЦИИ МЕЖДУ ГЛАВАМИ ---
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
                System.out.println("Кнопка 'Далее' не найдена. Книга закончилась");
                hasNextChapter = false;
            }
        }
    }
}