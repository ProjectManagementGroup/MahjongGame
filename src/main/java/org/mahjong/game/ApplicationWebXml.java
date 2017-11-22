package org.mahjong.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

/**
 * This is a helper Java class that provides an alternative to creating a web.xml.
 * http://stackoverflow.com/questions/8587489/how-to-set-active-spring-3-1-environment-profile-via-a-properites-file-and-not-v
 * 
 */
public class ApplicationWebXml extends SpringBootServletInitializer {

    private final Logger log = LoggerFactory.getLogger(ApplicationWebXml.class);

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.profiles(addDefaultProfile()).sources(Application.class);
    }

    /**
     * Set a default profile if it has not been set.
     * <p/>
     * <p>
     * Please use -Dspring.profiles.active=dev
     * </p>
     */
    private String addDefaultProfile() {
        String profile = System.getProperty("spring.profiles.active");
        if (profile != null) {
            log.info("Running with Spring profile(s) : {}", profile);
            return profile;
        }

        log.warn("No Spring profile configured, running with default configuration");
        //return Constants.SPRING_PROFILE_DEVELOPMENT;
        return Constants.SPRING_PROFILE_PRODUCTION;
    }
}
