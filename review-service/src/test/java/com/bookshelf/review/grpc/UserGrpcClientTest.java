package com.bookshelf.review.grpc;

import com.bookshelf.grpc.user.UserServiceGrpc;
import com.bookshelf.grpc.user.ValidateTokenRequest;
import com.bookshelf.grpc.user.ValidateTokenResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserGrpcClientTest {

    private UserGrpcClient userGrpcClient;
    private UserServiceGrpc.UserServiceBlockingStub userStub;

    @BeforeEach
    void setUp() {
        userGrpcClient = new UserGrpcClient();
        userStub = mock(UserServiceGrpc.UserServiceBlockingStub.class);
        ReflectionTestUtils.setField(userGrpcClient, "userStub", userStub);
    }

    // ── validateToken ──────────────────────────────────────────────────────────

    @Test
    void validateToken_returnsValidResponse_whenStubSucceeds() {
        ValidateTokenResponse response = ValidateTokenResponse.newBuilder()
                .setValid(true).setUserId("u1").setUsername("alice").build();
        when(userStub.validateToken(any(ValidateTokenRequest.class))).thenReturn(response);

        ValidateTokenResponse result = userGrpcClient.validateToken("good-token");

        assertThat(result.getValid()).isTrue();
        assertThat(result.getUserId()).isEqualTo("u1");
    }

    @Test
    void validateToken_returnsInvalidResponse_whenStubThrowsStatusRuntimeException() {
        when(userStub.validateToken(any(ValidateTokenRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        ValidateTokenResponse result = userGrpcClient.validateToken("any-token");

        assertThat(result.getValid()).isFalse();
    }

}
