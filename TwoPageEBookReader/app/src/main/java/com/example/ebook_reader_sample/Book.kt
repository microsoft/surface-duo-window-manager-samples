/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */
package com.example.ebook_reader_sample

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Rect
import android.text.Html
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.min

class Book(private val filePath: String, private val activityContext: Context) {
    private val assetManager: AssetManager = activityContext.assets
    private var chapterStarts: ArrayList<Int> = ArrayList()
    private var chapterLengths: ArrayList<Int> = ArrayList()
    var numChapters = 0
        get() = chapterStarts.size
    var chapterStartString = activityContext.resources.getString(R.string.chapter_start)
    var chapterEndString = activityContext.resources.getString(R.string.chapter_end)

    init {
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

    private lateinit var paragraphStrings: ArrayList<String>
    var chapterTitle = ""
        get() = "(Section ${currentChapter}) ${paragraphStrings[0]}"

    var currentChapter = 0
        set(inChapter) {
            field = inChapter

            paragraphStrings = ArrayList()

            if (field >= 0 && field < chapterStarts.size) {
                val bufferedReader = BufferedReader(InputStreamReader(assetManager.open(filePath)))

                for (line in 0 until chapterStarts[field] - 1) {
                    bufferedReader.readLine()
                }

                var paragraphBuffer = StringBuffer()
                var lineBuffer = ""
                for (line in 0 until chapterLengths[field]) {
                    lineBuffer = bufferedReader.readLine()
                    if (lineBuffer.isEmpty()) {
                        if (!paragraphBuffer.isEmpty()) {
                            paragraphStrings.add(textStringHelper(paragraphBuffer.toString()))
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


    private var currentParagraph = 0

    private lateinit var pageStrings: ArrayList<ArrayList<String>>
    fun getPageStrings(index: Int): ArrayList<String> {
        return if (index < pageStrings.size && index >= 0) {
            pageStrings[index]
        }
        else {
            ArrayList()
        }
    }
    var numPages = 0
        get() = pageStrings.size

    var currentPage: Int
        get(){
            var page = 0
            var remainingParagraphs = currentParagraph
            while (page < pageStrings.size && pageStrings[page].size <= remainingParagraphs ) {
                remainingParagraphs -= pageStrings[page].size
                page += 1
            }
            return page
        }
        set(inPage) {
            currentParagraph = 0
            for (page in 0 until min(inPage, pageStrings.size)) {
                currentParagraph += pageStrings[page].size
            }
        }

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

    val pagePadding = activityContext.resources.getDimension(R.dimen.page_padding).toInt()

    fun buildPages() {
        if (pageRects.isEmpty()) { return }

        pageStrings = ArrayList()
        var textStrings = ArrayList<String>()

        var pageRectIndex = 0
        var availableHeight = pageRects[pageRectIndex].height() - (2 * pagePadding)
        var widthSpec = View.MeasureSpec.makeMeasureSpec(pageRects[pageRectIndex].width() - (2 * pagePadding), View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val paragraphStringsCopy = paragraphStrings.clone() as ArrayList<String>
        val paragraphIt = paragraphStringsCopy.listIterator()

        val textView = View.inflate(activityContext, R.layout.book_page_text_paragraph, null) as TextView
        textView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        textView.textSize = fontSize.toFloat()
        while (paragraphIt.hasNext()) {
            val paragraphString = paragraphIt.next()

            textView.text = Html.fromHtml(paragraphString)
            textView.measure(widthSpec,heightSpec)
            if (textView.measuredHeight > availableHeight) {
                var splitIndex = paragraphString.lastIndexOf(". ")
                while (splitIndex != -1) {
                    textView.text = Html.fromHtml(paragraphString.substring(0,splitIndex + 2))
                    textView.measure(widthSpec,heightSpec)
                    if (textView.measuredHeight < availableHeight) {
                        textStrings.add(paragraphString.substring(0,splitIndex + 2))
                        break;
                    }

                    splitIndex = paragraphString.lastIndexOf(". ", splitIndex - 1)
                }

                if (splitIndex == -1) {
                    paragraphIt.add(paragraphString)
                }
                else {
                    paragraphIt.add(paragraphString.substring(splitIndex + 2))
                }
                paragraphIt.previous()

                pageStrings.add(textStrings)
                textStrings = ArrayList()
                pageRectIndex = (pageRectIndex + 1) % pageRects.size
                widthSpec = View.MeasureSpec.makeMeasureSpec(pageRects[pageRectIndex].width() - (2 * pagePadding), View.MeasureSpec.AT_MOST)
                availableHeight = pageRects[pageRectIndex].height() - (2 * pagePadding)
            } else {
                availableHeight -= textView.measuredHeight
                textStrings.add(paragraphString)
            }
        }

        pageStrings.add(textStrings)
    }

    private fun textStringHelper(inString: String): String {
        var step1 = inString.replace("\\s+".toRegex(), " ")
        var step2 = step1.replace("_(?<italics>((?!_).)+)_".toRegex(),"<i>\${italics}</i>")

        return "\t$step2"
    }
}
