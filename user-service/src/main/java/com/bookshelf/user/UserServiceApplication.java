package com.bookshelf.user;

import net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.bookshelf.user.config.JwtProperties;

@SpringBootApplication(exclude = GrpcServerSecurityAutoConfiguration.class)
@EnableConfigurationProperties(JwtProperties.class)
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
