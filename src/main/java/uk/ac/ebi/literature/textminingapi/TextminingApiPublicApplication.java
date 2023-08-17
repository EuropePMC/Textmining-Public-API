package uk.ac.ebi.literature.textminingapi;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
@PropertySources({ @PropertySource("classpath:application-utility.properties"),
		@PropertySource(value = "classpath:application-utility-${spring.profiles.active}.properties") })
public class TextminingApiPublicApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(TextminingApiPublicApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
	}

}