package com.bookshelf.book.grpc;

import com.bookshelf.grpc.user.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserGrpcClient {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userStub;

    public ValidateTokenResponse validateToken(String token) {
        try {
            return userStub.validateToken(ValidateTokenRequest.newBuilder().setToken(token).build());
        } catch (StatusRuntimeException e) {
            log.error("gRPC validateToken error: {}", e.getStatus());
            return ValidateTokenResponse.newBuilder().setValid(false).build();
        }
    }
}
