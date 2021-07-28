/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.samples.twonote.utils

import android.content.Context
import android.util.Log
import com.microsoft.device.display.samples.twonote.R
import com.microsoft.device.display.samples.twonote.models.DirEntry
import com.microsoft.device.display.samples.twonote.models.INode
import com.microsoft.device.display.samples.twonote.models.Note
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.Exception

/**
 * Object to handle file persistence. Functions include saving and loading various data structures
 * from memory as well as interacting with local memory. @see DataProvider for more information on
 * how data structures are stored locally.
 */
object FileSystem {
    /**
     * Load inode information from the current directory into the DataProvider
     *
     * @param context: status information about the application
     * @param subDir: directory in device memory to access
     */
    private fun loadDirectory(context: Context, subDir: String) {
        DataProvider.clearInodes()
        readDirEntry(context, subDir)?.let { notes ->
            for (inode in notes.inodes.size - 1 downTo 0) {
                DataProvider.addINode(notes.inodes[inode])
            }
        }
    }

    /**
     * Create a new inode and add it to the DataProvider
     *
     * @param context: status information about the application
     */
    fun addInode(context: Context) {
        val prefix = context.resources.getString(R.string.default_note_name)
        val id = DataProvider.getNextInodeId()
        val inode = INode("$prefix $id", id = id)
        DataProvider.addINode(inode)
    }

    /**
     * Load category information from root into the DataProvider
     *
     * @param context: status information about the application
     * @param subDir: directory in device memory to access
     */
    fun loadCategories(context: Context, subDir: String) {
        if (DataProvider.getCategories().isEmpty()) {
            readDirEntry(context, subDir)?.let { dir ->
                for (category in dir.inodes.size - 1 downTo 0) {
                    DataProvider.addCategory(dir.inodes[category])
                }
            }

            // If no categories were found, make a new one
            if (DataProvider.getCategories().isEmpty())
                addCategory(context)

            loadDirectory(context, DataProvider.getActiveSubDirectory())
        }
    }

    /**
     * Create a new category and add it to the DataProvider
     *
     * @param context: status information about the application
     */
    private fun addCategory(context: Context) {
        val prefix = context.resources.getString(R.string.default_category_name)
        val id = DataProvider.getNextCategoryId()
        val inode = INode("$prefix $id", descriptor = "/c", id = id)
        DataProvider.addCategory(inode)
    }

    /**
     * Load DataProvider with inodes from given category (switches from one category to another)
     *
     * @param context: status information about the application
     * @param category: new category to switch to
     */
    fun switchCategory(context: Context, category: INode?) {
        var newNode = category
        if (newNode == null) {
            addCategory(context)
            newNode = DataProvider.getCategories()[0]
        }
        DataProvider.moveCategoryToTop(newNode)
        loadDirectory(context, DataProvider.getActiveSubDirectory())
    }

    /**
     * Read a file and parse note data
     *
     * @param context: status information about the application
     * @param subDir: directory in device memory to access
     * @param noteName: file name of the note to load
     * @return if file exists, return loaded note data from memory
     *          otherwise return null
     */
    fun loadNote(context: Context, subDir: String, noteName: String): Note? {
        val path: String? = context.getExternalFilesDir(null)?.absolutePath
        val file = File(path + subDir + noteName)
        var fileStream: FileInputStream? = null
        var objectStream: ObjectInputStream? = null

        try {
            fileStream = FileInputStream(file)
            objectStream = ObjectInputStream(fileStream)
            val note = objectStream.readObject()
            return if (note is Note) {
                note
            } else {
                Log.e(this::class.java.toString(), context.resources.getString(R.string.load_error_note))
                null
            }
        } catch (e: Exception) {
            Log.e(this::class.java.toString(), e.message.toString())
            return null
        } finally {
            objectStream?.close()
            fileStream?.close()
        }
    }

    /**
     * Read a file and parse directory entry information
     *
     * @param context: status information about the application
     * @param subDir: directory in device memory to access
     * @return if file exists, return loaded directory entry from memory
     *          otherwise, return a new directory entry for the specified path
     */
    private fun readDirEntry(context: Context, subDir: String): DirEntry? {
        val path: String? = context.getExternalFilesDir(null)?.absolutePath
        val file = File("$path$subDir/dEntry")
        var fileStream: FileInputStream? = null
        var objectStream: ObjectInputStream? = null

        try {
            fileStream = FileInputStream(file)
            objectStream = ObjectInputStream(fileStream)
            val entry = objectStream.readObject()
            return if (entry is DirEntry) {
                entry
            } else {
                Log.e(this::class.java.toString(), context.resources.getString(R.string.load_error_dir))
                null
            }
        } catch (e: Exception) {
            val entry = DirEntry()
            writeDirEntry(context, subDir, entry) // create a new dir entry
            return entry
        } finally {
            objectStream?.close()
            fileStream?.close()
        }
    }

    /**
     * Generate missing directories for a given path
     *
     * @param context: status information about the application
     * @param subDir: directory in device memory to access
     */
    private fun createDirectory(context: Context, subDir: String) {
        val path: String? = context.getExternalFilesDir(null)?.absolutePath
        val file = File(path + subDir)
        file.mkdirs()
    }

    /**
     * Update/create directory entry
     *
     * @param context: status information about the application
     * @param subDir: directory in device memory to access
     * @param entry: directory entry to write to memory
     */
    fun writeDirEntry(context: Context, subDir: String, entry: DirEntry) {
        val path: String? = context.getExternalFilesDir(null)?.absolutePath
        createDirectory(context, subDir)
        val file = File("$path$subDir/dEntry")
        val fileStream = FileOutputStream(file)
        val objectStream = ObjectOutputStream(fileStream)
        objectStream.writeObject(entry)
        objectStream.close()
        fileStream.close()
    }

    /**
     * Remove an inode and its associated content from memory
     *
     * @param context: status information about the application
     * @param subDir: directory in device memory to access
     * @param inode: inode to remove from memory
     * @return true if file deleted successfully, false otherwise
     */
    fun delete(context: Context, subDir: String, inode: INode): Boolean {
        val path: String? = context.getExternalFilesDir(null)?.absolutePath
        val file = File(path + subDir + inode.descriptor + inode.id)

        if (!DataProvider.getINodes().isNullOrEmpty()) {
            DataProvider.removeINode(inode)

            if (file.isFile) {
                return file.delete()
            }
            if (file.isDirectory) {
                return file.deleteRecursively()
            }
        }
        return false
    }

    /**
     * Save a note to memory at a given file path
     *
     * @param context: status information about the application
     * @param subDir: directory in device memory to access
     * @param note: note to be saved to device memory
     */
    fun save(context: Context, subDir: String, note: Note) {
        val path: String? = context.getExternalFilesDir(null)?.absolutePath
        val file = File("$path$subDir/n${note.id}")
        val fileStream = FileOutputStream(file)
        val objectStream = ObjectOutputStream(fileStream)
        objectStream.writeObject(note)
        objectStream.close()
        fileStream.close()
    }
}
