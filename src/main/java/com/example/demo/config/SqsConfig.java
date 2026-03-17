package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class SqsConfig {

	//配置SqsClient，之后就用这个client连接队列服务
    @Bean
    public SqsClient sqsClient() throws URISyntaxException {
        String URL = "http://192.168.10.131:4566";                                     //TODO fetch from config file
        SqsClientBuilder sqsClientBuilder = SqsClient.builder()
                                                        .region(Region.US_EAST_1) //TODO use best region
                                                        .endpointOverride(new URI(URL));
        return sqsClientBuilder.build();
    }
 }