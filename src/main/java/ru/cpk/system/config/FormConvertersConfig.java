package ru.cpk.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.cpk.system.model.Application;
import ru.cpk.system.model.Program;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.ProgramRepository;

@Configuration
public class FormConvertersConfig implements WebMvcConfigurer {

    private final ProgramRepository programRepository;
    private final ApplicationRepository applicationRepository;

    public FormConvertersConfig(ProgramRepository programRepository,
                                ApplicationRepository applicationRepository) {
        this.programRepository = programRepository;
        this.applicationRepository = applicationRepository;
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
    }
}
