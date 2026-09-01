package com.mediflow.medicine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.mediflow")
@EntityScan("com.mediflow")
@EnableJpaRepositories(basePackages = "com.mediflow")
public class MedicineAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedicineAppApplication.class, args);
	}

}
