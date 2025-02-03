package com.traderdeck.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.traderdeck.backend.utils.EnvConfig;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		System.setProperty("DB_URL", EnvConfig.get("DB_URL"));
		System.setProperty("DB_USERNAME", EnvConfig.get("DB_USERNAME"));
		System.setProperty("DB_PASSWORD", EnvConfig.get("DB_PASSWORD"));

		SpringApplication.run(BackendApplication.class, args);
	}

}
