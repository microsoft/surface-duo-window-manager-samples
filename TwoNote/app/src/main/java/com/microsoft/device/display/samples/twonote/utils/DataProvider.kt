/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.samples.twonote.utils

import com.microsoft.device.display.samples.twonote.models.INode
import kotlin.math.max

/**
 * Object that manages currently active inodes and categories within the application
 * Acts like a cache for inodes, @see FileSystem for file persistence
 */
object DataProvider {
    private val inodes: MutableList<INode> = mutableListOf()
    private val categories: MutableList<INode> = mutableListOf()
    private var highestINodeId = 0
    private var highestCategoryId = 0

    /**
     * Add a new inode to the inode list
     *
     * @param inode: inode to add to the list
     */
    fun addINode(inode: INode) {
        inodes.add(0, inode)
        highestINodeId = max(inode.id, highestINodeId)
    }

    /**
     * Remove a specified inode from the list
     *
     * @param inode: inode to remove from the list
     */
    fun removeINode(inode: INode) {
        inodes.remove(inode)
    }

    /**
     * Get the list of inodes associated with the currently active category
     *
     * @return the list of currently active inodes
     */
    fun getINodes(): MutableList<INode> {
        return inodes
    }

    /**
     * Set a specified inode to be the most recently edited (moved to the top)
     *
     * @param inode: inode to update
     */
    fun moveINodeToTop(inode: INode) {
        if (inodes.remove(inode))
            inodes.add(0, inode)
    }

    /**
     * Find the next inode id that ensures no repeats
     *
     * @return a new unique inode id
     */
    fun getNextInodeId(): Int {
        return highestINodeId + 1
    }

    /**
     * Add a new category to the category list
     *
     * @param category: category to add to the list
     */
    fun addCategory(category: INode) {
        categories.add(0, category)
        highestCategoryId = max(category.id, highestCategoryId)
    }

    /**
     * Remove a specified category from the list
     *
     * @param category to remove from the list
     */
    fun removeCategory(category: INode) {
        categories.remove(category)
    }

    /**
     * Add a new inode to the inode list
     *
     * @return the list of all current categories
     */
    fun getCategories(): MutableList<INode> {
        return categories
    }

    /**
     * Set a specified category to be the active category (moved to the top)
     *
     * @param category: category to update
     */
    fun moveCategoryToTop(category: INode) {
        if (categories.remove(category))
            categories.add(0, category)
    }

    /**
     * Find the next category id that ensures no repeats
     *
     * @return a new unique category id
     */
    fun getNextCategoryId(): Int {
        return highestCategoryId + 1
    }

    /**
     * Get the subdirectory path associated with the currently active category
     *
     * @return a string representing the active category's directory path
     */
    fun getActiveSubDirectory(): String {
        return if (categories.isNotEmpty()) {
            "/c${categories[0].id}"
        } else {
            ""
        }
    }

    /**
     * Get the title associated with the currently active category
     *
     * @return the title of the active category
     */
    fun getActiveCategoryName(): String {
        return if (categories.isNotEmpty())
            categories[0].title
        else
            ""
    }

    /**
     * Set the title associated with the currently active category
     *
     * @param title: new title for the category
     */
    fun setActiveCategoryName(title: String) {
        if (categories.isNotEmpty())
            categories[0].title = title
    }

    /**
     * Remove all inodes associated with the currently active category
     */
    fun clearInodes() {
        for (inode in inodes.size - 1 downTo 0) {
            inodes.removeAt(inode)
        }
        highestINodeId = 0
    }
}
