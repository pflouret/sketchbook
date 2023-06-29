package gui

import com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerDotCaseStrategy
import com.krab.lazy.LazyGui
import com.krab.lazy.stores.GlobalReferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KCallable
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaType

class LazyGuiControlDelegate<T>(
    private val controlType: String,
    folder: String = "",
    private val defaultValue: Any? = null,
) : ReadWriteProperty<Any, T> {
    companion object {
        private val gui by lazy { GlobalReferences.gui }
        private val KOT_TO_JVM_SIMPLE_NAME = mapOf(
            "Float" to "float",
            "Integer" to "int",
            "Double" to "double",
            "Boolean" to "boolean",
        )
    }
    private val folder = folder.replace("/?$".toRegex(), "/")
    private lateinit var controlName: String
    private lateinit var getter: KCallable<*>
    private lateinit var setter: KCallable<*>

    @Suppress("UNCHECKED_CAST")
    override operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        if (!::getter.isInitialized) {
            initialize(property.name)
        }
        return getter.call(gui, controlPath()) as T
    }

    override operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        if (!::setter.isInitialized) {
            initialize(property.name)
        }
        setter.call(gui, controlPath(), value)
    }

    private fun effectiveFolder() = folder.replace(gui.folder, "")
    private fun controlPath() = "${effectiveFolder()}$controlName"

    private fun initialize(propertyName: String) {
        controlName = LowerDotCaseStrategy().translate(propertyName).replace(".", " ")

        if (defaultValue != null) {
            LazyGui::class.members
                .find(this::matchingDefaultValueMethod)!!
                .call(gui, controlPath(), defaultValue)
        }

        getter = gui::class.members
            .find { it.name == controlType && it.parameters.size == 2 }!!

        val setterName = when (controlType) {
            "plotXY" -> "plotSet"
            else -> "${controlType}Set"
        }
        setter = when (controlType) {
            "button" -> gui::buttonSet
            else -> gui::class.members.find { it.name == setterName && it.parameters.size == 3 }!!
        }
    }

    private fun matchingDefaultValueMethod(method: KCallable<*>): Boolean =
        method.name == controlType
                && method.parameters.size == 3
                && (method.parameters[2].type.javaType as Class<*>).simpleName ==
                defaultValue!!::class.java.simpleName.let {
                    KOT_TO_JVM_SIMPLE_NAME.getOrDefault(it, it)
                }
}