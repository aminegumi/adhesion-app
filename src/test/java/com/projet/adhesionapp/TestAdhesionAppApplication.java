package com.projet.adhesionapp;

import org.springframework.boot.SpringApplication;

public class TestAdhesionAppApplication {

    public static void main(String[] args) {
        SpringApplication.from(AdhesionAppApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
