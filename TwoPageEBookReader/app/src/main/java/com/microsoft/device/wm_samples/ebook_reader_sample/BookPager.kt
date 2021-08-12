/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */
package com.microsoft.device.wm_samples.ebook_reader_sample

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BookPagerAdapter(inBook: Book, inLayoutStateContainer: BookActivity) : RecyclerView.Adapter<PageViewHolder>() {
    private val book = inBook
    private val layoutStateContainer = inLayoutStateContainer

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        return when(layoutStateContainer.layoutMode) {
            BookActivity.LayoutMode.NORMAL -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.book_page_layout_normal, parent, false)
                view.findViewById<TextView>(R.id.caption_view).text = book.chapterTitle
                PageViewHolder(view, book.fontSize)
            }
            //TODO Setting view holder to two-page layout
            BookActivity.LayoutMode.SPLIT_HORIZONTAL -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.book_page_layout_split_horizontal, parent, false)
                view.findViewWithTag<View>("page_container_0").layoutParams = LinearLayout.LayoutParams(book.pageRects[0].width(),
                    LinearLayout.LayoutParams.MATCH_PARENT)
                view.findViewWithTag<View>("page_container_1").layoutParams = LinearLayout.LayoutParams(book.pageRects[1].width(),
                    LinearLayout.LayoutParams.MATCH_PARENT)

                view.findViewById<TextView>(R.id.caption_view).text = book.chapterTitle
                view.findViewById<TextView>(R.id.caption_view2).text = book.chapterTitle
                SplitPageViewHolder(view, book.fontSize)
            }
            BookActivity.LayoutMode.SPLIT_VERTICAL -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.book_page_layout_split_vertical, parent, false)
                view.findViewWithTag<View>("page_container_0").layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, book.pageRects[0].height())
                view.findViewWithTag<View>("page_container_1").layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, book.pageRects[1].height())

                view.findViewById<TextView>(R.id.caption_view).text = book.chapterTitle
                SplitPageViewHolder(view, book.fontSize)
            }
        }
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        if (layoutStateContainer.layoutMode == BookActivity.LayoutMode.NORMAL) {
            var stringList = ArrayList<String>()
            when (position) {
                0 -> {
                    stringList.add(book.chapterStartString)
                }
                itemCount - 1 -> {
                    stringList.add(book.chapterEndString)
                }
                else -> {
                    stringList = book.getPageStrings(position - 1)
                }
            }
            holder.repopulate(stringList)
        }
        else {
            //TODO Different page population for view holders in two-page layout
            var stringList = ArrayList<String>()
            var stringList2 = ArrayList<String>()
            when (position) {
                0 -> {
                    stringList.add(book.chapterStartString)
                    stringList2.add(book.chapterStartString)
                }
                itemCount - 1 -> {
                    stringList.add(book.chapterEndString)
                    stringList2.add(book.chapterEndString)
                }
                else -> {
                    stringList = book.getPageStrings(2 * (position - 1))
                    stringList2 = book.getPageStrings((2 * (position - 1)) + 1)
                }
            }
            (holder as SplitPageViewHolder).repopulate(stringList)
            (holder as SplitPageViewHolder).repopulate2(stringList2)
        }
    }

    override fun getItemCount(): Int {
        return if (layoutStateContainer.layoutMode == BookActivity.LayoutMode.NORMAL) {
            book.numPages + 2
        } else {
            //TODO Different view holder count in two-page layout
            (book.numPages / 2) + (book.numPages % 2) + 2
        }

    }
}

open class PageViewHolder(val rootView: View, val fontSize: Int) : RecyclerView.ViewHolder(rootView) {
    private val linearLayout = rootView.findViewById<LinearLayout>(R.id.linear_layout)

    fun repopulate(pageStrings: ArrayList<String>) {
        linearLayout.removeAllViews()
        for (string in pageStrings) {
            val textView = View.inflate(rootView.context, R.layout.book_page_text_paragraph, null) as TextView
            textView.text = Html.fromHtml(string)
            textView.textSize = fontSize.toFloat()
            linearLayout.addView(textView)
        }
    }
}

//TODO Extra functionality for view holders in two-page layout
class SplitPageViewHolder(view: View, int: Int) : PageViewHolder(view, int) {
    private val linearLayout2 = view.findViewById<LinearLayout>(R.id.linear_layout2)

    fun repopulate2(pageStrings: ArrayList<String>) {
        linearLayout2.removeAllViews()
        for (string in pageStrings) {
            val textView = View.inflate(rootView.context, R.layout.book_page_text_paragraph, null) as TextView
            textView.text = Html.fromHtml(string)
            textView.textSize = fontSize.toFloat()
            linearLayout2.addView(textView)
        }
    }
}