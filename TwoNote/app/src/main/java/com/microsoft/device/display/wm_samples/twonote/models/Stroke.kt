/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.twonote.models

import Defines.DEFAULT_THICKNESS
import android.graphics.Path
import android.graphics.RectF

/**
 * Class that encapsulates continuous paths with different associated pressures
 * Used to create a variable thickness line in inking and drawing events
 */
class Stroke {
    // Serializable fields
    private var xList: MutableList<MutableList<Float>> = mutableListOf()
    private var yList: MutableList<MutableList<Float>> = mutableListOf()
    private var pressureList: MutableList<MutableList<Float>> = mutableListOf()
    private var paintColor: Int = 0
    private var thicknessMultiplier: Int = DEFAULT_THICKNESS
    private var rotated = false
    private var highlightStroke = false

    // Path properties
    private var pathList: MutableList<Path> = mutableListOf()
    private var pathBounds: MutableList<RectF> = mutableListOf()
    private var xCoord: Float = 0f
    private var yCoord: Float = 0f
    private var prevPressure: Float = 0f

    /**
     * Create a new stroke with specified settings
     *
     * @param x: starting x coordinate of stroke
     * @param y: starting y coordinate of stroke
     * @param pressure: starting pressure of stroke
     * @param color: int value of color associated with stroke
     * @param thickness: thickness multiplier used for width calculations
     * @param rotation: rotation flag for stroke - true if rotated, false otherwise
     * @param highlight: highlighter flag for stroke - true if transparent, false by default
     */
    constructor(x: Float, y: Float, pressure: Float, color: Int, thickness: Int, rotation: Boolean, highlight: Boolean = false) {
        initStroke(x, y, pressure, color, thickness, rotation, highlight)
    }

    /**
     * Reconstruct a stroke using serialized data
     *
     * For each list of lists - each main index is associated with a specific path in the stroke
     *                          each sub index is associated with a coordinate for a given path
     *
     * @param x: collection of x coordinates
     * @param y: collection of y coordinates
     * @param pressure: collection of pressure values
     * @param color: int value of color for the stroke
     * @param thickness: thickness multiplier for width calculations
     * @param rotation: rotation flag for stroke - true if rotated, false otherwise
     * @param highlight: highlighter flag for stroke - true if transparent, false otherwise
     */
    constructor(
        x: List<MutableList<Float>>,
        y: List<MutableList<Float>>,
        pressure: List<MutableList<Float>>,
        color: Int,
        thickness: Int,
        rotation: Boolean,
        highlight: Boolean
    ) {
        // need at least one path
        if (x.isNotEmpty() && x[0].size > 0) {
            for (paths in x.indices) {
                for (coords in 0 until x[paths].size) {
                    if (paths == 0 && coords == 0)
                        initStroke(x[paths][coords], y[paths][coords], pressure[paths][coords], color, thickness, rotation, highlight)
                    else
                        continueDrawing(x[paths][coords], y[paths][coords], pressure[paths][coords])
                }
            }
            finishPath()
        }
    }

    /**
     * Initialize variables used for stroke creation
     *
     * @param x: starting x coordinate of stroke
     * @param y: starting y coordinate of stroke
     * @param pressure: starting pressure of stroke
     * @param color: int value of color associated with stroke
     * @param thickness: thickness multiplier used for width calculations
     * @param rotation: rotation flag for stroke - true if rotated, false otherwise
     * @param highlight: highlighter flag for stroke - true if transparent, false by default
     */
    private fun initStroke(x: Float, y: Float, pressure: Float, color: Int, thickness: Int, rotation: Boolean, highlight: Boolean = false) {
        xCoord = x
        yCoord = y
        prevPressure = pressure
        paintColor = color
        thicknessMultiplier = thickness
        rotated = rotation
        highlightStroke = highlight

        addPath(x, y, pressure)
    }

    /**
     * Initialize variables needed for a new path in this Stroke
     */
    private fun initPath() {
        xList.add(mutableListOf())
        yList.add(mutableListOf())
        pressureList.add(mutableListOf())
    }

    /**
     * Create a new path with a new pressure at given coordinates
     * Connect this path to the last path (if it exists) to ensure continuity
     *
     * @param x: x coordinate of new path endpoint
     * @param y: y coordinate of new path endpoint
     * @param pressure: pressure setting used for thickness calculation
     */
    private fun addPath(x: Float, y: Float, pressure: Float) {
        finishPath()
        initPath()

        val path = Path()
        path.moveTo(xCoord, yCoord)
        path.lineTo(x, y)
        pathList.add(path)
        pathBounds.add(RectF())

        updateValues(x, y, pressure)
    }

    /**
     * Extend the current path with a constant pressure to given coordinates
     *
     * @param x: x coordinate of new path endpoint
     * @param y: y coordinate of new path endpoint
     */
    private fun continuePath(x: Float, y: Float) {
        if (pathList.isNotEmpty()) {
            pathList[pathList.lastIndex].lineTo(x, y)
            updateValues(x, y, prevPressure)
        }
    }

    /**
     * A new coordinate has been added to this stroke, decide whether or not to add a new path
     *
     * @param x: x coordinate of path added
     * @param y: y coordinate of path added
     * @param pressure: pressure setting used for thickness calculation
     */
    fun continueDrawing(x: Float, y: Float, pressure: Float) {
        if (pressure == prevPressure && pathList.isNotEmpty())
            continuePath(x, y)
        else
            addPath(x, y, pressure)
    }

    /**
     * Record coordinates and other data associated with a newly added path to the stroke
     *
     * @param x: x coordinate of path added
     * @param y: y coordinate of path added
     * @param pressure: pressure setting used for thickness calculation
     */
    private fun updateValues(x: Float, y: Float, pressure: Float) {
        xCoord = x
        yCoord = y
        prevPressure = pressure

        xList[xList.lastIndex].add(x)
        yList[yList.lastIndex].add(y)
        pressureList[pressureList.lastIndex].add(pressure)
    }

    /**
     * Drawing of this path has ended, calculate bounds now that coordinates will no longer be added
     */
    fun finishPath() {
        if (pathList.isNotEmpty()) {
            val bounds = RectF()
            pathList[pathList.lastIndex].computeBounds(bounds, true)
            pathBounds.removeAt(pathBounds.lastIndex)
            pathBounds.add(bounds)
        }
    }

    /**
     * Break current stroke into two pieces
     * First stroke will have given size, new stroke will contain remaining paths
     *
     * @param i: index to break stroke at
     */
    private fun splitStroke(i: Int): Stroke {
        var range = IntRange(i + 1, xList.lastIndex)
        val x: List<MutableList<Float>> = xList.slice(range)
        val y: List<MutableList<Float>> = yList.slice(range)
        val p: List<MutableList<Float>> = pressureList.slice(range)

        val stroke = Stroke(x, y, p, paintColor, thicknessMultiplier, rotated, highlightStroke)

        range = IntRange(0, i)
        xList = xList.slice(range).toMutableList()
        yList = yList.slice(range).toMutableList()
        pressureList = pressureList.slice(range).toMutableList()
        pathBounds = pathBounds.slice(range).toMutableList()
        pathList = pathList.slice(range).toMutableList()

        return stroke
    }

    /**
     * Remove data associated with a given path
     *
     * @param i: index of the path to remove
     */
    private fun deleteComponents(i: Int) {
        xList.removeAt(i)
        yList.removeAt(i)
        pressureList.removeAt(i)
        pathBounds.removeAt(i)
        pathList.removeAt(i)
    }

    /**
     * Removes a specified path from the stroke
     * If the path is not an endpoint, the stroke is split into two pieces
     *
     * @param i: index of the path to remove
     * @return if i is not an endpoint, return the bottom half of the split stroke
     *          else return null
     */
    fun removeItem(i: Int): Stroke? {
        var stroke: Stroke? = null

        // split the stroke into two if middle is erased
        if (i > 0 && i < xList.lastIndex) {
            stroke = splitStroke(i)
        }

        deleteComponents(i)
        return stroke
    }

    /**
     * Convert this stroke to a serialized format using recorded coordinates details about each path
     */
    fun serializeData(): SerializedStroke {
        return SerializedStroke(xList, yList, pressureList, paintColor, thicknessMultiplier, rotated, highlightStroke)
    }

    /**
     * Get list of rectangular bounds defining the stroke
     *
     * @return
     */
    fun getBounds(): MutableList<RectF> {
        return pathBounds
    }

    /**
     * Get the color of the stroke
     *
     * @return int color value of the stroke
     */
    fun getColor(): Int {
        return paintColor
    }

    /**
     * Determine if the stroke has the highlighter property
     *
     * @return true if the stroke is transparent, false otherwise
     */
    fun getHighlight(): Boolean {
        return highlightStroke
    }

    /**
     * Get the collection of paths associated with the stroke
     *
     * @return list of paths
     */
    fun getPathList(): MutableList<Path> {
        return pathList
    }

    /**
     * Get the collection of pressures associated with the stroke
     *
     * @return collection of pressures
     *          each main index is associated with a specific path in the stroke
     *          each sub index is associated with a coordinate for a given path
     */
    fun getPressure(): MutableList<MutableList<Float>> {
        return pressureList
    }

    /**
     * Get rotation status of the stroke
     *
     * @return true if values of stroke have been transformed for rotation, false otherwise
     */
    fun getRotation(): Boolean {
        return rotated
    }

    /**
     * Get the number of paths in the stroke
     *
     * @return number of paths associated with this stroke
     */
    fun getSize(): Int {
        return pathList.size
    }

    /**
     * Get the thickness multiplier (directly related to the thickness bar value) of this stroke
     *
     * @return multiplier to be associated with thickness calculations
     */
    fun getThickness(): Int {
        return thicknessMultiplier
    }
}
