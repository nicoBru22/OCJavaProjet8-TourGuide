package com.openclassrooms.tourguide.tracker;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;

/**
 * Tracker est une classe qui étend Thread et permet de suivre périodiquement
 * la localisation de tous les utilisateurs gérés par un {@link TourGuideService}.
 * <p>
 * Le suivi s'effectue dans un thread séparé, qui s'exécute toutes les
 * 5 secondes pour récupérer et actualiser
 * les positions des utilisateurs.
 * </p>
 * <p>
 * La gestion du thread est réalisée via un {@link ExecutorService} à un seul thread.
 * </p>
 */
public class Tracker extends Thread {

    private Logger logger = LoggerFactory.getLogger(Tracker.class);

    /**
     * Intervalle en secondes entre deux phases de suivi des utilisateurs.
     */
    private static final long trackingPollingInterval = TimeUnit.MINUTES.toSeconds(5);

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final TourGuideService tourGuideService;

    private boolean stop = false;

    /**
     * Constructeur de Tracker.
     * <p>
     * Démarre automatiquement l'exécution du thread de suivi dès la création.
     * </p>
     *
     * @param tourGuideService le service utilisé pour récupérer les utilisateurs et suivre leur localisation
     */
    public Tracker(TourGuideService tourGuideService) {
        this.tourGuideService = tourGuideService;
        executorService.submit(this);
    }

    /**
     * Arrête proprement le thread Tracker.
     * <p>
     * Cette méthode demande l'arrêt de la boucle de suivi, puis interrompt
     * immédiatement le thread via le {@link ExecutorService}.
     * </p>
     */
    public void stopTracking() {
        stop = true;
        executorService.shutdownNow();
    }

    /**
     * Boucle principale d'exécution du thread Tracker.
     * <p>
     * Cette boucle tourne indéfiniment jusqu'à ce que le thread soit interrompu
     * ou que la méthode {@link #stopTracking()} soit appelée.
     * À chaque itération, le Tracker récupère la liste des utilisateurs et
     * déclenche leur suivi de localisation via {@link TourGuideService#trackUserLocation(User)}.
     * Puis il attend un intervalle défini avant de recommencer.
     * </p>
     */
    @Override
    public void run() {
        StopWatch stopWatch = new StopWatch();
        while (true) {
            if (Thread.currentThread().isInterrupted() || stop) {
                logger.debug("Tracker stopping");
                break;
            }

            List<User> users = tourGuideService.getAllUsers();
            logger.debug("Begin Tracker. Tracking " + users.size() + " users.");
            stopWatch.start();
            users.forEach(u -> tourGuideService.trackUserLocation(u));
            stopWatch.stop();
            logger.debug("Tracker Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
            stopWatch.reset();
            try {
                logger.debug("Tracker sleeping");
                TimeUnit.SECONDS.sleep(trackingPollingInterval);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
