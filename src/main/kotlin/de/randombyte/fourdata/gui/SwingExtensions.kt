package de.randombyte.fourdata.gui

import java.awt.Component
import java.awt.Container
import javax.swing.BoxLayout
import javax.swing.JPanel
import kotlin.reflect.full.createInstance

fun JPanel.horizontalLayout(init: JPanel.() -> Unit) = boxLayout(BoxLayout.X_AXIS, init)
fun JPanel.verticalLayout(init: JPanel.() -> Unit) = boxLayout(BoxLayout.Y_AXIS, init)

fun JPanel.boxLayout(axis: Int, init: JPanel.() -> Unit): JPanel {
    JPanel().apply {
        layout = BoxLayout(this, axis)
        init(this)
    }.also { add(it) }

    return this
}

fun <T : Component> JPanel.addComponent(component: T) = component.also { add(it) }

inline fun <reified T : Component> Container.create(init: T.() -> Unit)  = T::class.createInstance().apply(init).also { add(it) }