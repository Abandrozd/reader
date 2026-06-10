package com.example;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.options.Proxy;

import java.nio.file.Paths;

import net.datafaker.Faker;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class test {
    public static void main(String[] args) {
        int threadCount = 10; // Сколько инстансов запускаем одновременно
        
        // Создаем пул потоков
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int taskId = i; // Сохраняем номер задачи для логов
            
            // Запускаем асинхронную задачу в пуле потоков
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try (Playwright playwright = Playwright.create()) {
                    
                    Proxy proxy = new Proxy("http://91.188.241.58:9449")
                                .setUsername("pYEZ3d")
                                .setPassword("9mZApC");

                    System.out.println("Поток " + taskId + " запускает браузер...");
                    Browser browser = playwright.chromium()
                            .launch(new BrowserType.LaunchOptions()
                                    .setHeadless(false)
                                    .setProxy(proxy)
                            );

                    Page page = browser.newPage();

                    page.navigate("https://api.ipify.org", new Page.NavigateOptions().setTimeout(60000));
                    String ip = page.locator("body").innerText();
                    System.out.println("Поток " + taskId + " получил IP: " + ip);

                    Random random = new Random();
                    int randomTimeout = random.nextInt(1200, 2800);
                    System.out.println("Поток " + taskId + " спит " + randomTimeout + " мс");
                    
                    // Имитируем ожидание
                    Thread.sleep(randomTimeout);

                } catch (Exception e) {
                    System.err.println("Ошибка в потоке " + taskId + ": " + e.getMessage());
                }
            }, executor);
            
            futures.add(future);
        }

        // try (Playwright playwright = Playwright.create()) {
        //         Proxy proxy = new Proxy("http://91.188.242.71:9779").setUsername("pYEZ3d").setPassword("9mZApC");

        //         Browser browser = playwright.chromium()
        //                 .launch(new BrowserType.LaunchOptions().setProxy(proxy));

        //         Page page = browser.newPage();

        //         page.navigate("https://api.ipify.org", new Page.NavigateOptions().setTimeout(10000));
        //         String ip = page.locator("body").innerText();
        //         System.out.println("IP: " + ip);

        //         Random random = new Random();
        //         int randomTimeout = random.nextInt(100, 200);
        //         System.out.println(randomTimeout);
        //     }

        // Блокируем основной поток, пока все 3 задачи не завершатся
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Закрываем пул потоков
        executor.shutdown();
        System.out.println("Все браузеры завершили работу!");
    }

    public static String getRealisticChromeUA() {
        Random random = new Random();

        // Список популярных ОС
        String[] osList = {
                "Windows NT 10.0; Win64; x64", // Windows 10/11
                "Macintosh; Intel Mac OS X 10_15_7", // Современный Mac
                "X11; Linux x86_64" // Linux
        };

        String os;

        int num = random.nextInt(100);
        if (num < 70) {
            os = osList[0];
        } else if (num < 90) {
            os = osList[1];
        } else {
            os = osList[2];
        }

        int chromeVersion = random.nextInt(120, 126);
        int patch = random.nextInt(6000, 6500);
        int build = random.nextInt(50, 150);
        return String.format("Mozilla/5.0 (%s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%d.0.%d.%d Safari/537.36",
                os, chromeVersion, patch, build);
    }
}
