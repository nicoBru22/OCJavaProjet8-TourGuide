package com.openclassrooms.tourguide;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principale de l'application TourGuide.
 * 
 * Cette classe contient la méthode main qui démarre l'application Spring Boot.
 */
@SpringBootApplication
public class TourguideApplication {

    /**
     * Point d'entrée de l'application.
     * 
     * @param args Arguments de la ligne de commande.
     */
    public static void main(String[] args) {
        SpringApplication.run(TourguideApplication.class, args);
    }

}
