package no.mechatronics.sfi.fmi4j


import no.mechatronics.sfi.fmi4j.common.FmiStatus
import no.mechatronics.sfi.fmi4j.fmu.AbstractFmu
import no.mechatronics.sfi.fmi4j.fmu.FmuFile
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class CoSimulationFmuTest_kt {

    private lateinit var fmuFile: FmuFile

    @Before
    fun setUp() {

        val path = "../test/fmi2/cs/win64/20Sim/4.6.4.8004/ControlledTemperature/ControlledTemperature.fmu"
        val file = File(path)
        Assert.assertNotNull(file)
        fmuFile = FmuFile(file)

    }

    @Test
    @Throws(Exception::class)
    fun test() {

        fmuFile.asCoSimulationFmu().newInstance().use {fmu ->

            Assert.assertEquals("2.0", fmu.modelDescription.fmiVersion)

            val startTemp = fmu.getVariableByName("HeatCapacity1.T0").asRealVariable().start
            Assert.assertNotNull(startTemp)
            Assert.assertEquals(298.0, startTemp!!, 0.0)

            Assert.assertTrue(fmu.init())
            Assert.assertTrue(fmu.lastStatus === FmiStatus.OK)

            val heatCapacity1_C = fmu.getVariableByName("HeatCapacity1.C").asRealVariable()
            Assert.assertEquals(0.1, heatCapacity1_C.start!!, 0.0)
            println(heatCapacity1_C.read().value)

            val temperatureRoom = fmu.getVariableByName("Temperature_Room").asRealVariable()

            var first1 = java.lang.Double.NaN

            val dt = 1.0 / 100
            for (i in 0..4) {
                fmu.doStep(dt)
                Assert.assertTrue(fmu.lastStatus === FmiStatus.OK)

                val read = temperatureRoom.read()
                Assert.assertTrue(read.status == FmiStatus.OK)
                val value = read.value

                if (java.lang.Double.isNaN(first1)) {
                    first1 = value
                }
                println(value)

            }

            (fmu as AbstractFmu<*, *>).reset(false)

            Assert.assertTrue(fmu.lastStatus === FmiStatus.OK)

            val first = AtomicBoolean(true)
            while (fmu.currentTime < 5) {
                fmu.doStep(dt)
                Assert.assertTrue(fmu.lastStatus === FmiStatus.OK)

                val read = temperatureRoom.read()
                Assert.assertTrue(read.status == FmiStatus.OK)
                val value = read.value

                if (first.getAndSet(false)) {
                    Assert.assertEquals(first1, value, 0.0)
                }
                println(value)

            }

            fmuFile.asCoSimulationFmu().newInstance().use { fmu2 ->
                fmu2.init()
                println(fmu2.variableAccessor.readReal(temperatureRoom.valueReference))
            }

        }

    }
}
