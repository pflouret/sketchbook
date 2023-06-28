package gui

import com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerDotCaseStrategy
import com.krab.lazy.LazyGui
import p5.ProcessingAppK
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KCallable
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaType

class LazyGuiControlDelegate<T>(
    private val controlType: String,
    folder: String = "",
    private val defaultValue: Any? = null,
) : ReadWriteProperty<ProcessingAppK, T> {
    companion object {
        private val KOT_TO_JVM_SIMPLE_NAME = mapOf(
            "Float" to "float",
            "Integer" to "int",
            "Double" to "double",
            "Boolean" to "boolean",
        )
    }
    private val folder = folder.replace("/?$", "/")
    private lateinit var controlName: String
    private lateinit var getter: KCallable<*>
    private lateinit var setter: KCallable<*>

    @Suppress("UNCHECKED_CAST")
    override operator fun getValue(thisRef: ProcessingAppK, property: KProperty<*>): T {
        if (!::getter.isInitialized) {
            initialize(thisRef, property.name)
        }
        return getter.call(thisRef.gui, controlName) as T
    }

    override operator fun setValue(thisRef: ProcessingAppK, property: KProperty<*>, value: T) {
        if (!::setter.isInitialized) {
            initialize(thisRef, property.name)
        }
        setter.call(thisRef.gui, controlName, value)
    }

    private fun effectiveFolder(thisRef: ProcessingAppK) = folder.replace(thisRef.gui.folder, "")
    private fun controlPath(thisRef: ProcessingAppK) =
        "${effectiveFolder(thisRef)}$controlName"

    private fun initialize(thisRef: ProcessingAppK, propertyName: String) {
        controlName = LowerDotCaseStrategy().translate(propertyName).replace(".", " ")

        if (defaultValue != null) {
            LazyGui::class.members
                .find(this::matchingDefaultValueMethod)!!
                .call(thisRef.gui, controlPath(thisRef), defaultValue)
        }

        val setterName = "${controlType}Set"
        getter = thisRef.gui::class.members
            .find { it.name == controlType && it.parameters.size == 2 }!!
        setter = thisRef.gui::class.members
            .find { it.name == setterName && it.parameters.size == 3 }!!
    }

    private fun matchingDefaultValueMethod(method: KCallable<*>): Boolean =
        method.name == controlType
                && method.parameters.size == 3
                && (method.parameters[2].type.javaType as Class<*>).simpleName ==
                defaultValue!!::class.java.simpleName.let {
                    KOT_TO_JVM_SIMPLE_NAME.getOrDefault(it, it)
                }
}