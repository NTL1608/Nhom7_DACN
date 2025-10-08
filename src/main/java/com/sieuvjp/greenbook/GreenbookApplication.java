package com.sieuvjp.greenbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;


@SpringBootApplication
public class GreenbookApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreenbookApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        String url = "http://localhost:8080";
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                // Windows
                Runtime.getRuntime().exec("cmd /c start " + url);
            }
            System.out.println("Browser opened at: " + url);
        } catch (Exception e) {
            System.err.println("Failed to open browser: " + e.getMessage());
        }
    }
}