package com.Examlens_Backend.FileHandler.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;

@Configuration
public class configuration
{
    @Value("${frontend.url}")
    private String frontend_url ;

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate() ;
    }

    @Bean(name = "fileProcessingExecutor")
    public Executor fileProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500); // Backlog queue
        executor.setThreadNamePrefix("File-Processor-");
        executor.initialize();
        return executor;
    }


    @Bean
    public WebMvcConfigurer webMvcConfigurer(){
         return new WebMvcConfigurer() {

             @Override
             public void addCorsMappings(CorsRegistry registry){
                   registry.addMapping("/**")
                           .allowedOrigins(frontend_url)
                           .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                           .allowedHeaders("*")
                           .allowCredentials(true);
             }
         } ;
    }
}
