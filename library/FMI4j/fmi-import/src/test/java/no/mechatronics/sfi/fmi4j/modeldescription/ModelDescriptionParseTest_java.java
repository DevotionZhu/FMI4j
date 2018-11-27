package no.mechatronics.sfi.fmi4j.modeldescription;

import no.mechatronics.sfi.fmi4j.TestUtils;
import no.mechatronics.sfi.fmi4j.common.OSUtil;
import no.mechatronics.sfi.fmi4j.modeldescription.parser.ModelDescriptionParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;

@EnabledIfEnvironmentVariable(named = "TEST_FMUs", matches = ".*")
public class ModelDescriptionParseTest_java {

    @Test
    public void test1() {

        File fmuFile = new File(TestUtils.getTEST_FMUs(),
                "2.0/cs/" + OSUtil.getCurrentOS() +
                        "/20sim/4.6.4.8004/ControlledTemperature/ControlledTemperature.fmu");
        Assertions.assertTrue(fmuFile.exists());

        Assertions.assertNotNull(ModelDescriptionParser.parse(fmuFile).asCoSimulationModelDescription());

        String xml = ModelDescriptionParser.extractModelDescriptionXml(fmuFile);
        Assertions.assertNotNull(ModelDescriptionParser.parse(xml).asCoSimulationModelDescription());

    }

    @Test
    public void test2() {

        File fmuFile = new File(TestUtils.getTEST_FMUs(),
                "2.0/cs/" + OSUtil.getCurrentOS() +
                        "/JModelica.org/1.15/PID_Controller/PID_Controller.fmu");
        Assertions.assertTrue(fmuFile.exists());

        Assertions.assertNotNull(ModelDescriptionParser.parse(fmuFile).asCoSimulationModelDescription());

        String xml = ModelDescriptionParser.extractModelDescriptionXml(fmuFile);
        Assertions.assertNotNull(ModelDescriptionParser.parse(xml).asCoSimulationModelDescription());

    }

    @Test
    @EnabledOnOs(OS.LINUX)
    public void test3() {

        File fmuFile = new File(TestUtils.getTEST_FMUs(),
                "2.0/cs/" + OSUtil.getCurrentOS() +
                        "/EDALab_HIFSuite/2017.05_antlia/uart/uart.fmu");

        Assertions.assertTrue(fmuFile.exists());

        Assertions.assertNotNull(ModelDescriptionParser.parse(fmuFile).asCoSimulationModelDescription());

        String xml = ModelDescriptionParser.extractModelDescriptionXml(fmuFile);
        Assertions.assertNotNull(ModelDescriptionParser.parse(xml).asCoSimulationModelDescription());

    }

//    @Test
//    @EnabledOnOs(OS.LINUX)
//    public void test4() {
//
//        File fmuFile = new File(TestUtils.getTEST_FMUs(),
//                "2.0/cs/" + OSUtil.getCurrentOS() +
//                        "/AMESim/15/fuelrail_cs/fuelrail_cs.fmu");
//
//        Assertions.assertTrue(fmuFile.exists());
//
//        Assertions.assertNotNull(ModelDescriptionParser.parse(fmuFile).asCoSimulationModelDescription());
//
//        String xml = ModelDescriptionParser.extractModelDescriptionXml(fmuFile);
//        Assertions.assertNotNull(ModelDescriptionParser.parse(xml).asCoSimulationModelDescription());
//
//    }

}
