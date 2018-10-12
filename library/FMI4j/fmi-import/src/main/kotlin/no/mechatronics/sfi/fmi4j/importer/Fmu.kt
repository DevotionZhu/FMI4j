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

package no.mechatronics.sfi.fmi4j.importer

import no.mechatronics.sfi.fmi4j.common.*
import no.mechatronics.sfi.fmi4j.importer.cs.CoSimulationLibraryWrapper
import no.mechatronics.sfi.fmi4j.importer.cs.CoSimulationSlave
import no.mechatronics.sfi.fmi4j.importer.jni.Fmi2CoSimulationLibrary
import no.mechatronics.sfi.fmi4j.importer.jni.Fmi2Library
import no.mechatronics.sfi.fmi4j.importer.jni.Fmi2ModelExchangeLibrary
import no.mechatronics.sfi.fmi4j.importer.me.ModelExchangeFmuStepper
import no.mechatronics.sfi.fmi4j.importer.me.ModelExchangeInstance
import no.mechatronics.sfi.fmi4j.importer.me.ModelExchangeLibraryWrapper
import no.mechatronics.sfi.fmi4j.importer.misc.FmiType
import no.mechatronics.sfi.fmi4j.importer.misc.extractTo
import no.mechatronics.sfi.fmi4j.modeldescription.*
import no.mechatronics.sfi.fmi4j.modeldescription.parser.ModelDescriptionParser
import no.mechatronics.sfi.fmi4j.solvers.Solver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.IllegalStateException
import java.net.URL
import java.nio.file.Files

interface IFmu: Closeable {

    val guid: String
        get() = modelDescription.guid

    val modelName: String
        get() = modelDescription.modelName

    val modelDescription: ModelDescription

}

interface FmuProvider: IFmu {

    /**
     * Does the FMU support Co-simulation?
     */
    val supportsCoSimulation: Boolean

    /**
     * Does the FMU support Model Exchange?
     */
    val supportsModelExchange: Boolean

    /**
     * Treat this FMU as a CoSimulation FMU
     */
    fun asCoSimulationFmu(): CoSimulationFmu

    /**
     * Threat this FMU as a ModelExchange FMU
     */
    fun asModelExchangeFmu(): ModelExchangeFmu

}

class CoSimulationFmu(
        private val fmu: Fmu
): IFmu by fmu {

    override val modelDescription: CoSimulationModelDescription by lazy {
        fmu.modelDescription.asCoSimulationModelDescription()
    }

    private val libraryCache: Fmi2CoSimulationLibrary by lazy {
        loadLibrary()
    }

    private fun loadLibrary(): Fmi2CoSimulationLibrary {
        val modelIdentifier = modelDescription.modelIdentifier
        val libName = fmu.getAbsoluteLibraryPath(modelIdentifier)
        return Fmi2CoSimulationLibrary(libName).also {
            fmu.registerLibrary(it)
        }
    }

    @JvmOverloads
    fun newInstance(visible: Boolean = false, loggingOn: Boolean = false): CoSimulationSlave {
        val lib = if (modelDescription.canBeInstantiatedOnlyOncePerProcess) loadLibrary() else libraryCache
        val c = fmu.instantiate(modelDescription, lib, FmiType.CO_SIMULATION, visible, loggingOn)
        val wrapper = CoSimulationLibraryWrapper(c, lib)
        return CoSimulationSlave(wrapper, modelDescription).also {
            fmu.registerInstance(it)
        }
    }

}

class ModelExchangeFmu(
        private val fmu: Fmu
): IFmu by fmu {

    override val modelDescription: ModelExchangeModelDescription by lazy {
        fmu.modelDescription.asModelExchangeModelDescription()
    }

    private val libraryCache: Fmi2ModelExchangeLibrary by lazy {
        loadLibrary()
    }

    private fun loadLibrary(): Fmi2ModelExchangeLibrary {
        val modelIdentifier = modelDescription.modelIdentifier
        val libName = fmu.getAbsoluteLibraryPath(modelIdentifier)
        return Fmi2ModelExchangeLibrary(libName).also {
            fmu.registerLibrary(it)
        }
    }

    @JvmOverloads
    fun newInstance(visible: Boolean = false, loggingOn: Boolean = false): ModelExchangeInstance {
        val lib = if (modelDescription.canBeInstantiatedOnlyOncePerProcess) loadLibrary() else libraryCache
        val c = fmu.instantiate(modelDescription, lib, FmiType.MODEL_EXCHANGE, visible, loggingOn)
        val wrapper = ModelExchangeLibraryWrapper(c, lib)
        return ModelExchangeInstance(wrapper, modelDescription).also {
            fmu.registerInstance(it)
        }
    }

    @JvmOverloads
    fun newInstance(solver: Solver, visible: Boolean = false, loggingOn: Boolean = false): ModelExchangeFmuStepper {
        return newInstance(visible, loggingOn).let {
            ModelExchangeFmuStepper(it, solver)
        }
    }

}

/**
 *
 * Represents an FMU
 *
 * @author Lars Ivar Hatledal
 */
class Fmu private constructor(
        private val fmuFile: File
) : FmuProvider {

    private var isClosed = false
    private val libraries = mutableListOf<Fmi2Library>()
    private val instances = mutableListOf<AbstractFmuInstance<*, *>>()

    private val coSimulationFmu by lazy {
        CoSimulationFmu(this)
    }

    private val modelExchangeFmu by lazy {
        ModelExchangeFmu(this)
    }

    /**
     * Get the content of the modelDescription.xml file as a String
     */
    val modelDescriptionXml: String
        get() = modelDescriptionFile.readText()

    override val modelDescription: ModelDescriptionProvider by lazy {
        ModelDescriptionParser.parse(modelDescriptionXml)
    }

    /**
     * Does the FMU support Co-simulation?
     */
    override val supportsCoSimulation: Boolean
        get() = modelDescription.supportsCoSimulation

    /**
     * Does the FMU support Model Exchange?
     */
    override val supportsModelExchange: Boolean
        get() = modelDescription.supportsModelExchange

    override fun asCoSimulationFmu(): CoSimulationFmu {
        if (!supportsCoSimulation) {
            throw IllegalStateException("FMU does not support Co-simulation!")
        }
        return coSimulationFmu
    }

    override fun asModelExchangeFmu(): ModelExchangeFmu {
        if (!supportsModelExchange) {
            throw IllegalStateException("FMU does not support Model Exchange!")
        }
        return modelExchangeFmu
    }

    /**
     * Get the file handle for the modelDescription.xml file
     */
    private val modelDescriptionFile: File
        get() = File(fmuFile, MODEL_DESC)

    private val resourcesPath: String
        get() = "file:///${File(fmuFile, RESOURCES_FOLDER)
                .absolutePath.replace("\\", "/")}"

    /**
     * Get the absolute name of the native library on the form "C://folder/name.extension"
     */
    fun getAbsoluteLibraryPath(modelIdentifier: String): String {
        return File(fmuFile, BINARIES_FOLDER + File.separator + libraryFolderName + platformBitness
                + File.separator + modelIdentifier + libraryExtension).absolutePath
    }

//    private val coSimulationBuilder: CoSimulationSlaveBuilder? by lazy {
//        if (supportsCoSimulation) CoSimulationSlaveBuilder(this) else null
//    }
//
//    private val modelExchangeBuilder: ModelExchangeInstanceBuilder? by lazy {
//        if (supportsModelExchange) ModelExchangeInstanceBuilder(this) else null
//    }

    internal fun registerLibrary(library: Fmi2Library) {
        libraries.add(library)
    }

    internal fun registerInstance(instance: AbstractFmuInstance<*, *>) {
        instances.add(instance)
    }

    internal fun instantiate(modelDescription: CommonModelDescription, library: Fmi2Library,
                             fmiType: FmiType, visible: Boolean, loggingOn: Boolean): Long {
        LOG.trace("Calling instantiate: visible=$visible, loggingOn=$loggingOn")

        return library.instantiate(modelDescription.modelIdentifier,
                fmiType.code, modelDescription.guid, resourcesPath, visible, loggingOn)
    }

    override fun close() {
        if (!isClosed) {

            LOG.debug("Closing FMU '${modelDescription.modelName}'..")

            terminateInstances()
            disposeNativeLibraries()
            deleteExtractedFmuFolder()

            fmus.remove(this)
            isClosed = true

        }
    }

    private fun disposeNativeLibraries() {
        libraries.forEach {
            it.close()
        }
        libraries.clear()
        System.gc()
    }

    private fun terminateInstances() {
        instances.forEach { instance ->
            if (!instance.isTerminated) {
                instance.terminate()
            }
            if (!instance.wrapper.isInstanceFreed) {
                instance.wrapper.freeInstance()
            }
        }
        instances.clear()
    }

    /**
     * Deletes the temporary folder where the FMU was extracted
     */
    private fun deleteExtractedFmuFolder(): Boolean {

        if (fmuFile.exists()) {
            return fmuFile.deleteRecursively().also { success ->
                if (success) {
                    LOG.debug("Deleted extracted FMU contents: $fmuFile")
                } else {
                    LOG.debug("Failed to delete extracted FMU contents: $fmuFile")
                }
            }
        }
        return true
    }

    protected fun finalize() {
        if (!isClosed) {
            LOG.warn("FMU has not been closed prior to garbage collection. Doing it for you..")
            close()
        }
    }

    override fun toString(): String {
        return "Fmu(fmu=${fmuFile.absolutePath})"
    }

    companion object {

        private val LOG: Logger = LoggerFactory.getLogger(Fmu::class.java)

        private const val RESOURCES_FOLDER = "resources"
        private const val BINARIES_FOLDER = "binaries"
        private const val MAC_OS_FOLDER = "darwin"
        private const val WINDOWS_FOLDER = "win"
        private const val LINUX_FOLDER = "linux"
        private const val MAC_OS_LIBRARY_EXTENSION = ".dylib"
        private const val WINDOWS_LIBRARY_EXTENSION = ".dll"
        private const val LINUX_LIBRARY_EXTENSION = ".so"

        private const val FMU_EXTENSION = "fmu"
        private const val FMI4J_FILE_PREFIX = "fmi4j_"

        private const val MODEL_DESC = "modelDescription.xml"


        private val fmus = mutableListOf<Fmu>()

        init {
            Runtime.getRuntime().addShutdownHook(Thread {
                fmus.toMutableList().forEach {
                    //mutableList because the list is modified during call
                    it.close()
                }
            })
        }

        private val libraryFolderName: String
            get() = when {
                isWindows -> WINDOWS_FOLDER
                isLinux -> LINUX_FOLDER
                isMac -> MAC_OS_FOLDER
                else -> throw UnsupportedOperationException("OS '$osName' is unsupported!")
            }

        private val libraryExtension: String
            get() = when {
                isWindows -> WINDOWS_LIBRARY_EXTENSION
                isLinux -> LINUX_LIBRARY_EXTENSION
                isMac -> MAC_OS_LIBRARY_EXTENSION
                else -> throw UnsupportedOperationException("OS '$osName' is unsupported!")
            }

        private fun createTempDir(fmuName: String): File {
            return Files.createTempDirectory(FMI4J_FILE_PREFIX + fmuName).toFile().also {
                File(it, "resources").apply {
                    if (!exists()) {
                        mkdir()
                    }
                }
            }
        }

        /**
         * Creates an FMU from the provided File
         */
        @JvmStatic
        @Throws(IOException::class, FileNotFoundException::class)
        fun from(file: File): Fmu {

            val extension = file.extension.toLowerCase()
            if (extension != FMU_EXTENSION) {
                throw IllegalArgumentException("File is not an FMU! Invalid extension found: .$extension")
            }

            if (!file.exists()) {
                throw FileNotFoundException("No such file: $file!")
            }

            return createTempDir(file.nameWithoutExtension).let { temp ->
                file.extractTo(temp)
            Fmu(temp).also {
                    fmus.add(it)
                }
            }

        }

        /**
         * Creates an FMU from the provided URL.
         */
        @JvmStatic
        @Throws(IOException::class)
        fun from(url: URL): Fmu {

            val extension = File(url.file).extension
            if (extension != FMU_EXTENSION) {
                throw IllegalArgumentException("URL does not point to an FMU! Invalid extension found: .$extension")
            }

            return createTempDir(File(url.file).nameWithoutExtension).let { temp ->
                url.extractTo(temp)
                Fmu(temp).also {
                    fmus.add(it)
                }
            }
        }

    }

}
