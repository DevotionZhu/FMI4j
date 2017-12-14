package no.mechatronics.sfi.fmi4j

import no.mechatronics.sfi.fmi4j.misc.VariableReader
import no.mechatronics.sfi.fmi4j.misc.VariableWriter
import no.mechatronics.sfi.fmi4j.modeldescription.ModelVariables
import no.mechatronics.sfi.fmi4j.modeldescription.me.ModelExchangeModelDescription
import no.mechatronics.sfi.fmi4j.proxy.ModelExchangeLibraryWrapper
import no.mechatronics.sfi.fmi4j.proxy.enums.Fmi2Status
import no.mechatronics.sfi.fmi4j.proxy.structs.Fmi2EventInfo
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations
import org.apache.commons.math3.ode.FirstOrderIntegrator


open class ModelExchangeFmu internal constructor(
        fmuFile: FmuFile,
        modelDescription: ModelExchangeModelDescription,
        wrapper: ModelExchangeLibraryWrapper
): AbstractFmu<ModelExchangeModelDescription, ModelExchangeLibraryWrapper>(fmuFile, modelDescription, wrapper) {

    fun setTime(time: Double) = wrapper.setTime(time)

    fun setContinuousStates(x: DoubleArray) = wrapper.setContinuousStates(x)

    fun enterEventMode() = wrapper.enterEventMode()

    fun enterContinuousTimeMode() = wrapper.enterContinuousTimeMode()

    fun newDiscreteStates(eventInfo: Fmi2EventInfo) = wrapper.newDiscreteStates(eventInfo)

    fun completedIntegratorStep() = wrapper.completedIntegratorStep()

    fun getDerivatives(derivatives: DoubleArray) = wrapper.getDerivatives(derivatives)

    fun getEventIndicators(eventIndicators: DoubleArray) = wrapper.getEventIndicators(eventIndicators)

    fun getContinuousStates(x: DoubleArray) = wrapper.getContinuousStates(x)

    fun getNominalsOfContinuousStates(x_nominal: DoubleArray) = wrapper.getNominalsOfContinuousStates(x_nominal)

}

class ModelExchangeFmuWithIntegrator internal constructor(
        private val fmu: ModelExchangeFmu,
        private val integrator: FirstOrderIntegrator
) : FmiSimulation  {

    private val states: DoubleArray
    private val derivatives: DoubleArray

    private val preEventIndicators: DoubleArray
    private val eventIndicators: DoubleArray

    private val eventInfo: Fmi2EventInfo = Fmi2EventInfo()

    override var currentTime: Double = 0.0
        private set

    override val fmuFile: FmuFile = fmu.fmuFile
    override val modelDescription = fmu.modelDescription
    override val modelVariables = fmu.modelVariables

    private val ode: FirstOrderDifferentialEquations by lazy {
        object : FirstOrderDifferentialEquations {
            override fun getDimension(): Int =  modelDescription.numberOfContinuousStates

            override fun computeDerivatives(time: Double, y: DoubleArray, yDot: DoubleArray) {

                fmu.getDerivatives(yDot)

            }
        }
    }


    init {

        val numberOfContinuousStates = modelDescription.numberOfContinuousStates
        val numberOfEventIndicators = modelDescription.numberOfEventIndicators

        this.states = DoubleArray(numberOfContinuousStates)
        this.derivatives = DoubleArray(numberOfContinuousStates)

        this.preEventIndicators = DoubleArray(numberOfEventIndicators)
        this.eventIndicators = DoubleArray(numberOfEventIndicators)
    }

    override fun write(name: String) = fmu.write(name)
    override fun read(name: String) = fmu.read(name)

    override fun write(vr: Int) = fmu.write(vr)
    override fun read(vr: Int) = fmu.read(vr)

    override fun reset() = fmu.reset()
    override fun reset(requireReinit: Boolean) = fmu.reset(requireReinit)
    override fun terminate() = fmu.terminate()

    override fun close() {
        terminate()
    }

    override fun isTerminated() = fmu.isTerminated()

    override fun getLastStatus() = fmu.getLastStatus()

    override fun init() = init(0.0)
    override fun init(start: Double) = init(start, -1.0)

    override fun init(start: Double, stop: Double) : Boolean {

        if (fmu.init(start, stop)) {
            currentTime = start
            eventInfo.setNewDiscreteStatesNeededTrue()
            eventInfo.setTerminateSimulationFalse()

            while (eventInfo.getNewDiscreteStatesNeeded()) {
                fmu.newDiscreteStates(eventInfo)
                if (eventInfo.getTerminateSimulation()) {
                    terminate()
                    return false
                }
            }
            fmu.enterContinuousTimeMode()
            //getContinuousStates(states)
            fmu.getEventIndicators(eventIndicators)

            return true
        }

        return false

    }

    override fun doStep(dt: Double): Boolean {

        assert(dt > 0)

        println(currentTime)

        var time  = currentTime
        val stopTime =  time + dt

        var tNext: Double
        while ( time < stopTime ) {

            tNext = Math.min( time +  dt, stopTime);

            val timeEvent = eventInfo.getNextEventTimeDefined() && eventInfo.nextEventTime <= time
            if (timeEvent) {
                tNext = eventInfo.nextEventTime
            }

            var stateEvent = false
            if (tNext -  time > 1E-13) {
                val solve = solve(time, tNext)
                stateEvent = solve.stateEvent
                time = solve.time
            } else {
                time = tNext
            }

            fmu.setTime(time)

            val completedIntegratorStep =  fmu.completedIntegratorStep()
            if (completedIntegratorStep.terminateSimulation) {
                terminate()
                return false
            }

            val stepEvent = completedIntegratorStep.enterEventMode

            if (timeEvent || stateEvent || stepEvent) {
                fmu.enterEventMode()

                eventInfo.setNewDiscreteStatesNeededTrue()
                eventInfo.setTerminateSimulationFalse()

                while (eventInfo.getNewDiscreteStatesNeeded()) {
                    fmu.newDiscreteStates(eventInfo)
                    if (eventInfo.getTerminateSimulation()) {
                        terminate()
                        return false
                    }
                }
                fmu.enterContinuousTimeMode()
            }


        }
        currentTime = time
        return true
    }

    private class SolveResult(
            val stateEvent: Boolean,
            val time: Double
    )

    private fun solve(t: Double, tNext:Double) : SolveResult {

        fmu.getContinuousStates(states)
        //getDerivatives(derivatives)

        val dt = tNext - t
        val integratedTime = integrator.integrate(ode, t, states, currentTime + dt, states)

        fmu.setContinuousStates(states)

        for (i in preEventIndicators.indices) {
            preEventIndicators[i] = eventIndicators[i]
        }

        fmu.getEventIndicators(eventIndicators)

        var stateEvent = false
        for (i in preEventIndicators.indices) {
            stateEvent = preEventIndicators[i] * eventIndicators[i] < 0
            if (stateEvent) break
        }

        return SolveResult(stateEvent, integratedTime)

    }

}