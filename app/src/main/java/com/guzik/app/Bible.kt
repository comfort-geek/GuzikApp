package com.guzik.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.FileOutputStream
import java.util.zip.Inflater

// ============ DATA ============
data class Book(val id: Int, val key: String, val name: String, val testament: String)
data class Verse(val chapter: Int, val verse: Int, val text: String)
data class ChapterSummary(val summary: String, val whatHappened: String, val application: String)

object BibleDatabase {
    private var db: SQLiteDatabase? = null

    fun open(context: Context): SQLiteDatabase {
        if (db != null) return db!!
        val dbFile = context.getDatabasePath("bible.db")
        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            context.assets.open("bible.db").use { input ->
                FileOutputStream(dbFile).use { output -> input.copyTo(output) }
            }
        }
        db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        return db!!
    }

    fun getBooks(context: Context): List<Book> {
        val list = mutableListOf<Book>()
        val c = open(context).rawQuery("SELECT id, key, name, testament FROM books ORDER BY id", null)
        while (c.moveToNext()) { list.add(Book(c.getInt(0), c.getString(1), c.getString(2), c.getString(3))) }
        c.close()
        return list
    }

    fun getVerses(context: Context, bookId: Int, chapter: Int): List<Verse> {
        val list = mutableListOf<Verse>()
        val c = open(context).rawQuery("SELECT chapter, verse, text FROM verses WHERE book_id=? AND chapter=? ORDER BY verse", arrayOf(bookId.toString(), chapter.toString()))
        while (c.moveToNext()) { list.add(Verse(c.getInt(0), c.getInt(1), c.getString(2))) }
        c.close()
        return list
    }

    fun getMaxChapter(context: Context, bookId: Int): Int {
        val c = open(context).rawQuery("SELECT MAX(chapter) FROM verses WHERE book_id=?", arrayOf(bookId.toString()))
        var max = 1
        if (c.moveToFirst()) max = c.getInt(0)
        c.close()
        return max
    }

    fun getCommentary(context: Context, bookId: Int, chapter: Int): String? {
        val c = open(context).rawQuery("SELECT html_compressed FROM commentary WHERE book_id=? AND chapter=?", arrayOf(bookId.toString(), chapter.toString()))
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
        } catch (e: Exception) { null }
    }

    fun getSummary(context: Context, bookId: Int, chapter: Int): ChapterSummary? {
        val c = open(context).rawQuery("SELECT summary, what_happened, application FROM summaries WHERE book_id=? AND chapter=?", arrayOf(bookId.toString(), chapter.toString()))
        if (!c.moveToFirst()) { c.close(); return null }
        val s = ChapterSummary(c.getString(0) ?: "", c.getString(1) ?: "", c.getString(2) ?: "")
        c.close()
        return s
    }
}

// ============ SCREEN ============
@Composable
fun BibleScreen() {
    val context = LocalContext.current
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var selectedChapter by remember { mutableStateOf(1) }
    var verses by remember { mutableStateOf<List<Verse>>(emptyList()) }
    var tab by remember { mutableStateOf(0) }
    var commentaryHtml by remember { mutableStateOf<String?>(null) }
    var summary by remember { mutableStateOf<ChapterSummary?>(null) }
    var maxChapter by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) {
        books = BibleDatabase.getBooks(context)
        if (books.isNotEmpty()) selectedBook = books[0]
    }
    LaunchedEffect(selectedBook) {
        selectedBook?.let {
            maxChapter = BibleDatabase.getMaxChapter(context, it.id)
            selectedChapter = 1
        }
    }
    LaunchedEffect(selectedBook, selectedChapter) {
        selectedBook?.let {
            verses = BibleDatabase.getVerses(context, it.id, selectedChapter)
            summary = BibleDatabase.getSummary(context, it.id, selectedChapter)
            commentaryHtml = null
            if (tab == 2) commentaryHtml = BibleDatabase.getCommentary(context, it.id, selectedChapter)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Bible", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Offline - 66 books, 31,098 verses", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        if (selectedBook == null) {
            LazyColumn {
                items(books) { b ->
                    Column(Modifier.fillMaxWidth().clickable { selectedBook = b }.padding(12.dp)) {
                        Text(b.name, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Text(if (b.testament == "Old") "Old Testament" else "New Testament", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { selectedBook = null }) { Text("← Books") }
                Text("${selectedBook!!.name} $selectedChapter", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row {
                    Button(onClick = { if (selectedChapter > 1) selectedChapter-- }, enabled = selectedChapter > 1) { Text("◀") }
                    Spacer(Modifier.width(4.dp))
                    Button(onClick = { if (selectedChapter < maxChapter) selectedChapter++ }, enabled = selectedChapter < maxChapter) { Text("▶") }
                }
            }
            Spacer(Modifier.height(8.dp))

            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }) { Text("Verses", Modifier.padding(12.dp)) }
                Tab(selected = tab == 1, onClick = { tab = 1; summary = BibleDatabase.getSummary(context, selectedBook!!.id, selectedChapter) }) { Text("Summary", Modifier.padding(12.dp)) }
                Tab(selected = tab == 2, onClick = { tab = 2; commentaryHtml = BibleDatabase.getCommentary(context, selectedBook!!.id, selectedChapter) }) { Text("Commentary", Modifier.padding(12.dp)) }
            }

            if (tab == 0) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(verses) { v ->
                        Row(Modifier.padding(vertical = 6.dp)) {
                            Text("${v.verse} ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(v.text, fontSize = 17.sp, lineHeight = 24.sp)
                        }
                    }
                }
            } else if (tab == 1) {
                if (summary == null) Text("No summary for this chapter.", Modifier.padding(16.dp))
                else LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Text("Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text(summary!!.summary, fontSize = 16.sp, lineHeight = 24.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("What Happened", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text(summary!!.whatHappened, fontSize = 16.sp, lineHeight = 24.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Application", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text(summary!!.application, fontSize = 16.sp, lineHeight = 24.sp)
                    }
                }
            } else {
                if (commentaryHtml == null) Text("No commentary for this chapter.", Modifier.padding(16.dp))
                else AndroidView(factory = { ctx -> WebView(ctx).apply { loadDataWithBaseURL(null, commentaryHtml!!, "text/html", "UTF-8", null) } }, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
