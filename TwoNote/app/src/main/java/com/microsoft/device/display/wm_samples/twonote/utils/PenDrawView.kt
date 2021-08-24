/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.twonote.utils

import Defines.DEFAULT_THICKNESS
import Defines.ERASER_RADIUS
import Defines.LAND_TO_PORT
import Defines.OPAQUE
import Defines.PORT_TO_LAND
import Defines.TRANSPARENT
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import com.microsoft.device.display.wm_samples.twonote.models.SerializedStroke
import com.microsoft.device.display.wm_samples.twonote.models.Stroke
import kotlin.math.min

/**
 * Custom view that allows users to draw and erase on a virtual canvas
 * Pen events tested with Surface Pen, other pens have not been verified
 */
class PenDrawView : View {
    // Stroke-related attributes
    private var strokeList: MutableList<Stroke> = mutableListOf()
    private val eraser = RectF()

    // Canvas and drawing properties
    private var currentColor: Int = 0
    private var currentThickness: Int = DEFAULT_THICKNESS
    private var disabled = true
    private var eraserMode = false
    private var highlightMode = false
    private var isErasing = false
    private var rotated = false

    companion object {
        // Attributes used for scaling drawings based on rotation
        private var scaledPath = Path()
        private var scaledBound = RectF()
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    private fun init() {
        currentColor = Color.RED
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /**
     * Event triggered when canvas is initialized or cleared
     * Iterates through strokeList and renders all strokes
     *
     * @param canvas: virtual canvas to draw strokes on
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (strokeList.isNotEmpty()) {
            var line = 0
            while (line < strokeList.size && line >= 0) {
                val stroke = strokeList[line]
                val pathList = stroke.getPathList()
                val bounds = stroke.getBounds()
                val pressures = stroke.getPressure()
                val thickness = stroke.getThickness()
                val color = stroke.getColor()

                if (pathList.isNotEmpty()) {
                    var section = 0
                    while (section < stroke.getSize() && section >= 0) {
                        val bound = bounds[section]
                        val path = pathList[section]
                        val strokeWidth = pressures[section][0] * thickness
                        val diffRotations = rotated != stroke.getRotation()
                        val matrix = when (rotated) {
                            true -> PORT_TO_LAND
                            false -> LAND_TO_PORT
                        }

                        // If strokes were drawn in a different rotation state than is currently displayed,
                        // transform their paths and bounds to match the current rotation state
                        if (diffRotations) {
                            matrix.mapRect(scaledBound, bound)
                            path.transform(matrix, scaledPath)
                        }

                        // If a stroke or path is removed, decrease the index so the next stroke/path doesn't get skipped
                        if (isErasing && ((!diffRotations && bound.intersect(eraser)) || (diffRotations && scaledBound.intersect(eraser)))) {
                            val newStroke = stroke.removeItem(section)
                            section--
                            // Add split stroke to next position in stroke list to maintain chronological order of strokes for undo
                            newStroke?.let { strokeList.add(line + 1, newStroke) }
                            if (stroke.getSize() == 0) {
                                strokeList.removeAt(line)
                                line--
                            }
                        } else {
                            val configurePaint = configurePaint(color, strokeWidth, stroke.getHighlight())
                            val pathToDraw = if (diffRotations) scaledPath else path
                            canvas.drawPath(pathToDraw, configurePaint)
                        }
                        section++
                    }
                }
                line++
            }
        }
    }

    /**
     * Listener for touch events on the canvas
     *
     * @param event: touch event triggered by the user
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (disabled)
            return true

        isErasing = false
        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER || eraserMode) {
            configureEraser(event)
        } else {
            handleInkingEvent(event)
        }
        invalidate()

        return true
    }

    /**
     * Initialize new eraser coordinates and bounds
     *
     * @param event: touch event triggered by the user
     */
    private fun configureEraser(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            isErasing = true

            val offset = ERASER_RADIUS
            val left = (event.x - offset).coerceAtLeast(0f)
            val right = min(event.x + offset, width.toFloat() - 1)
            val top = min(event.y - offset, height.toFloat() - 1)
            val bottom = (event.y + offset).coerceAtLeast(0f)
            eraser.set(left, top, right, bottom)
        }
    }

    /**
     * Add new coordinate to current list of strokes
     *
     * @param event: touch event triggered by the user
     */
    private fun handleInkingEvent(event: MotionEvent) {
        // Keep constant pressure if in highlight mode (1 = normal pressure)
        val pressure = if (highlightMode) 1f else event.pressure
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val stroke = Stroke(event.x, event.y, pressure, currentColor, currentThickness, rotated, highlightMode)
                strokeList.add(stroke)
            }
            MotionEvent.ACTION_MOVE -> {
                if (strokeList.isNotEmpty())
                    strokeList[strokeList.lastIndex].continueDrawing(event.x, event.y, pressure)
            }
            MotionEvent.ACTION_UP -> {
                if (strokeList.isNotEmpty())
                    strokeList[strokeList.lastIndex].finishPath()
            }
        }
    }

    /**
     * Create a paint object to associate with a path when drawing
     *
     * @param color: int value of color to paint with
     * @param strokeWidth: width brush stroke associated with paint
     * @param: highlight: true if the app is in highlight mode, false by default
     * @return Paint object with given parameters
     */
    private fun configurePaint(color: Int, strokeWidth: Float, highlight: Boolean = false): Paint {
        val configuredPaint = Paint()

        configuredPaint.style = Paint.Style.STROKE
        configuredPaint.isAntiAlias = true
        configuredPaint.strokeCap = if (highlight) Paint.Cap.SQUARE else Paint.Cap.ROUND
        configuredPaint.strokeWidth = strokeWidth
        configuredPaint.color = color

        return configuredPaint
    }

    /**
     * Change color of virtual paintbrush
     *
     * @param color: int value of color to change to
     */
    fun changePaintColor(color: Int) {
        // alpha values range from 0 (transparent) to 255 (opaque)
        currentColor = if (highlightMode)
            ColorUtils.setAlphaComponent(color, TRANSPARENT)
        else
            ColorUtils.setAlphaComponent(color, OPAQUE)
    }

    /**
     * Change thickness of virtual paintbrush
     *
     * @param thickness: new width of paint strokes
     */
    fun changeThickness(thickness: Int) {
        currentThickness = thickness
    }

    /**
     * Enable/disable highlighting
     *
     * @param force: if force is not null, set highlight mode to the value of force
     *                  else toggle value of highlight mode
     * @return the value of highlight mode after it has been toggled
     */
    fun toggleHighlightMode(force: Boolean? = null): Boolean {
        highlightMode = force ?: !highlightMode
        changePaintColor(currentColor)
        return highlightMode
    }

    /**
     * Enable/disable forced erasing (non-stylus erasing)
     *
     * @param force: if force is not null, set eraser mode to the value of force
     *                  else toggle value of eraser mode
     * @return the value of eraser mode after it has been toggled
     */
    fun toggleEraserMode(force: Boolean? = null): Boolean {
        eraserMode = force ?: !eraserMode
        return eraserMode
    }

    /**
     * Initialize canvas with a list of drawings
     *
     * @param s: list of strokes to initialize the canvas with
     */
    fun setStrokeList(s: List<Stroke>) {
        strokeList = s.toMutableList()
    }

    /**
     * Update rotation of canvas after device rotation has changed
     *
     * @param rotation: true if application is rotated, false otherwise
     */
    fun setRotation(rotation: Boolean) {
        rotated = rotation
    }

    /**
     * Get list of serialized drawings from canvas
     *
     * @return list of strokes from canvas after they have been serialized
     */
    fun getDrawingList(): List<SerializedStroke> {
        val list: MutableList<SerializedStroke> = mutableListOf()
        for (stroke in strokeList) {
            list.add(stroke.serializeData())
        }
        return list.toList()
    }

    /**
     * Completely clear canvas
     */
    fun clearDrawing() {
        strokeList.clear()
        invalidate()
    }

    /**
     * Undo last drawing made
     */
    fun undo() {
        if (strokeList.isNotEmpty()) {
            strokeList.removeAt(strokeList.lastIndex)
            invalidate()
        }
    }

    /**
     * Disable drawing on canvas
     */
    fun disable() {
        disabled = true
    }

    /**
     * Enable drawing on canvas
     */
    fun enable() {
        disabled = false
    }
}
