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

import no.mechatronics.sfi.fmi4j.modeldescription.enums.*
import org.w3c.dom.Node
import javax.xml.bind.JAXBContext
import javax.xml.bind.annotation.*
import javax.xml.bind.annotation.adapters.XmlAdapter

interface ScalarVariable {

    /**
     * The full, unique name of the variable. Every variable is uniquely identified within an FMU
     * instance by this name or by its ScalarVariable index (the element position in the
     * ModelVariables list; the first list element has index=1).
     */
    val name: String

    /**
     * If present, name of type defined with TypeDefinitions / SimpleType. The value
     * defined in the corresponding TypeDefinition (see section 2.2.3) is used as
     * default. [If, for example “min” is present both in Real (of TypeDefinition) and in
     * “Real” (of ScalarVariable), then the “min” of ScalarVariable is actually
     * used.] For Real, Integer, Boolean, String, this attribute is optional. For
     * Enumeration it is required, because the Enumeration items are defined in
     * TypeDefinitions / SimpleType.
     */
    val declaredType: String

    /**
     * An optional description string describing the meaning of the variable
     */
    val description: String
    val causality: Causality?
    val variability: Variability?
    val initial: Initial?

    /**
     * A handle of the variable to efficiently identify the variable value in the model interface.
     * This handle is a secret of the tool that generated the C functions. It is not required to be
     * unique. The only guarantee is that valueReference is sufficient to identify the respective variable value in the call of the C functions. This implies that it is unique for a
     * particular base data type (Real, Integer/Enumeration, Boolean, String) with
     * exception of variables that have identical values (such variables are also called “alias”
     * variables). This attribute is “required”.
     */
    val valueReference: Int

    val typeName: String

    val start: Any?

    fun asIntegerVariable(): IntegerVariable
    fun asRealVariable(): RealVariable
    fun asStringVariable(): StringVariable
    fun asBooleanVariable(): BooleanVariable

}

@XmlRootElement(name="ScalarVariable")
@XmlAccessorType(XmlAccessType.FIELD)
class ScalarVariableImpl : ScalarVariable {

    /**
     * @inheritDoc
     */
    @XmlAttribute
    override val name: String = ""

    /**
     * @inheritDoc
     */
    @XmlAttribute
    override val declaredType: String = ""

    /**
     * @inheritDoc
     */
    @XmlAttribute
    override val description: String = ""

    /**
     * @inheritDoc
     */
    @XmlAttribute
    override val causality: Causality? = null

    /**
     * @inheritDoc
     */
    @XmlAttribute
    override val variability: Variability? = null

    /**
     * @inheritDoc
     */
    @XmlAttribute
    override var initial: Initial? = null

    @XmlAttribute(name="valueReference")
    private val _valueReference: Int? = null

    override val typeName: String = ""

    override val start: Any? = null

    /**
     * @inheritDoc
     */
    override val valueReference: Int
        get(){
            return _valueReference!!
        }

    @XmlElement(name="Integer")
    internal val integerAttribute: IntegerAttribute? = null

    @XmlElement(name="Real")
    internal val realAttribute: RealAttribute? = null

    @XmlElement(name="String")
    internal val stringAttribute: StringAttribute? = null

    @XmlElement(name="Boolean")
    internal val booleanAttribute: BooleanAttribute? = null

    override fun asIntegerVariable() = if (integerAttribute != null) IntegerVariable(this) else throw IllegalStateException("Variable $name is not of type Integer!")

    override fun asRealVariable() = if (realAttribute != null) RealVariable(this) else throw IllegalStateException("Variable $name is not of type Real!")

    override fun asStringVariable() = if (stringAttribute != null) StringVariable(this) else throw IllegalStateException("Variable $name is not of type String!")

    override fun asBooleanVariable() = if (booleanAttribute != null) BooleanVariable(this) else throw IllegalStateException("Variable $name is not of type Boolean!")

    override fun toString(): String {
        return "ScalarVariableImpl(name='$name', declaredType='$declaredType', description='$description', causality=$causality, variability=$variability, initial=$initial)"
    }

}

@XmlAccessorType(XmlAccessType.FIELD)
internal class IntegerAttribute {

    /**
     * Minimum value of variable (variable Value ≥ min). If not defined, the
     * minimum is the largest negative number that can be represented on the
     * machine. The min definition is an information from the FMU to the
     * environment defining the region in which the FMU is designed to operate, see
     * also comment after this table.
     */
    @XmlAttribute
    val min: Int? = null

    /**
     * Maximum value of variable (variableValue ≤ max). If not defined, the
     * maximum is the largest positive number that can be represented on the
     * machine. The max definition is an information from the FMU to the
     * environment defining the region in which the FMU is designed to operate, see
     * also comment after this table.
     */
    @XmlAttribute
    val max: Int? = null

    /**
     * @inheritDoc
     */
    @XmlAttribute
    var start: Int? = null

}

@XmlAccessorType(XmlAccessType.FIELD)
internal class RealAttribute {

    /**
     * Minimum value of variable (variable Value ≥ min). If not defined, the
     * minimum is the largest negative number that can be represented on the
     * machine. The min definition is an information from the FMU to the
     * environment defining the region in which the FMU is designed to operate, see
     * also comment after this table.
     */
    @XmlAttribute
    val min: Double? = null

    /**
     * Maximum value of variable (variableValue ≤ max). If not defined, the
     * maximum is the largest positive number that can be represented on the
     * machine. The max definition is an information from the FMU to the
     * environment defining the region in which the FMU is designed to operate, see
     * also comment after this table.
     */
    @XmlAttribute
    val max: Double? = null

    /**
     * Nominal value of variable. If not defined and no other information about the
     * nominal value is available, then nominal = 1 is assumed.
     * [The nominal value of a variable can be, for example used to determine the
     * absolute tolerance for this variable as needed by numerical algorithms:
     * absoluteTolerance = nominal*tolerance*0.01
     * where tolerance is, e.g., the relative tolerance defined in
     * <DefaultExperiment>, see section 2.2.5.]
     */
    @XmlAttribute
    val nominal : Double?  = null

    /**
     * @inheritDoc
     */
    @XmlAttribute
    var start: Double? = null

    /**
     * If present, this variable is the derivative of variable with ScalarVariable index "derivative",
     */
    @XmlAttribute
    val derivative: Int? = null

    /**
     * If true, indicates that the variable gets during time integration much larger
     * than its nominal value nominal. [Typical examples are the monotonically
     * increasing rotation angles of crank shafts and the longitudinal position of a
     * vehicle along the track in long distance simulations. This information can, for
     * example, be used to increase numerical stability and accuracy by setting the
     * corresponding bound for the relative error to zero (relative tolerance = 0.0), if
     * the corresponding variable or an alias of it is a continuous state variable.]
     */
    @XmlAttribute
    val unbounded: Boolean? = null

    /**
     * Only for Model exchange
     * <br>
     * If true, state can be reinitialized at an event by the FMU. If false, state will never be reinitialized at an event by the FMU
     *
     */
    @XmlAttribute
    val reint: Boolean = false

    /**
     * Physical quantity of the variable, for example “Angle”, or “Energy”. The
     * quantity names are not standardized.
     */
    @XmlAttribute
    val quantity: String? = null

    /**
     * Unit of the variable defined with UnitDefinitions.Unit.name that is used
     * for the model equations [, for example “N.m”: in this case a Unit.name =
     * "N.m" must be present under UnitDefinitions].
     */
    @XmlAttribute
    val unit: String? = null

    /**
     * Default display unit. The conversion to the “unit” is defined with the element
     * “<fmiModelDescription><UnitDefinitions>”. If the corresponding
     * “displayUnit” is not defined under <UnitDefinitions> <Unit>
     * <DisplayUnit>, then displayUnit is ignored. It is an error if
     * displayUnit is defined in element Real, but unit is not, or unit is not
     * defined under <UnitDefinitions><Unit>.
     */
    @XmlAttribute
    val displayUnit: String? = null

    /**
     * If this attribute is true, then the “offset” of “displayUnit” must be ignored
     * (for example 10 degree Celsius = 10 Kelvin if “relativeQuantity = true”
     * and not 283,15 Kelvin).
     */
    @XmlAttribute
    val relativeQuantity: String? = null

}

@XmlAccessorType(XmlAccessType.FIELD)
internal class StringAttribute  {

    /**
     * @inheritDoc
     */
    @XmlAttribute
     val start: String? = null

}

@XmlAccessorType(XmlAccessType.FIELD)
internal class BooleanAttribute  {

    /**
     * @inheritDoc
     */
    @XmlAttribute
     val start: Boolean? = null

}


sealed class TypedScalarVariable<E> : ScalarVariable {

    /**
     * Initial or guess value of variable. This value is also stored in the C functions
     * [Therefore, calling fmi2SetXXX to set start values is only necessary, if a different
     * value as stored in the xml file is desired.] The interpretation of start is defined by
     * ScalarVariable / initial. A different start value can be provided with an
     * fmi2SetXXX function before fmi2ExitInitializationMode is called (but not
     * for variables with variability = ″constant″).
     * [The standard approach is to set the start value before
     * fmi2EnterInitializationMode. However, if the initialization shall be modified
     * in the calling environment (e.g. changing from initialization of states to steadystate
     * initialization), it is also possible to use the start value as iteration variable of
     * an algebraic loop: Via an additional condition in the environment, such as 𝑥̇ = 0,
     * the actual start value is determined.]
     */
    override abstract var start: E?

}


class IntegerVariable(v : ScalarVariableImpl) : ScalarVariable by v, TypedScalarVariable<Int>() {

    /**
     * @see IntegerAttribute.min
     */
    val min: Int? = v.integerAttribute!!.min
    /**
     * @see IntegerAttribute.max
     */
    val max: Int? = v.integerAttribute!!.max

    /**
     * @see IntegerAttribute.start
     */
    override var start = v.integerAttribute!!.start

    override val typeName = "Integer"

    override fun toString(): String {
        return "IntegerVariable(min=$min, max=$max, start=$start)"
    }

}

class RealVariable(v : ScalarVariableImpl) : ScalarVariable by v, TypedScalarVariable<Double>() {

    /**
     * @see RealAttribute.min
     */
    val min = v.realAttribute!!.min

    /**
     * @see RealAttribute.max
     */
    val max = v.realAttribute!!.max

    /**
     * @see RealAttribute.nominal
     */
    val nominal = v.realAttribute!!.nominal

    /**
     * @see RealAttribute.unbounded
     */
    val unbounded = v.realAttribute!!.unbounded

    /**
     * @see RealAttribute.quantity
     */
    val quantity = v.realAttribute!!.quantity

    /**
     * @see RealAttribute.unit
     */
    val unit = v.realAttribute!!.unit

    /**
     * @see RealAttribute.displayUnit
     */
    val displayUnit = v.realAttribute!!.displayUnit

    /**
     * @see RealAttribute.relativeQuantity
     */
    val relativeQuantity = v.realAttribute!!.relativeQuantity

    /**
     * @see RealAttribute.derivative
     */
    val derivative = v.realAttribute!!.derivative

    /**
     * @see RealAttribute.start
     */
    override var start = v.realAttribute!!.start

    override val typeName = "Real"

    override fun toString(): String {
        return "RealVariable(min=$min, max=$max, nominal=$nominal, unbounded=$unbounded, quantity=$quantity, unit=$unit, displayUnit=$displayUnit, relativeQuantity=$relativeQuantity, derivative=$derivative, start=$start)"
    }

}

class StringVariable(v : ScalarVariableImpl) : ScalarVariable by v, TypedScalarVariable<String>() {

    /**
     * @see StringAttribute.start
     */
    override var start = v.stringAttribute!!.start

    override val typeName = "String"

    override fun toString(): String {
        return "StringVariable(start=$start)"
    }

}

class BooleanVariable(v : ScalarVariableImpl) : ScalarVariable by v, TypedScalarVariable<Boolean>() {

    /**
     * @see BooleanAttribute.start
     */
    override var start = v.booleanAttribute!!.start

   override val typeName = "Boolean"

    override fun toString(): String {
        return "BooleanVariable(start=$start)"
    }

}


class ScalarVariableAdapter : XmlAdapter<Any, TypedScalarVariable<*>>() {

    @Throws(Exception::class)
    override fun unmarshal(v: Any): TypedScalarVariable<*> {

        val node = v as Node
        val child = node.childNodes.item(0)

        val unmarshal by lazy {
            val ctx = JAXBContext.newInstance(ScalarVariableImpl::class.java)
            val unmarshaller = ctx.createUnmarshaller()
            unmarshaller.unmarshal(node, ScalarVariableImpl::class.java).value
        }

        when (child.nodeName) {

            "Integer" -> return IntegerVariable(unmarshal)
            "Real" -> return RealVariable(unmarshal)
            "String" -> return StringVariable(unmarshal)
            "Boolean" -> return BooleanVariable(unmarshal)
            else -> throw RuntimeException("Error parsing XML. Unable to understand of what type the ScalarVariable is..")

        }

    }

    override fun marshal(v: TypedScalarVariable<*>?): Any {
        TODO("not implemented")
    }

}
