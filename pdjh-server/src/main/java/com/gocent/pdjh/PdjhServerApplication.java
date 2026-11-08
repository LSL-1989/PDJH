package com.gocent.pdjh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class PdjhServerApplication {

	public static void main(String[] args) {
		// 启动诊断：若二者均为 null，说明运行 classpath 未包含 resources（会落到默认 8080 / 无 DataSource）
		ClassLoader cl = PdjhServerApplication.class.getClassLoader();
		System.out.println("[pdjh] application.yml = " + cl.getResource("application.yml"));
		System.out.println("[pdjh] application.properties = " + cl.getResource("application.properties"));
		SpringApplication.run(PdjhServerApplication.class, args);
	}

}
