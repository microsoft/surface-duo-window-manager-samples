/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */
package com.microsoft.device.wm_samples.ebook_reader_sample

import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoRepository
import androidx.window.layout.WindowInfoRepository.Companion.windowInfoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import kotlin.math.max
import kotlin.math.min


class BookActivity : AppCompatActivity(), ViewTreeObserver.OnGlobalLayoutListener {
    private lateinit var book: Book
    var layoutMode = LayoutMode.NORMAL
    var foldRect = Rect()

    private lateinit var bookPagerView: ViewPager2
    private lateinit var windowInfoRep: WindowInfoRepository
    private var pagePagerCallback = BookPagerCallback()
    private val handler = Handler(Looper.getMainLooper())
    private val mainThreadExecutor = Executor { r: Runnable -> handler.post(r) }
    //private val layoutStateContainer = LayoutStateContainer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        renderLoading()

        windowInfoRep = windowInfoRepository()

        // Create a new coroutine since repeatOnLifecycle is a suspend function
        lifecycleScope.launch(Dispatchers.Main) {
            // The block passed to repeatOnLifecycle is executed when the lifecycle
            // is at least STARTED and is cancelled when the lifecycle is STOPPED.
            // It automatically restarts the block when the lifecycle is STARTED again.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Safely collect from windowInfoRepo when the lifecycle is STARTED
                // and stops collection when the lifecycle is STOPPED
                windowInfoRep.windowLayoutInfo
                    .collect { newLayoutInfo ->
                        layoutMode = LayoutMode.NORMAL
                        foldRect = Rect()
                        for (displayFeature : DisplayFeature in newLayoutInfo.displayFeatures) {
                            if (displayFeature is FoldingFeature) {
                                foldRect = displayFeature.bounds
                                layoutMode = if (displayFeature.orientation == FoldingFeature.Orientation.HORIZONTAL) {
                                    LayoutMode.SPLIT_VERTICAL
                                } else {
                                    LayoutMode.SPLIT_HORIZONTAL
                                }
                            }
                        }
                    }
            }
        }


        book = Book(intent.getStringExtra("BOOK_FILEPATH")!!, this)
        book.currentChapter = 0
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        renderLoading()
    }

    override fun onResume() {
        super.onResume()

        renderLoading()
    }

    // renderLoading() resets the activity view to render an empty pager view
    // this.onGlobalLayout() is added as a post-render callback
    private fun renderLoading() {
        setContentView(R.layout.activity_book)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = intent.getStringExtra("BOOK_TITLE")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bookPagerView = findViewById(R.id.view_pager_view)
        bookPagerView.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    // onGlobalLayout() gets the layout metrics of the empty pager view, and passes them to the Book object
    // At the end, renderBook() is called
    override fun onGlobalLayout() {
        bookPagerView.viewTreeObserver.removeOnGlobalLayoutListener(this)

        val bookPagerLocation = IntArray(2)
        bookPagerView.getLocationInWindow(bookPagerLocation)
        val bookPagerRect = Rect(bookPagerLocation[0], bookPagerLocation[1], bookPagerLocation[0] + bookPagerView.width, bookPagerLocation[1] + bookPagerView.height)
        val captionHeight = resources.getDimension(R.dimen.caption_height).toInt()

        val tempPageRects = ArrayList<Rect>()
        when (layoutMode) {
            LayoutMode.NORMAL -> {
                tempPageRects.add(Rect(bookPagerRect.left, bookPagerRect.top, bookPagerRect.right, bookPagerRect.bottom - captionHeight))
            }
            // TODO Setting book to two-page layout
            LayoutMode.SPLIT_VERTICAL -> {
                tempPageRects.add(Rect(bookPagerRect.left, bookPagerRect.top, bookPagerRect.right, foldRect.top))
                tempPageRects.add(Rect(bookPagerRect.left, foldRect.bottom, bookPagerRect.right, bookPagerRect.bottom - captionHeight))
            }
            LayoutMode.SPLIT_HORIZONTAL -> {
                tempPageRects.add(Rect(bookPagerRect.left, bookPagerRect.top, foldRect.left, bookPagerRect.bottom - captionHeight))
                tempPageRects.add(Rect(foldRect.right, bookPagerRect.top, bookPagerRect.right, bookPagerRect.bottom - captionHeight))
            }
        }

        book.pageRects = tempPageRects

        renderBook()
    }

    // renderBook() creates or replaces the pager in the pager view
    private fun renderBook() {
        if (bookPagerView.adapter != null) {
            bookPagerView.unregisterOnPageChangeCallback(pagePagerCallback)
        }

        bookPagerView.adapter = BookPagerAdapter(book, this)
        bookPagerView.setPageTransformer(PageTransformer())
        val position = if (layoutMode == LayoutMode.NORMAL) book.currentPage + 1 else (book.currentPage/2) + 1
        bookPagerView.setCurrentItem(position, false)
        bookPagerView.registerOnPageChangeCallback(pagePagerCallback)
        bookPagerView.isUserInputEnabled = true
    }

    inner class BookPagerCallback : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            when (position) {
                0 -> jumpToPreviousChapter()
                bookPagerView.adapter!!.itemCount - 1 -> jumpToNextChapter()
                else -> {
                    book.currentPage = if (layoutMode == LayoutMode.NORMAL) {
                        position - 1
                    } else {
                        (2 * (position - 1))
                    }
                }
            }
        }
    }

    private fun jumpToPreviousChapter() {
        if (book.currentChapter > 0) {
            bookPagerView.isUserInputEnabled = false
            handler.postDelayed(
                {
                    book.currentChapter = book.currentChapter - 1
                    book.currentPage = book.numPages - 1
                    renderBook()
                },
                250
            )
        }
    }

    private fun jumpToNextChapter() {
        if (book.currentChapter < book.numChapters - 1) {
            bookPagerView.isUserInputEnabled = false
            handler.postDelayed(
                {
                    book.currentChapter = book.currentChapter + 1
                    book.currentPage = 0
                    renderBook()
                },
                250
            )
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
        subMenu?.add(getString(R.string.attribution_label))
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
                if (item.title == getString(R.string.attribution_label)) {
                    AlertDialog.Builder(this).setTitle("Project Gutenberg")
                        .setMessage(this.resources.getString(R.string.attribution_string))
                        .show()
                } else if (item.title != null && item.title.contains("Section")) {
                    book.currentChapter = item.title.substring(8).toInt()
                    book.currentPage = 0
                    renderBook()
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

//    override fun onStart() {
//        super.onStart()
//        windowInfoRep.registerLayoutChangeCallback(mainThreadExecutor, layoutStateContainer)
//    }
//
//    override fun onStop() {
//        super.onStop()
//        windowInfoRep.unregisterLayoutChangeCallback(layoutStateContainer)
//    }

    enum class LayoutMode {
        NORMAL, SPLIT_VERTICAL, SPLIT_HORIZONTAL
    }

//    inner class LayoutStateContainer : Consumer<WindowLayoutInfo> {
//        var layoutMode = LayoutMode.NORMAL
//        var foldRect = Rect()
//
//        override fun accept(newLayoutInfo: WindowLayoutInfo) {
//            layoutMode = LayoutMode.NORMAL
//            foldRect = Rect()
//            for (displayFeature : DisplayFeature in newLayoutInfo.displayFeatures) {
//                if (displayFeature is FoldingFeature) {
//                    foldRect = displayFeature.bounds
//                    layoutMode = if (displayFeature.orientation == FoldingFeature.Orientation.HORIZONTAL) {
//                        LayoutMode.SPLIT_VERTICAL
//                    } else {
//                        LayoutMode.SPLIT_HORIZONTAL
//                    }
//                }
//            }
//            renderLoading()
//        }
//    }

}
