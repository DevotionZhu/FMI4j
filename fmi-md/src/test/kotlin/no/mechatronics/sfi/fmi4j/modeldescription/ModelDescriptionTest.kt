/*
 * The MIT License
 *
 * Copyright 2017-2018 Norwegian University of Technology
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING  FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package no.mechatronics.sfi.fmi4j.modeldescription

import org.apache.commons.io.IOUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.nio.charset.Charset

class ModelDescriptionTest {

    private lateinit var modelDescription: ModelDescription

    @Before
    fun setUp() {

        val xml = IOUtils.toString(javaClass.classLoader
                .getResource("v2/cs/ControlledTemperature/modelDescription.xml"), Charset.defaultCharset())
        modelDescription = ModelDescriptionParser.parse(xml)

    }

    @Test
    fun getFmiVersion() {

        val fmiVersion = modelDescription.fmiVersion
        Assert.assertEquals("2.0", fmiVersion)
        println("fmiVersion=$fmiVersion")

    }

    @Test
    fun getModelName() {

        val value = modelDescription.modelName
        Assert.assertEquals("ControlledTemperature", value)
        println("modelName=$value")

    }

    @Test
    fun getGuid() {

        val value = modelDescription.guid
        Assert.assertEquals("{06c2700b-b39c-4895-9151-304ddde28443}", value)
        println("guid=$value")

    }

    @Test
    fun getLicense() {

        val value = modelDescription.license
        Assert.assertEquals("-", value)
        println("licence=$value")

    }

    @Test
    fun testDefaultExperiment() {

        val ex = modelDescription.defaultExperiment!!

        Assert.assertEquals(0.0, ex.startTime, 0.0)
        Assert.assertEquals(20.0, ex.stopTime, 0.0)
        Assert.assertEquals(1.0e-4, ex.stepSize, 0.0)

    }

    @Test
    fun testStartVariables() {

        val variable = modelDescription.modelVariables.getByName("HeatCapacity1.T0").asRealVariable()
        Assert.assertEquals(298.0, variable.start!!, 0.0)

    }

}

class VariableNamingConventionTest {

    @Test
    fun getVariableNamingConvention()  {
        val url = javaClass.classLoader.getResourceAsStream("v2/cs/ControlledTemperature/modelDescription.xml")
        Assert.assertNotNull(url)
        val xml = IOUtils.toString(url, Charset.forName("UTF-8"))
        val md = ModelDescriptionParser.parse(xml)

        Assert.assertTrue(md.fmiVersion == "2.0")
        Assert.assertTrue(md.variableNamingConvention == VariableNamingConvention.STRUCTURED)
    }

}