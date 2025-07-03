package ee.fhir.fhirest.core.service.conformance;

import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain.CacheConfiguration;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class HapiContextHolderCacheWithDefaultEnvTest {
    @Autowired
    private HapiContextHolder hapiContextHolder;

    @Test
    void shouldUseDefaultValue_whenCacheEnvVarIsNotProvided() {
        //Arrange
        final long expected = 600000;

        ConformanceHolder.setCodeSystems(List.of());
        ConformanceHolder.setValueSets(List.of());

        hapiContextHolder.init();

        //Act
        final ValidationSupportChain validationSupportChain = ReflectionTestUtils.invokeMethod(hapiContextHolder,"getValidationSupport");

        assertNotNull(validationSupportChain);
        final CacheConfiguration cacheConfiguration = (CacheConfiguration) ReflectionTestUtils.getField(validationSupportChain,"myCacheConfiguration");
        assertNotNull(cacheConfiguration);

        final long actual = cacheConfiguration.getCacheTimeout();

        //Assert
        Assertions.assertEquals(expected, actual);
    }
}