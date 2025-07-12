package com.openclassrooms.tourguide.helper;

/**
 * Classe utilitaire pour gérer le nombre interne d'utilisateurs utilisés
 * principalement lors des tests.
 * <p>
 * Elle permet de définir et récupérer le nombre d'utilisateurs internes simulés.
 * </p>
 */
public class InternalTestHelper {

    /**
     * Nombre par défaut d'utilisateurs internes pour les tests.
     * Peut être modifié jusqu'à 100 000.
     */
    private static int internalUserNumber = 100;

    /**
     * Définit le nombre d'utilisateurs internes à utiliser.
     * 
     * @param internalUserNumber le nouveau nombre d'utilisateurs internes
     */
    public static void setInternalUserNumber(int internalUserNumber) {
        InternalTestHelper.internalUserNumber = internalUserNumber;
    }

    /**
     * Récupère le nombre d'utilisateurs internes actuellement configuré.
     * 
     * @return le nombre d'utilisateurs internes
     */
    public static int getInternalUserNumber() {
        return internalUserNumber;
    }
}
