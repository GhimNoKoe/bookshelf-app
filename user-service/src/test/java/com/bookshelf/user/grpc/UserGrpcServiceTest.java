package com.bookshelf.user.grpc;

import com.bookshelf.grpc.user.GetUserRequest;
import com.bookshelf.grpc.user.UserResponse;
import com.bookshelf.grpc.user.ValidateTokenRequest;
import com.bookshelf.grpc.user.ValidateTokenResponse;
import com.bookshelf.user.model.User;
import com.bookshelf.user.service.JwtService;
import com.bookshelf.user.service.UserService;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserGrpcServiceTest {

    @Mock UserService userService;
    @Mock JwtService jwtService;
    @InjectMocks UserGrpcService userGrpcService;

    // ── getUser ────────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getUser_sendsUserResponseAndCompletes() {
        User user = User.builder().id("u1").username("alice").email("alice@example.com").build();
        when(userService.findById("u1")).thenReturn(user);
        StreamObserver<UserResponse> responseObserver = mock(StreamObserver.class);

        userGrpcService.getUser(GetUserRequest.newBuilder().setUserId("u1").build(), responseObserver);

        ArgumentCaptor<UserResponse> captor = ArgumentCaptor.forClass(UserResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        assertThat(captor.getValue().getUserId()).isEqualTo("u1");
        assertThat(captor.getValue().getUsername()).isEqualTo("alice");
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUser_sendsErrorWhenUserNotFound() {
        when(userService.findById("missing")).thenThrow(mock(ResponseStatusException.class));
        StreamObserver<UserResponse> responseObserver = mock(StreamObserver.class);

        userGrpcService.getUser(GetUserRequest.newBuilder().setUserId("missing").build(), responseObserver);

        verify(responseObserver).onError(any());
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    // ── validateToken ──────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void validateToken_returnsValidTrueWithUserInfo_whenTokenIsGood() {
        when(jwtService.validateToken("good-token")).thenReturn(true);
        when(jwtService.extractUsername("good-token")).thenReturn("alice");
        User user = User.builder().id("u1").username("alice").build();
        when(userService.findByUsername("alice")).thenReturn(user);
        StreamObserver<ValidateTokenResponse> responseObserver = mock(StreamObserver.class);

        userGrpcService.validateToken(
                ValidateTokenRequest.newBuilder().setToken("good-token").build(), responseObserver);

        ArgumentCaptor<ValidateTokenResponse> captor = ArgumentCaptor.forClass(ValidateTokenResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        assertThat(captor.getValue().getValid()).isTrue();
        assertThat(captor.getValue().getUserId()).isEqualTo("u1");
        assertThat(captor.getValue().getUsername()).isEqualTo("alice");
    }

    @Test
    @SuppressWarnings("unchecked")
    void validateToken_returnsValidFalse_whenTokenIsInvalid() {
        when(jwtService.validateToken("bad-token")).thenReturn(false);
        StreamObserver<ValidateTokenResponse> responseObserver = mock(StreamObserver.class);

        userGrpcService.validateToken(
                ValidateTokenRequest.newBuilder().setToken("bad-token").build(), responseObserver);

        ArgumentCaptor<ValidateTokenResponse> captor = ArgumentCaptor.forClass(ValidateTokenResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        assertThat(captor.getValue().getValid()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void validateToken_returnsValidFalse_whenTokenIsValidButUserLookupFails() {
        when(jwtService.validateToken("good-token")).thenReturn(true);
        when(jwtService.extractUsername("good-token")).thenReturn("ghost");
        when(userService.findByUsername("ghost")).thenThrow(mock(ResponseStatusException.class));
        StreamObserver<ValidateTokenResponse> responseObserver = mock(StreamObserver.class);

        userGrpcService.validateToken(
                ValidateTokenRequest.newBuilder().setToken("good-token").build(), responseObserver);

        ArgumentCaptor<ValidateTokenResponse> captor = ArgumentCaptor.forClass(ValidateTokenResponse.class);
        verify(responseObserver).onNext(captor.capture());
        assertThat(captor.getValue().getValid()).isFalse();
    }
}
