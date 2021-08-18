/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */
package com.microsoft.device.wm_samples.ebook_reader_sample

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

const val book_dir = "books/"

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        setSupportActionBar(findViewById(R.id.toolbar))

        val titleRecyclerView = findViewById<RecyclerView>(R.id.title_recycler)
        titleRecyclerView.layoutManager = LinearLayoutManager(this)
        titleRecyclerView.adapter = TitleRecyclerAdapter(assets.list(book_dir)!!)
    }

    private inner class TitleRecyclerAdapter(inBookList: Array<String>) : RecyclerView.Adapter<TitleViewHolder>() {
        private var bookList = inBookList

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TitleViewHolder {
            val pageView = LayoutInflater.from(parent.context).inflate(R.layout.start_title_layout, parent, false)
            return TitleViewHolder(pageView)
        }

        override fun onBindViewHolder(holder: TitleViewHolder, position: Int) {
            holder.titleText.text = bookList[position].replace(".txt", "")
        }

        override fun getItemCount(): Int {
            return bookList.size
        }
    }

    private inner class TitleViewHolder(parent: View) : RecyclerView.ViewHolder(parent), View.OnClickListener {
        val titleText: TextView = parent.findViewById(R.id.book_title)
        init {
            titleText.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val intent = Intent(applicationContext, BookActivity::class.java)
            intent.putExtra("BOOK_FILEPATH", "books/${titleText.text}.txt")
            intent.putExtra("BOOK_TITLE", "${titleText.text}")
            startActivity(intent)
        }
    }
}
