package com.example.daugia;

import com.example.daugia.auction.config.AuctionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT55S")
@EnableConfigurationProperties(AuctionProperties.class)
public class BiddingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BiddingApplication.class, args);
	}

}
