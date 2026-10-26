package com.example.ckd_authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.example.ckd_authservice",
        "Controllers",
        "Database",
        "services"
})
public class CkdAuthserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CkdAuthserviceApplication.class, args);
    }

}
