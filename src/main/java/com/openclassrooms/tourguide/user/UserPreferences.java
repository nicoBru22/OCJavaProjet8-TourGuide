package com.openclassrooms.tourguide.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Représente les préférences d'un utilisateur concernant ses voyages et attractions.
 * Cette classe permet de définir des paramètres personnalisés utilisés dans les recommandations.
 * 
 * <p>Les préférences incluent :</p>
 * <ul>
 *   <li>La proximité des attractions (en mètres)</li>
 *   <li>La durée du voyage (en jours)</li>
 *   <li>La quantité de billets souhaitée</li>
 *   <li>Le nombre d'adultes participants</li>
 *   <li>Le nombre d'enfants participants</li>
 * </ul>
 * 
 * <p>Les valeurs par défaut sont :</p>
 * <ul>
 *   <li>Proximité attraction : {@code Integer.MAX_VALUE} (illimitée)</li>
 *   <li>Durée du voyage : 1 jour</li>
 *   <li>Quantité de billets : 1</li>
 *   <li>Nombre d'adultes : 1</li>
 *   <li>Nombre d'enfants : 0</li>
 * </ul>
 * 
 * @author 
 */
@Getter
@Setter
@NoArgsConstructor
public class UserPreferences {
	
	/**
	 * La distance maximale (en mètres) à laquelle une attraction est considérée proche.
	 */
	private int attractionProximity = Integer.MAX_VALUE;
	
	/**
	 * La durée prévue du voyage en nombre de jours.
	 */
	private int tripDuration = 1;
	
	/**
	 * La quantité de billets souhaitée pour le voyage.
	 */
	private int ticketQuantity = 1;
	
	/**
	 * Le nombre d'adultes participant au voyage.
	 */
	private int numberOfAdults = 1;
	
	/**
	 * Le nombre d'enfants participant au voyage.
	 */
	private int numberOfChildren = 0;

}
