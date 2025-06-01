package com.openclassrooms.tourguide.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import gpsUtil.location.VisitedLocation;
import lombok.Getter;
import lombok.Setter;
import tripPricer.Provider;

/**
 * Représente un utilisateur dans le système TourGuide.
 * 
 * Cette classe stocke les informations personnelles de l'utilisateur, 
 * ses préférences, ses localisations visitées, ses récompenses, et les offres de voyages.
 */
@Getter
public class User {
    
    /**
     * Identifiant unique de l'utilisateur.
     */
    private final UUID userId;
    
    /**
     * Nom d'utilisateur.
     */
    private final String userName;
    
    /**
     * Numéro de téléphone de l'utilisateur.
     */
    @Setter
    private String phoneNumber;
    
    /**
     * Adresse email de l'utilisateur.
     */
    @Setter
    private String emailAddress;
    
    /**
     * Date et heure de la dernière mise à jour de la localisation de l'utilisateur.
     */
    @Setter
    private LocalDateTime latestLocationTimestamp;
    
    /**
     * Liste des localisations visitées par l'utilisateur.
     */
    private List<VisitedLocation> visitedLocations = new ArrayList<>();
    
    /**
     * Liste des récompenses obtenues par l'utilisateur.
     */
    private List<UserReward> userRewards = new ArrayList<>();
    
    /**
     * Préférences personnelles de l'utilisateur.
     */
    @Setter
    private UserPreferences userPreferences = new UserPreferences();
    
    /**
     * Liste des offres de voyages (trip deals) disponibles pour l'utilisateur.
     */
    @Setter
    private List<Provider> tripDeals = new ArrayList<>();
    
    /**
     * Constructeur principal.
     * 
     * @param userId Identifiant unique de l'utilisateur
     * @param userName Nom d'utilisateur
     * @param phoneNumber Numéro de téléphone
     * @param emailAddress Adresse email
     */
    public User(UUID userId, String userName, String phoneNumber, String emailAddress) {
        this.userId = userId;
        this.userName = userName;
        this.phoneNumber = phoneNumber;
        this.emailAddress = emailAddress;
    }
    
    /**
     * Ajoute une nouvelle localisation visitée à la liste.
     * 
     * @param visitedLocation Localisation visitée à ajouter
     */
    public void addToVisitedLocations(VisitedLocation visitedLocation) {
        visitedLocations.add(visitedLocation);
    }
    
    /**
     * Vide la liste des localisations visitées.
     */
    public void clearVisitedLocations() {
        visitedLocations.clear();
    }
    
    /**
     * Ajoute une récompense utilisateur à la liste des récompenses,
     * uniquement si une récompense avec la même attraction n'existe pas déjà.
     *
     * @param userReward La récompense utilisateur à ajouter.
     */
    public void addUserReward(UserReward userReward) {
        boolean alreadyExists = userRewards.stream()
            .anyMatch(r -> r.attraction.attractionName.equals(userReward.attraction.attractionName));
        if (!alreadyExists) {
            userRewards.add(userReward);
        }
    }

    
    /**
     * Récupère la dernière localisation visitée.
     * 
     * @return La dernière localisation dans la liste des localisations visitées
     * @throws IndexOutOfBoundsException si la liste est vide
     */
    public VisitedLocation getLastVisitedLocation() {
        return visitedLocations.get(visitedLocations.size() - 1);
    }
}
