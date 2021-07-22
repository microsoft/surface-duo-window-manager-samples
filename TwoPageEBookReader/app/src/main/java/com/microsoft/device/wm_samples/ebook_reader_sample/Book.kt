/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */
package com.microsoft.device.wm_samples.ebook_reader_sample

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Rect
import android.text.Html
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.max
import kotlin.math.min

class Book(private val filePath: String, private val activityContext: Context) {
    private val assetManager: AssetManager = activityContext.assets
    private var chapterStarts: ArrayList<Int> = ArrayList()
    private var chapterLengths: ArrayList<Int> = ArrayList()

    val numChapters
        get() = chapterStarts.size

    var chapterStartString = activityContext.resources.getString(R.string.chapter_start)
    var chapterEndString = activityContext.resources.getString(R.string.chapter_end)

    val chapterTitle
        get() = "(Section ${currentChapter}) ${paragraphStrings[0]}"

    var currentChapter = 0
        set(inChapter) {
            _setCurrentChapter(inChapter)
            field = inChapter
        }

    private lateinit var paragraphStrings: ArrayList<String>
    private var currentParagraph = 0

    private lateinit var pageStrings: ArrayList<ArrayList<String>>

    val numPages
        get() = pageStrings.size

    var currentPage: Int
        get() = _getCurrentPage()
        set(inPage) = _setCurrentPage(inPage)

    val pagePadding = activityContext.resources.getDimension(R.dimen.page_padding).toInt()
    var pageRects = ArrayList<Rect>()
        set(input) {
            field = input
            buildPages()
        }

    var fontSize = 16
        set(input) {
            field = input
            buildPages()
        }

    init {
        constructor()
    }

    private fun constructor() {
        val bufferedReader = BufferedReader(InputStreamReader(assetManager.open(filePath)))

        var line = 0
        var lineCount = 0
        var lineString: String? = ""
        var emptyLineCount = 2

        do {
            if (lineString!!.isEmpty()) {
                emptyLineCount += 1
            } else {
                if (emptyLineCount >= 3) {
                    chapterStarts.add(line)
                    chapterLengths.add(lineCount)
                    lineCount = 0
                }
                emptyLineCount = 0
            }
            line += 1
            lineCount += 1
            lineString = bufferedReader.readLine()
        } while (lineString != null)
        bufferedReader.close()

        chapterLengths.add(lineCount)
        chapterLengths.removeAt(0)
    }

    private fun _setCurrentChapter(inChapter: Int) {
        paragraphStrings = ArrayList()

        if (inChapter >= 0 && inChapter < chapterStarts.size) {
            val bufferedReader = BufferedReader(InputStreamReader(assetManager.open(filePath)))

            for (line in 0 until chapterStarts[inChapter] - 1) {
                bufferedReader.readLine()
            }

            var paragraphBuffer = StringBuffer()
            var lineBuffer: String
            for (line in 0 until chapterLengths[inChapter]) {
                lineBuffer = bufferedReader.readLine()
                if (lineBuffer.isEmpty()) {
                    if (paragraphBuffer.isNotEmpty()) {
                        paragraphStrings.add(formatGutenbergToHTML(paragraphBuffer.toString()))
                        paragraphBuffer = StringBuffer()
                    }
                } else {
                    paragraphBuffer.append("$lineBuffer ")
                }
            }
            bufferedReader.close()
        }

        buildPages()
    }

    fun getPageStrings(index: Int): ArrayList<String> {
        return if (index < pageStrings.size && index >= 0) {
            pageStrings[index]
        }
        else {
            ArrayList()
        }
    }

    private fun _getCurrentPage(): Int {
        var page = 0
        var remainingParagraphs = currentParagraph
        while (page < pageStrings.size && pageStrings[page].size <= remainingParagraphs ) {
            remainingParagraphs -= pageStrings[page].size
            page += 1
        }
        return page
    }

    private fun _setCurrentPage(inPage: Int) {
        currentParagraph = 0
        for (page in 0 until min(inPage, pageStrings.size)) {
            currentParagraph += pageStrings[page].size
        }
    }

    private fun measureTextHeight(string: String, width: Int): Int {
        val textView = View.inflate(activityContext, R.layout.book_page_text_paragraph, null) as TextView
        textView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        textView.textSize = fontSize.toFloat()
        textView.text = Html.fromHtml(string)
        var widthSpec = View.MeasureSpec.makeMeasureSpec(width - (2 * pagePadding), View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        textView.measure(widthSpec, heightSpec)
        return textView.measuredHeight
    }

    //buildPages() uses the available rects of the current layout to organize the chapter's strings into pages
    //TODO buildPages() supports multiple pages by cycling through pageRects instead of using only one Rect of constraints
    fun buildPages() {
        if (pageRects.isEmpty()) { return }

        pageStrings = ArrayList()

        val paragraphStringsCopy = paragraphStrings.clone() as ArrayList<String>
        val paragraphIt = paragraphStringsCopy.listIterator()

        var textStrings = ArrayList<String>()
        var pageRectIndex = 0
        var availableHeight = pageRects[pageRectIndex].height() - (2 * pagePadding)

        while (paragraphIt.hasNext()) {
            val paragraphString = paragraphIt.next()
            val measuredHeight = measureTextHeight(paragraphString, pageRects[pageRectIndex].width())

            if (measuredHeight > availableHeight) {
                var splitIndex = paragraphString.length + 1
                var measuredHeight2 = measuredHeight

                while (splitIndex > 0 && measuredHeight2 > availableHeight) {
                    splitIndex = max(0, paragraphString.lastIndexOf(' ',splitIndex - 1))
                    measuredHeight2 = measureTextHeight(paragraphString.substring(0, splitIndex), pageRects[pageRectIndex].width())
                }

                textStrings.add(paragraphString.substring(0, splitIndex))
                paragraphIt.add(paragraphString.substring(splitIndex))
                paragraphIt.previous()

                pageStrings.add(textStrings)
                textStrings = ArrayList()
                pageRectIndex = (pageRectIndex + 1) % pageRects.size
                availableHeight = pageRects[pageRectIndex].height() - (2 * pagePadding)

            } else {
                availableHeight -= measuredHeight
                textStrings.add(paragraphString)
            }
        }

        pageStrings.add(textStrings)
    }

    private fun formatGutenbergToHTML(inString: String): String {
        val step1 = inString.replace("\\s+".toRegex(), " ")
        val step2 = step1.replace("_(?<italics>((?!_).)+)_".toRegex(),"<i>\${italics}</i>")

        return "\t$step2"
    }
}
