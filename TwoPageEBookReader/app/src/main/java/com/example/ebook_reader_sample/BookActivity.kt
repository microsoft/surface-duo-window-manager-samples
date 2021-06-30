/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */
package com.example.ebook_reader_sample

import android.content.res.Configuration
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Consumer
import androidx.viewpager2.widget.ViewPager2
import androidx.window.DisplayFeature
import androidx.window.FoldingFeature
import androidx.window.WindowLayoutInfo
import androidx.window.WindowManager
import java.util.concurrent.Executor
import kotlin.math.max
import kotlin.math.min

class BookActivity : AppCompatActivity(), ViewTreeObserver.OnGlobalLayoutListener {
    private lateinit var book: Book

    private lateinit var bookPagerView: ViewPager2
    private lateinit var windowManager: WindowManager
    private var pagePagerCallback = BookPagerCallback()
    private val handler = Handler(Looper.getMainLooper())
    private val mainThreadExecutor = Executor { r: Runnable -> handler.post(r) }
    private val layoutStateContainer = LayoutStateContainer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        renderLoading()

        windowManager = WindowManager(this)

        book = Book(intent.getStringExtra("BOOK_FILEPATH")!!, this)
        book.currentChapter = 0
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        renderLoading()
    }

    //renderLoading() resets the activity view to render an empty pager view
    //this.onGlobalLayout() is added as a post-render callback
    private fun renderLoading() {
        setContentView(R.layout.activity_book)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = intent.getStringExtra("BOOK_TITLE")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bookPagerView = findViewById(R.id.view_pager_view)
        bookPagerView.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    //onGlobalLayout() gets the layout metrics of the empty pager view, and passes them to the Book object
    //At the end, renderBook() is called
    override fun onGlobalLayout() {
        bookPagerView.viewTreeObserver.removeOnGlobalLayoutListener(this)

        val bookPagerLocation = IntArray(2)
        bookPagerView.getLocationInWindow(bookPagerLocation)
        val bookPagerRect = Rect(bookPagerLocation[0], bookPagerLocation[1], bookPagerLocation[0] + bookPagerView.width, bookPagerLocation[1] + bookPagerView.height)
        val captionHeight = resources.getDimension(R.dimen.caption_height).toInt()

        val tempPageRects = ArrayList<Rect>()
        when (layoutStateContainer.layoutMode) {
            LayoutMode.NORMAL -> {
                tempPageRects.add(Rect(bookPagerRect.left, bookPagerRect.top, bookPagerRect.right, bookPagerRect.bottom - captionHeight))
            }
            //TODO Setting book to two-page layout
            LayoutMode.SPLIT_VERTICAL -> {
                tempPageRects.add(Rect(bookPagerRect.left, bookPagerRect.top, bookPagerRect.right, layoutStateContainer.foldRect.top))
                tempPageRects.add(Rect(bookPagerRect.left, layoutStateContainer.foldRect.bottom, bookPagerRect.right, bookPagerRect.bottom - captionHeight))
            }
            LayoutMode.SPLIT_HORIZONTAL -> {
                tempPageRects.add(Rect(bookPagerRect.left, bookPagerRect.top, layoutStateContainer.foldRect.left, bookPagerRect.bottom - captionHeight))
                tempPageRects.add(Rect(layoutStateContainer.foldRect.right, bookPagerRect.top, bookPagerRect.right, bookPagerRect.bottom - captionHeight))
            }
        }

        book.pageRects = tempPageRects

        renderBook()
    }

    //renderBook() creates or replaces the pager in the pager view
    private fun renderBook() {
        if (bookPagerView.adapter != null) {
            bookPagerView.unregisterOnPageChangeCallback(pagePagerCallback)
        }

        bookPagerView.adapter = BookPagerAdapter(book, layoutStateContainer)
        bookPagerView.setPageTransformer(PageTransformer())
        val position = if (layoutStateContainer.layoutMode == LayoutMode.NORMAL) book.currentPage + 1 else (book.currentPage/2) + 1
        bookPagerView.setCurrentItem(position, false)
        bookPagerView.registerOnPageChangeCallback(pagePagerCallback)
        bookPagerView.isUserInputEnabled = true
    }

    inner class BookPagerCallback() : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            when (position) {
                0 -> {
                    if (book.currentChapter > 0) {
                        bookPagerView.isUserInputEnabled = false
                        handler.postDelayed({
                            book.currentChapter = book.currentChapter - 1
                            book.currentPage = book.numPages - 1
                            renderBook()
                        }, 250)
                    }
                }
                bookPagerView.adapter!!.itemCount - 1 -> {
                    if (book.currentChapter < book.numChapters - 1) {
                        bookPagerView.isUserInputEnabled = false
                        handler.postDelayed({
                            book.currentChapter = book.currentChapter + 1
                            book.currentPage = 0
                            renderBook()
                        }, 250)
                    }
                }
                else -> {
                    book.currentPage = if (layoutStateContainer.layoutMode == LayoutMode.NORMAL) {
                        position - 1
                    } else {
                        (2 * (position - 1))
                    }
                }
            }
        }
    }

    inner class PageTransformer : ViewPager2.PageTransformer {
        override fun transformPage(page: View, position: Float) {
            page.apply {
                when {
                    position < -1 -> {
                        translationX = 0f
                        translationZ = 0f
                        alpha = 0f
                    }
                    position > 1 -> {
                        translationX = 0f
                        translationZ = -1f
                        alpha = 0f
                    }
                    position > 0 -> {
                        translationX = width * -position
                        translationZ = -1f
                        alpha = 1f
                    }
                    else -> {
                        translationX = 0f
                        translationZ = 0f
                        alpha = 1f
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.book_menu, menu)
        val subMenu = menu?.findItem(R.id.action_chapter_group)?.subMenu
        for (chapter in 0 until book.numChapters) {
            subMenu?.add("Section $chapter")
        }
        subMenu?.add("Accreditation")
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_font_up -> {
                book.fontSize = min(24, book.fontSize + 2)
                renderBook()
            }
            R.id.action_font_down -> {
                book.fontSize = max(8, book.fontSize - 2)
                renderBook()
            }
            else -> {
                if (item.title == "Accreditation") {
                    AlertDialog.Builder(this).setTitle("Project Gutenberg")
                        .setMessage(this.resources.getString(R.string.accreditations))
                        .show()
                }
                else if (item.title != null && "Section (\\d+)".toRegex().matches(item.title)) {
                    book.currentChapter = item.title.substring(8).toInt()
                    book.currentPage = 0
                    renderBook()
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        windowManager.registerLayoutChangeCallback(mainThreadExecutor, layoutStateContainer)
    }

    override fun onStop() {
        super.onStop()
        windowManager.unregisterLayoutChangeCallback(layoutStateContainer)
    }

    enum class LayoutMode {
        NORMAL, SPLIT_VERTICAL, SPLIT_HORIZONTAL
    }

    //TODO Containers and callbacks for two-page layout information
    inner class LayoutStateContainer : Consumer<WindowLayoutInfo> {
        var layoutMode = LayoutMode.NORMAL
        var foldRect = Rect()

        override fun accept(newLayoutInfo: WindowLayoutInfo) {
            layoutMode = LayoutMode.NORMAL
            foldRect = Rect()
            for (displayFeature : DisplayFeature in newLayoutInfo.displayFeatures) {
                if (displayFeature is FoldingFeature) {
                    foldRect = displayFeature.bounds
                    layoutMode = if (displayFeature.orientation == FoldingFeature.ORIENTATION_HORIZONTAL) {
                        LayoutMode.SPLIT_VERTICAL
                    } else {
                        LayoutMode.SPLIT_HORIZONTAL
                    }
                }
            }
            renderLoading()
        }
    }

}