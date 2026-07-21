package com.gcoedu.core.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SchoolClassControllerAuthorizationTest {

    @Test
    void classesBySchoolAllowsMunicipalityAdministrator() throws NoSuchMethodException {
        Method endpoint = SchoolClassController.class
                .getMethod("getClassesBySchoolId", String.class);

        PreAuthorize authorization = endpoint.getAnnotation(PreAuthorize.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).contains("'TECADM'");
    }
}
