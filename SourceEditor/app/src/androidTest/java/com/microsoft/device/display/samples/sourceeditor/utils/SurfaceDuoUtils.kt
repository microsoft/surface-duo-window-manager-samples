/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.samples.sourceeditor.utils

import android.view.Surface
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

// testing device
private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

private const val leftX: Int = 675 //           middle of left screen
private const val rightX: Int = 2109 //         middle of right screen
private const val leftMiddleX: Int = 1340 //    left of hinge area
private const val rightMiddleX: Int = 1434 //   right of hinge area
private const val bottomY: Int = 1780 //        bottom of screen
private const val middleY: Int = 900 //         middle of screen
private const val spanSteps: Int = 400 //       spanning/unspanning swipe
private const val switchSteps: Int = 600 //     switch from one screen to the other

fun spanFromLeft() {
    device.swipe(leftX, bottomY, leftMiddleX, middleY, spanSteps)
}

fun unspanToLeft() {
    device.swipe(rightX, bottomY, leftX, middleY, spanSteps)
}

fun spanFromRight() {
    device.swipe(rightX, bottomY, rightMiddleX, middleY, spanSteps)
}

fun unspanToRight() {
    device.swipe(leftX, bottomY, rightX, middleY, spanSteps)
}

fun moveToLeft() {
    device.swipe(rightX, bottomY, leftX, middleY, switchSteps)
}

fun moveToRight() {
    device.swipe(leftX, bottomY, rightX, middleY, switchSteps)
}

/**
 * Switches application from single screen mode to dual screen mode
 */
fun switchFromSingleToDualScreen() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    when (device.displayRotation) {
        Surface.ROTATION_0, Surface.ROTATION_180 -> device.swipe(675, 1780, 1350, 900, 400)
        Surface.ROTATION_270 -> device.swipe(1780, 675, 900, 1350, 400)
        Surface.ROTATION_90 -> device.swipe(1780, 2109, 900, 1400, 400)
    }
}

/**
 * Switches application from dual screen mode to single screen
 */
fun switchFromDualToSingleScreen() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    when (device.displayRotation) {
        Surface.ROTATION_0, Surface.ROTATION_180 -> device.swipe(1500, 1780, 650, 900, 400)
        Surface.ROTATION_270 -> device.swipe(1780, 1500, 900, 650, 400)
        Surface.ROTATION_90 -> device.swipe(1780, 1250, 900, 1500, 400)
    }
}

/**
 * Re-enables the sensors and un-freezes the device rotation allowing its contents
 * to rotate with the device physical rotation. During a test execution, it is best to
 * keep the device frozen in a specific orientation until the test case execution has completed.
 */
fun unfreezeRotation() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.unfreezeRotation()
}

/**
 * Simulates orienting the device to the left and also freezes rotation
 * by disabling the sensors.
 */
fun setOrientationLeft() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.setOrientationLeft()
}

/**
 * Simulates orienting the device into its natural orientation and also freezes rotation
 * by disabling the sensors.
 */
fun setOrientationNatural() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.setOrientationNatural()
}

/**
 * Simulates orienting the device to the right and also freezes rotation
 * by disabling the sensors.
 */
fun setOrientationRight() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.setOrientationRight()
}
