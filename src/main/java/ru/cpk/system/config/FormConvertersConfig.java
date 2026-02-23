package ru.cpk.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.cpk.system.model.Application;
import ru.cpk.system.model.Program;
import ru.cpk.system.model.User;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.ProgramRepository;
import ru.cpk.system.repository.UserRepository;

@Configuration
public class FormConvertersConfig implements WebMvcConfigurer {

    private final ProgramRepository programRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public FormConvertersConfig(ProgramRepository programRepository,
                                ApplicationRepository applicationRepository,
                                UserRepository userRepository) {
        this.programRepository = programRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, Program>() {
            @Override
            public Program convert(String source) {
                if (source == null || source.isBlank()) {
                    return null;
                }
                return programRepository.findById(Long.valueOf(source)).orElse(null);
            }
        });

        registry.addConverter(new Converter<String, Application>() {
            @Override
            public Application convert(String source) {
                if (source == null || source.isBlank()) {
                    return null;
                }
                return applicationRepository.findById(Long.valueOf(source)).orElse(null);
            }
        });

        registry.addConverter(new Converter<String, User>() {
            @Override
            public User convert(String source) {
                if (source == null || source.isBlank()) {
                    return null;
                }
                return userRepository.findById(Long.valueOf(source)).orElse(null);
            }
        });
    }
}
