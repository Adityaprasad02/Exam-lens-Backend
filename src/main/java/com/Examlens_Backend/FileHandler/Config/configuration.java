package com.Examlens_Backend.FileHandler.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.concurrent.Executor;

@Configuration
public class configuration
{
    @Value("${frontend.url}")
    private String frontend_url ;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder){
        return builder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean(name = "fileProcessingExecutor")
    public Executor fileProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("File-Processor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
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