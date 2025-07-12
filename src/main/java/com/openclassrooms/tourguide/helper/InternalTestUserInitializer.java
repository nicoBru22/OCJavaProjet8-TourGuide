package com.openclassrooms.tourguide.helper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

import com.openclassrooms.tourguide.user.User;

import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

/**
 * Classe utilitaire pour initialiser un ensemble d'utilisateurs internes
 * destinés aux tests.
 * <p>
 * Cette classe génère un nombre spécifié d'utilisateurs avec des données
 * fictives, notamment des historiques de localisation aléatoires.
 * </p>
 */
public class InternalTestUserInitializer {

    /**
     * Initialise une carte d'utilisateurs internes avec un nombre donné d'utilisateurs.
     * Chaque utilisateur possède un identifiant unique, un nom, un numéro de téléphone fictif,
     * une adresse email et un historique de trois localisations visitées générées aléatoirement.
     *
     * @param numberOfUsers le nombre d'utilisateurs internes à créer
     * @return une Map associant le nom de l'utilisateur à l'objet User correspondant
     */
    public Map<String, User> initializeUsers(int numberOfUsers) {
        Map<String, User> internalUserMap = new HashMap<>();

        IntStream.range(0, numberOfUsers).forEach(i -> {
            String userName = "internalUser" + i;
            User user = new User(UUID.randomUUID(), userName, "000", userName + "@tourGuide.com");
            generateUserLocationHistory(user);
            internalUserMap.put(userName, user);
        });

        return internalUserMap;
    }

    /**
     * Génère un historique de localisation fictif pour un utilisateur donné.
     * Trois localisations aléatoires sont ajoutées à la liste des localisations visitées.
     *
     * @param user l'utilisateur pour lequel générer l'historique
     */
    private void generateUserLocationHistory(User user) {
        IntStream.range(0, 3).forEach(i -> {
            user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
                new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
        });
    }

    /**
     * Génère une longitude aléatoire valide comprise entre -180 et +180 degrés.
     *
     * @return une longitude aléatoire
     */
    private double generateRandomLongitude() {
        return -180 + new Random().nextDouble() * 360;
    }

    /**
     * Génère une latitude aléatoire valide comprise entre -85.05112878 et +85.05112878 degrés.
     *
     * @return une latitude aléatoire
     */
    private double generateRandomLatitude() {
        return -85.05112878 + new Random().nextDouble() * 170.10225756;
    }

    /**
     * Génère une date aléatoire dans les 30 derniers jours par rapport à la date actuelle.
     *
     * @return une date aléatoire récente
     */
    private Date getRandomTime() {
        LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }
}
