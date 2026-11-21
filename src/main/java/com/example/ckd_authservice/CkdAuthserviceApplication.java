package com.example.ckd_authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.context.annotation.Import(com.ckd.auth.SecurityAutoConfiguration.class)
@SpringBootApplication(scanBasePackages = {
        "com.example.ckd_authservice",
        "controllers",
        "Database",
        "services"
})
public class CkdAuthserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CkdAuthserviceApplication.class, args);
    }

}
