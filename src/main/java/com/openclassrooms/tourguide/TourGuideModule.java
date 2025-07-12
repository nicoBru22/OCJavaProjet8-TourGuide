package com.openclassrooms.tourguide;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import gpsUtil.GpsUtil;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.service.RewardsService;

/**
 * Classe de configuration Spring pour l'application TourGuide.
 * 
 * Cette classe définit les beans nécessaires au fonctionnement de l'application,
 * notamment GpsUtil, RewardCentral et RewardsService.
 * 
 * Chaque méthode annotée avec {@code @Bean} instancie et configure un composant
 * injectable par Spring dans le contexte de l'application.
 */
@Configuration
public class TourGuideModule {
	
    /**
     * Bean fournissant une instance de {@link GpsUtil}.
     * 
     * @return une nouvelle instance de GpsUtil.
     */
	@Bean
	public GpsUtil getGpsUtil() {
		return new GpsUtil();
	}
	
    /**
     * Bean fournissant une instance de {@link RewardsService}.
     * 
     * Cette instance est construite avec les beans GpsUtil et RewardCentral.
     * 
     * @return une nouvelle instance de RewardsService.
     */
	@Bean
	public RewardsService getRewardsService() {
		return new RewardsService(getGpsUtil(), getRewardCentral());
	}
	
    /**
     * Bean fournissant une instance de {@link RewardCentral}.
     * 
     * @return une nouvelle instance de RewardCentral.
     */
	@Bean
	public RewardCentral getRewardCentral() {
		return new RewardCentral();
	}
	
}
