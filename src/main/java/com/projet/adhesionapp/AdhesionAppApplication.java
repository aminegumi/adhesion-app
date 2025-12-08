package com.projet.adhesionapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AdhesionAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdhesionAppApplication.class, args);
    }

}
