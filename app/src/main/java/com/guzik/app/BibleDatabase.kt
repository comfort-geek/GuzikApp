package com.guzik.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import java.util.zip.Inflater

data class Book(val id: Int, val key: String, val name: String, val testament: String)
data class Verse(val chapter: Int, val verse: Int, val text: String)

object BibleDatabase {
    private var db: SQLiteDatabase? = null

    fun open(context: Context): SQLiteDatabase {
        if (db != null) return db!!
        val dbFile = context.getDatabasePath("bible.db")
        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            context.assets.open("bible.db").use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        return db!!
    }

    fun getBooks(context: Context): List<Book> {
        val list = mutableListOf<Book>()
        val c = open(context).rawQuery("SELECT id, key, name, testament FROM books ORDER BY id", null)
        while (c.moveToNext()) {
            list.add(Book(c.getInt(0), c.getString(1), c.getString(2), c.getString(3)))
        }
        c.close()
        return list
    }

    fun getVerses(context: Context, bookId: Int, chapter: Int): List<Verse> {
        val list = mutableListOf<Verse>()
        val c = open(context).rawQuery(
            "SELECT chapter, verse, text FROM verses WHERE book_id=? AND chapter=? ORDER BY verse",
            arrayOf(bookId.toString(), chapter.toString())
        )
        while (c.moveToNext()) {
            list.add(Verse(c.getInt(0), c.getInt(1), c.getString(2)))
        }
        c.close()
        return list
    }

    fun getCommentary(context: Context, bookId: Int, chapter: Int): String? {
        val c = open(context).rawQuery(
            "SELECT html_compressed FROM commentary WHERE book_id=? AND chapter=?",
            arrayOf(bookId.toString(), chapter.toString())
        )
        if (!c.moveToFirst()) { c.close(); return null }
        val blob = c.getBlob(0)
        c.close()
        return try {
            val inflater = Inflater()
            inflater.setInput(blob)
            val out = ByteArray(500_000)
            val len = inflater.inflate(out)
            inflater.end()
            String(out, 0, len, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
