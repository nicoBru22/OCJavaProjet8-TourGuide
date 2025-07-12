package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.helper.InternalTestUserInitializer;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;
import tripPricer.TripPricer;

/**
 * Service principal de l'application TourGuide.
 * <p>
 * Ce service gère la localisation des utilisateurs, le calcul des récompenses,
 * la récupération des attractions proches, ainsi que les offres de voyages.
 * </p>
 * <p>
 * Il fonctionne en mode asynchrone pour suivre les utilisateurs et interagit avec les services
 * externes GPS et TripPricer.
 * </p>
 */
@Service
public class TourGuideService {

	/**
	 * Logger utilisé pour enregistrer les événements et messages de débogage du service.
	 */
	private static final Logger logger = LogManager.getLogger();

	/**
	 * Service GPS permettant de récupérer les localisations et les attractions.
	 */
	private final GpsUtil gpsUtil;

	/**
	 * Service chargé de gérer le calcul et l'ajout des récompenses utilisateur.
	 */
	private final RewardsService rewardsService;

	/**
	 * Service utilisé pour obtenir des prix de voyage auprès d'un fournisseur tiers.
	 */
	private final TripPricer tripPricer = new TripPricer();

	/**
	 * Composant responsable du suivi périodique de la localisation des utilisateurs.
	 */
	public final Tracker tracker;

	/**
	 * Indicateur permettant d'exécuter le service en mode test avec des utilisateurs simulés.
	 */
	private boolean testMode = true;

	/**
	 * Exécuteur de tâches asynchrones utilisant un pool fixe de 100 threads.
	 */
	private final ExecutorService executorService = Executors.newFixedThreadPool(100);

	/**
	 * Clé API utilisée pour interroger le fournisseur de prix de voyage TripPricer.
	 */
	private static final String tripPricerApiKey = "test-server-api-key";

	/**
	 * Dictionnaire interne des utilisateurs indexés par nom d'utilisateur.
	 */
	private final Map<String, User> internalUserMap = new HashMap<>();

	/**
	 * Composant chargé d'initialiser les utilisateurs fictifs lors de l'exécution en mode test.
	 */
	private final InternalTestUserInitializer internalTestUserInitializer = new InternalTestUserInitializer();


    /**
     * Constructeur du service TourGuide.
     * <p>
     * Initialise les services GPS, de récompenses, configure la locale par défaut,
     * initialise les utilisateurs internes en mode test,
     * et démarre le tracker de localisation utilisateur.
     * </p>
     *
     * @param gpsUtil        le service GPS permettant d'accéder aux attractions et localisations
     * @param rewardsService le service de gestion des récompenses utilisateur
     */
    public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
        this.gpsUtil = gpsUtil;
        this.rewardsService = rewardsService;

        Locale.setDefault(Locale.US);

        if (testMode) {
            logger.info("Mode test activé");
            logger.debug("Initialisation des utilisateurs internes");
            internalUserMap.putAll(internalTestUserInitializer.initializeUsers(InternalTestHelper.getInternalUserNumber()));
            logger.debug("Initialisation terminée");
        }
        tracker = new Tracker(this);
        addShutDownHook();
    }

    /**
     * Récupère la liste des récompenses de l'utilisateur.
     *
     * @param user utilisateur ciblé
     * @return liste des {@link UserReward} associés à l'utilisateur
     */
    public List<UserReward> getUserRewards(User user) {
        List<UserReward> listUserReward = user.getUserRewards();
        logger.info("Récompenses récupérées pour l'utilisateur : {}", listUserReward);
        return listUserReward;
    }

    /**
     * Obtient la dernière localisation visitée de l'utilisateur.
     * <p>
     * Si aucune localisation n'est enregistrée, effectue un suivi asynchrone pour récupérer
     * la position actuelle.
     * </p>
     *
     * @param user utilisateur dont on souhaite connaître la localisation
     * @return dernière localisation visitée ({@link VisitedLocation})
     * @throws InterruptedException si l'attente asynchrone est interrompue
     * @throws ExecutionException   si le calcul asynchrone échoue
     */
    public VisitedLocation getUserLocation(User user) throws InterruptedException, ExecutionException {
        VisitedLocation visitedLocation;
        if (user.getVisitedLocations().size() > 0) {
            visitedLocation = user.getLastVisitedLocation();
        } else {
            visitedLocation = trackUserLocation(user).get();
        }
        return visitedLocation;
    }

    /**
     * Recherche un utilisateur par son nom d'utilisateur.
     *
     * @param userName nom de l'utilisateur recherché
     * @return objet {@link User} correspondant, ou {@code null} si non trouvé
     */
    public User getUser(String userName) {
        User user = internalUserMap.get(userName);
        logger.info("Utilisateur récupéré : {}", user);
        return user;
    }

    /**
     * Récupère la liste de tous les utilisateurs connus.
     *
     * @return liste de tous les {@link User}
     */
    public List<User> getAllUsers() {
        List<User> userList = internalUserMap.values().stream()
                .collect(Collectors.toList());
        logger.info("Liste de tous les utilisateurs : {}", userList);
        return userList;
    }

    /**
     * Ajoute un nouvel utilisateur à la collection interne s'il n'existe pas déjà.
     *
     * @param user utilisateur à ajouter
     */
    public void addUser(User user) {
        if (!internalUserMap.containsKey(user.getUserName())) {
            internalUserMap.put(user.getUserName(), user);
        }
    }

    /**
     * Récupère les offres de voyages disponibles adaptées à l'utilisateur.
     * <p>
     * Le cumul des points de récompense est calculé et utilisé pour obtenir des offres auprès du service TripPricer.
     * </p>
     *
     * @param user utilisateur ciblé
     * @return liste des {@link Provider} représentant les offres de voyages
     */
    public List<Provider> getTripDeals(User user) {
        int cumulativeRewardPoints = user.getUserRewards().stream()
                .mapToInt(UserReward::getRewardPoints)
                .sum();

        List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
                user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
                user.getUserPreferences().getTripDuration(), cumulativeRewardPoints);
        user.setTripDeals(providers);

        return providers;
    }

    /**
     * Suit la localisation d'un utilisateur de façon asynchrone.
     * 
     * Cette méthode :
     * <ul>
     *     <li>récupère la localisation actuelle via le service GPS,</li>
     *     <li>ajoute cette localisation à l'historique de l'utilisateur,</li>
     *     <li>calcule les récompenses associées.</li>
     * </ul>
     *
     * @param user utilisateur à suivre
     * @return {@link CompletableFuture} complété avec la localisation visitée
     */
    public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
        CompletableFuture<VisitedLocation> futureVisitedLocation = CompletableFuture.supplyAsync(() -> gpsUtil.getUserLocation(user.getUserId()), executorService);

        CompletableFuture<VisitedLocation> visitedLocationToAdd = futureVisitedLocation.thenApply(visitedLocation -> {
            user.addToVisitedLocations(visitedLocation);
            return visitedLocation;
        });

        CompletableFuture<VisitedLocation> result = visitedLocationToAdd.thenCompose(visitedLocation ->
                rewardsService.calculateRewardsAsync(user)
                        .thenApply(v -> visitedLocation)
        );

        return result;
    }

    /**
     * Récupère toutes les attractions triées par distance croissante par rapport
     * à une localisation donnée.
     *
     * @param visitedLocation localisation de référence
     * @return liste triée des {@link Attraction} proches
     */
    public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) {
        List<Attraction> allAttractions = gpsUtil.getAttractions();
        List<Attraction> sortedAttractions = allAttractions.parallelStream()
                .sorted(Comparator.comparingDouble(attraction -> rewardsService.getDistance(attraction, visitedLocation.location)))
                .collect(Collectors.toList());
        logger.info("Nombre d'attractions récupérées : {}", sortedAttractions.size());
        return sortedAttractions;
    }

    /**
     * Récupère les 5 attractions les plus proches de la position d'un utilisateur
     * ainsi que des informations détaillées associées.
     *
     * @param visitedLocation localisation actuelle de l'utilisateur
     * @param user            utilisateur ciblé (pour calcul des récompenses)
     * @return liste de 5 maps contenant les informations clés des attractions proches
     */
    public List<Map<String, Object>> getFiveNearAttractions(VisitedLocation visitedLocation, User user) {
        double userLatitude = visitedLocation.location.latitude;
        double userLongitude = visitedLocation.location.longitude;

        List<Attraction> nearByAttractions = getNearByAttractions(visitedLocation);
        List<Attraction> fiveNearAttractions = nearByAttractions.stream()
                .limit(5)
                .collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();

        for (Attraction attraction : fiveNearAttractions) {
            Map<String, Object> attractionInfo = new HashMap<>();
            attractionInfo.put("attractionName", attraction.attractionName);
            attractionInfo.put("attractionLat", attraction.latitude);
            attractionInfo.put("attractionLong", attraction.longitude);
            attractionInfo.put("userLatitude", userLatitude);
            attractionInfo.put("userLongitude", userLongitude);

            double distanceMiles = rewardsService.getDistance(attraction, visitedLocation.location);
            attractionInfo.put("distanceMiles", distanceMiles);

            int rewardPoints = rewardsService.getRewardPoints(attraction, user);
            attractionInfo.put("rewardPoints", rewardPoints);

            result.add(attractionInfo);
        }
        logger.info("5 attractions proches : {}", result);
        return result;
    }

    /**
     * Ajoute un hook pour l'arrêt de la JVM.
     * <p>
     * Ce hook permet d'arrêter proprement le suivi des utilisateurs lors de
     * la fermeture de l'application.
     * </p>
     */
    private void addShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> tracker.stopTracking()));
    }

}
