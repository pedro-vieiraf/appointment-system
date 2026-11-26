package com.pedrovieira.appointmentsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SpringBootApplication.
 */
@SpringBootApplication
@EnableScheduling
public class AppointmentSystemApplication {

  /**
   * Main method for the application entrance.
   */
  public static void main(String[] args) {
    SpringApplication.run(AppointmentSystemApplication.class, args);
  }

}
