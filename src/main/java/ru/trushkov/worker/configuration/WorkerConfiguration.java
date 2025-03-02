package ru.trushkov.worker.configuration;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WorkerConfiguration {
    /*@Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.registerModule(new JaxbAnnotationModule());
        return builder
                .messageConverters(new MappingJackson2XmlHttpMessageConverter(xmlMapper))
                .build();
    }*/
}
