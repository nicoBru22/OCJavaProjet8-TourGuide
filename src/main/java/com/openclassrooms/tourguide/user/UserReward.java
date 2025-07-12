package com.openclassrooms.tourguide.user;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Représente une récompense attribuée à un utilisateur,
 * associant un lieu visité à une attraction et un nombre de points de récompense.
 * 
 * Cette classe possède deux constructeurs générés automatiquement :
 * <ul>
 *   <li>Un constructeur avec les champs finals {@code visitedLocation} et {@code attraction} (via {@link RequiredArgsConstructor})</li>
 *   <li>Un constructeur avec tous les champs : {@code visitedLocation}, {@code attraction} et {@code rewardPoints} (via {@link AllArgsConstructor})</li>
 * </ul>
 */
@AllArgsConstructor
@RequiredArgsConstructor
public class UserReward {

    /**
     * La localisation visitée par l'utilisateur.
     */
    public final VisitedLocation visitedLocation;

    /**
     * L'attraction associée à cette récompense.
     */
    public final Attraction attraction;

    /**
     * Le nombre de points de récompense attribués.
     */
    @Getter
    @Setter
    private int rewardPoints;

}
