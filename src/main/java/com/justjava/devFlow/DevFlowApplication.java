package com.justjava.devFlow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DevFlowApplication {
	public static void main(String[] args) {
		SpringApplication.run(DevFlowApplication.class, args);
	}

}
