package com.traderdeck.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.traderdeck.backend.utils.EnvConfig;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		// Database configuration
		System.setProperty("DB_URL", EnvConfig.get("DB_URL"));
		System.setProperty("DB_USERNAME", EnvConfig.get("DB_USERNAME"));
		System.setProperty("DB_PASSWORD", EnvConfig.get("DB_PASSWORD"));
		
		// AWS Bedrock configuration
		System.setProperty("AWS_REGION", EnvConfig.get("AWS_REGION"));
		System.setProperty("AWS_ACCESS_KEY_ID", EnvConfig.get("AWS_ACCESS_KEY_ID"));
		System.setProperty("AWS_SECRET_ACCESS_KEY", EnvConfig.get("AWS_SECRET_ACCESS_KEY"));
		System.setProperty("BEDROCK_FLOW_ID", EnvConfig.get("BEDROCK_FLOW_ID"));
		System.setProperty("BEDROCK_FLOW_ALIAS_ID", EnvConfig.get("BEDROCK_FLOW_ALIAS_ID"));

		SpringApplication.run(BackendApplication.class, args);
	}

}
