package ru.cpk.system.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.cpk.system.model.Program;
import ru.cpk.system.model.RoleName;
import ru.cpk.system.model.User;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.AssessmentResultRepository;
import ru.cpk.system.repository.CertificateRepository;
import ru.cpk.system.repository.PaymentRepository;
import ru.cpk.system.repository.ProgramRepository;
import ru.cpk.system.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProgramRepository programRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private AssessmentResultRepository assessmentResultRepository;
    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private DataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new DataInitializer(
            userRepository,
            programRepository,
            applicationRepository,
            paymentRepository,
            assessmentResultRepository,
            certificateRepository,
            passwordEncoder,
            "once"
        );

        when(userRepository.findByUsername(anyString())).thenAnswer(invocation ->
            Optional.of(existingUser(invocation.getArgument(0)))
        );

        when(programRepository.findByTitleContainingIgnoreCase(anyString(), any(Sort.class))).thenAnswer(invocation -> {
            String title = invocation.getArgument(0);
            Program existing = new Program();
            existing.setTitle(title);
            return List.of(existing);
        });

        // В once-режиме при наличии заявок workflow-seed не выполняется.
        when(applicationRepository.count()).thenReturn(1L);
    }

    @Test
    void onceModeDoesNotOverwriteExistingDemoUsers() {
        initializer.run();

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());

        verify(userRepository, never()).deleteAllInBatch();
        verify(programRepository, never()).deleteAllInBatch();
        verify(applicationRepository, never()).deleteAllInBatch();
    }

    private User existingUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setRole(resolveRole(username));
        user.setEnabled(true);
        user.setPassword("already-encoded");
        user.setFullName("Existing " + username);
        user.setEmail(username + "@example.com");
        user.setPhone("+7-900-000-00-00");
        return user;
    }

    private RoleName resolveRole(String username) {
        return switch (username) {
            case "admin" -> RoleName.ADMIN;
            case "methodist" -> RoleName.METHODIST;
            case "teacher", "teacher2", "teacher3" -> RoleName.TEACHER;
            default -> RoleName.STUDENT;
        };
    }
}
