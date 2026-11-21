package cl.duoc.atlas.e2e;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Runner de la suite E2E (Cucumber sobre JUnit Platform, ejecutado por Maven Surefire).
 *
 * <p>Filtrar por etiqueta: {@code mvn test -Dcucumber.filter.tags="@seguridad"}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "cl.duoc.atlas.e2e")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value =
        "pretty, summary, html:target/cucumber-report/cucumber.html,"
            + " json:target/cucumber-report/cucumber.json,"
            + " junit:target/cucumber-report/cucumber.xml")
public class RunCucumberTest {}
