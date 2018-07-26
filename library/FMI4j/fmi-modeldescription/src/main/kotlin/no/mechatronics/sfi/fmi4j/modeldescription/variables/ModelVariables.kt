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

package no.mechatronics.sfi.fmi4j.modeldescription.variables

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import no.mechatronics.sfi.fmi4j.common.StringArray
import java.io.Serializable

/**
 * @author Lars Ivar Hatledal
 */
interface ModelVariables : Iterable<TypedScalarVariable<*>> {

    /**
     * Get the number of model categories held by this structure
     */
    val size: Int
        get() = getVariables().size

    fun getVariables(): List<TypedScalarVariable<*>>

    operator fun get(index: Int): TypedScalarVariable<*> = getVariables()[index]

    override fun iterator(): Iterator<TypedScalarVariable<*>> = getVariables().iterator()

    val integers: List<IntegerVariable>
        get() = mapNotNull { (it as? IntegerVariable)?.asIntegerVariable() }

    val reals: List<RealVariable>
        get() = mapNotNull { (it as? RealVariable)?.asRealVariable() }

    val strings: List<StringVariable>
        get() = mapNotNull { (it as? StringVariable)?.asStringVariable() }

    val booleans: List<BooleanVariable>
        get() = mapNotNull { (it as? BooleanVariable)?.asBooleanVariable() }

    val enumerations: List<EnumerationVariable>
        get() = mapNotNull { (it as? EnumerationVariable)?.asEnumerationVariable() }

    /**
     * Does a variable with the provided valueReference exist?
     *
     * @param valueReference
     */
    fun isValidValueReference(valueReference: Int): Boolean {
        return valueReference in map { it.valueReference }
    }

    /**
     * Get the valueReference of the variable named <name>
     *
     * @name name
     * @throws IllegalArgumentException if there is no variable with the provided name
     */
    fun getValueReference(name: String): Int {
        return firstOrNull { it.name == name }?.valueReference
                ?: throw IllegalArgumentException("No variable with name '$name'")
    }

    /**
     * Get a list of value references matching the provided names
     * @throws IllegalArgumentException if a name is provided that does not match a variable
     */
    fun getValueReferences(names: StringArray): List<Int> {
        return names.map { getValueReference(it) }
    }

    /**
     * Get all variables with the given valueReference
     *
     * @vr valueReference
     * @throws IllegalArgumentException if there are no variables with the provided value reference
     */
    fun getByValueReference(vr: Int): List<TypedScalarVariable<*>> {
        return filter { it.valueReference == vr }
    }

    /**
     * Get variable by name
     * @name the variable name
     * @throws IllegalArgumentException if there is no variable with the provided name
     */
    fun getByName(name: String): TypedScalarVariable<*> {
        return firstOrNull { it.name == name }
                ?: throw IllegalArgumentException("No variable with name '$name'")
    }

    /**
     * Return a list of all variables with the proved causality
     */
    fun getByCausality(causality: Causality): List<TypedScalarVariable<*>> {
        return filter { it.causality == causality }
    }

}

/**
 * @author Lars Ivar Hatledal
 */
@JacksonXmlRootElement(localName = "ModelVariables")
class ModelVariablesImpl : ModelVariables, Serializable {

    @JacksonXmlProperty(localName = "ScalarVariable")
    @JacksonXmlElementWrapper(useWrapping = false)
    private val variables: List<ScalarVariableImpl>? = null

    @Transient
    private var _variables: List<TypedScalarVariable<*>>? = null

    override fun getVariables(): List<TypedScalarVariable<*>> {
        if (_variables == null) {
            _variables = variables!!.map { it.toTyped() }
        }
        return _variables!!
    }

    override fun toString(): String {
        return "ModelVariablesImpl(variables=$variables)"
    }

}

