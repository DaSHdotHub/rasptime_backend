package io.github.dashdothub.rasptime_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RasptimeBackendApplication {

	public static void main(String[] args) {

        SpringApplication.run(RasptimeBackendApplication.class, args);
	}

}
