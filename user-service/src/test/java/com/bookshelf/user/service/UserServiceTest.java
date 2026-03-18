package com.bookshelf.user.service;

import com.bookshelf.user.dto.RegisterRequest;
import com.bookshelf.user.model.User;
import com.bookshelf.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService userService;

    // ── register ───────────────────────────────────────────────────────────────

    @Test
    void register_encodesPasswordAndSavesUser() {
        RegisterRequest req = new RegisterRequest("alice", "alice@example.com", "secret123");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        User saved = User.builder().id("u1").username("alice").email("alice@example.com").password("hashed").build();
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.register(req);

        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getPassword()).isEqualTo("hashed");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsConflict_whenUsernameAlreadyTaken() {
        RegisterRequest req = new RegisterRequest("alice", "alice@example.com", "secret123");
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void register_throwsConflict_whenEmailAlreadyRegistered() {
        RegisterRequest req = new RegisterRequest("alice", "alice@example.com", "secret123");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email already registered");
    }

    // ── findByUsername ─────────────────────────────────────────────────────────

    @Test
    void findByUsername_returnsUser() {
        User user = User.builder().id("u1").username("alice").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThat(userService.findByUsername("alice")).isEqualTo(user);
    }

    @Test
    void findByUsername_throwsNotFound_whenMissing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUsername("ghost"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User not found");
    }

    // ── findById ───────────────────────────────────────────────────────────────

    @Test
    void findById_returnsUser() {
        User user = User.builder().id("u1").username("alice").build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        assertThat(userService.findById("u1")).isEqualTo(user);
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById("missing"))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ── loadUserByUsername ─────────────────────────────────────────────────────

    @Test
    void loadUserByUsername_returnsUserDetailsWithRoleUser() {
        User user = User.builder().id("u1").username("alice").password("hashed").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = userService.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("hashed");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_throwsUsernameNotFoundException_whenMissing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
