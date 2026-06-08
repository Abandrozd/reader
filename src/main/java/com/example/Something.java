package com.example;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.BrowserContext;

import java.nio.file.Paths;

import net.datafaker.Faker;
import java.util.Locale;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Something {
    public static void main(String[] args) {
        Faker faker = new Faker(Locale.of("ru"));

        String fileName = "users.txt";
        Path filePath = Paths.get(fileName);

        long currentLines = 0;

        try {
            if (Files.exists(filePath)) {
                currentLines = Files.lines(filePath).count();
            }
        } catch (IOException err) {
            System.out.println("Не удалось прочитать файл: " + err.getMessage());
        }

        try (FileWriter fw = new FileWriter(fileName);
                BufferedWriter writer = new BufferedWriter(fw)) {
            writer.write("ID;ФИО;Email;Телефон;Город;Профессия\n");

            for (int i = 1; i <= 100; i++) {
                String str = "";

                String firstName = faker.name().firstName();
                String lastName = faker.name().lastName();
                String email = transliterate(firstName.toLowerCase()) + "_" + transliterate(lastName.toLowerCase()) + i
                        + faker.internet().emailAddress();

                String randomUserAgent = faker.internet().userAgent();

                System.out.println(randomUserAgent);

                String phone = faker.phoneNumber().cellPhone();
                String city = faker.address().city();
                String profession = faker.job().title();

                str = i + " | " + firstName + " " + lastName + " | " + email + " | " + phone + " | " + city + " | "
                        + profession;
                writer.write(str + '\n');
            }

            System.out.println("Успешное завершение генерации! Файл: " + fileName);

        } catch (IOException e) {
            System.out.println("Ошибка при записании файла" + e.getMessage());
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
}
