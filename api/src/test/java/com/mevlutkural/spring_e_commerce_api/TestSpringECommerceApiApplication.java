package com.mevlutkural.spring_e_commerce_api;

import org.springframework.boot.SpringApplication;

public class TestSpringECommerceApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(SpringECommerceApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
