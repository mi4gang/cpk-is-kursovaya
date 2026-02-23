package ru.cpk.system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import ru.cpk.system.model.Application;
import ru.cpk.system.model.AssessmentResult;
import ru.cpk.system.model.RoleName;
import ru.cpk.system.model.User;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.AssessmentResultRepository;
import ru.cpk.system.repository.UserRepository;
import ru.cpk.system.service.ApplicationWorkflowService;

@ExtendWith(MockitoExtension.class)
class AssessmentControllerTest {

    @Mock
    private AssessmentResultRepository assessmentResultRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationWorkflowService workflowService;

    @InjectMocks
    private AssessmentController controller;

    private User teacher;
    private Application application;
    private UsernamePasswordAuthenticationToken authentication;
    private AssessmentResult assessment;
    private BindingResult bindingResult;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setId(7L);
        teacher.setUsername("teacher");
        teacher.setRole(RoleName.TEACHER);

        application = new Application();
        application.setId(1L);
        application.setAssignedTeacher(teacher);
        application.setApplicationDate(LocalDate.now());

        assessment = new AssessmentResult();
        assessment.setApplication(application);
        assessment.setScore(80);
        assessment.setResultDate(LocalDate.now());
        assessment.setComment("ok");

        bindingResult = new BeanPropertyBindingResult(assessment, "assessment");
        authentication = new UsernamePasswordAuthenticationToken(
            "teacher",
            "password",
            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );

        when(userRepository.findByUsername("teacher")).thenReturn(Optional.of(teacher));
        when(applicationRepository.findByIdAndAssignedTeacherId(1L, 7L)).thenReturn(Optional.of(application));
        when(assessmentResultRepository.save(any(AssessmentResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void saveWithFromProgramIdRedirectsToTeacherGroup() {
        String view = controller.save(
            assessment,
            bindingResult,
            55L,
            authentication,
            new ExtendedModelMap()
        );

        String expectedMessage = URLEncoder.encode("Результат аттестации сохранен", StandardCharsets.UTF_8);
        assertThat(view).isEqualTo("redirect:/teacher/groups/55?message=" + expectedMessage);
    }

    @Test
    void saveWithoutFromProgramIdRedirectsToAssessmentsList() {
        String view = controller.save(
            assessment,
            bindingResult,
            null,
            authentication,
            new ExtendedModelMap()
        );

        assertThat(view).isEqualTo("redirect:/assessments");
    }
}
