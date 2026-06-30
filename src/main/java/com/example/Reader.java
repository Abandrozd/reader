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
            page.locator("p[data-index]").first().waitFor(new Locator.WaitForOptions().setTimeout(25000));

            // Хранилище для индексов абзацев, которые мы уже прочитали в этой главе
            Set<String> readParagraphs = new HashSet<>();

            int emptyScrollCount = 0;
            int maxEmptyScrolls = 3;

            while (true) {
                // ================================
                // 1. ПРОВЕРКА "КРИТИЧЕСКОЙ" ПЛАШКИ (Выход из чтения)
                // ================================
                // Проверяем, не появилась ли именно плашка подарка (giftSelectionModal)
                Locator giftModalCloseBtn = page.locator(".giftSelectionModal .lmSimpleModal__close:visible").first();
                if (giftModalCloseBtn.count() > 0) {
                    System.out.println(
                            "Появилась плашка giftSelectionModal! Закрываем её и ПОЛНОСТЬЮ выходим из чтения.");
                    giftModalCloseBtn.click();
                    page.waitForTimeout(500);
                    return; // Команда return моментально завершает метод simulateReading. Чтение
                            // прекращается.
                }

                // ================================
                // 2. ОБРАБОТКА ПОЛОК И ОБЫЧНЫХ МОДАЛОК (Без прерывания)
                // ================================
                Locator shelfButtons = page.locator("button.shelve-button:visible");
                if (shelfButtons.count() > 0) {
                    List<String> names = List.of("Читаю", "Прочитать", "Прочитано", "Избранное");
                    String name = names.get(random.nextInt(names.size()));
                    Locator targetShelf = page.locator("button.shelve-button:has-text('" + name + "'):visible").first();

                    if (targetShelf.count() > 0) {
                        System.out
                                .println("Появилась плашка выбора полки! Выбираем: " + name + " и продолжаем чтение.");
                        targetShelf.click();
                        page.waitForTimeout(1000);
                    }
                } else {
                    // Если это НЕ подарочная плашка и НЕ полка, закроем её как обычную
                    Locator closeModalBtn = page.locator(".lmSimpleModal__close:visible").first();
                    if (closeModalBtn.count() > 0) {
                        System.out.println("Появилась обычная модалка. Закрываем и продолжаем.");
                        closeModalBtn.click();
                        page.waitForTimeout(500);
                    }
                }
                // ================================

                // вытаскиваем только текстовые значения data-index,
                // которые есть в DOM прямо сейчас
                @SuppressWarnings("unchecked")
                List<String> availableIndices = (List<String>) page.evalOnSelectorAll(
                        "p[data-index]",
                        "elements => elements.map(el => el.getAttribute('data-index'))");

                boolean foundNewContent = false;

                for (String index : availableIndices) {
                    // Если индекс валидный и мы его еще не читали
                    if (index != null && !readParagraphs.contains(index)) {

                        Locator p = page.locator("p[data-index='" + index + "']").first();

                        try {
                            p.evaluate("node => node.scrollIntoView({ behavior: 'smooth', block: 'center' })");
                            page.waitForTimeout(100);

                            String text = p.innerText();
                            int textLength = text.length();

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

                            readParagraphs.add(index);
                            foundNewContent = true;
                            emptyScrollCount = 0;
                            break;

                        } catch (Exception e) {
                            System.out.println("Абзац " + index + " временно недоступен, обновляем поиск...");
                            break;
                        }
                    }
                }

                if (!foundNewContent) {
                    page.mouse().wheel(0, 800);
                    page.waitForTimeout(1500);

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