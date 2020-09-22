package de.codecentric.batch.simple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@SpringBootConfiguration
@EnableAutoConfiguration
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	// If you would like to configure your own batch infrastructure via BatchConfigurer,
	// just add a bean of that type to the ApplicationContext, like in the following code.
	// This starter's implementation will step aside then.
	// @Bean
	// public BatchConfigurer batchConfigurer(DataSource dataSource){
	// return new DefaultBatchConfigurer(dataSource);
	// }

    // If you would like to use your own JobParametersConverter in JobOperationsController,
    // just add a bean of that type to the JobParametersConverter, like in the following code.
    // This starter's implementation will step aside then.
    // @Bean
    // public JobParametersConverter jobParametersConverter(){
    // return new MyOwnJobParametersConverter();
    // }

}
