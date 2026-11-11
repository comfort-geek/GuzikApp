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
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.zip.Inflater

// ============ DATA ============
data class Book(
    val id: Int,
    val key: String,
    val name: String,
    val testament: String
)

data class Verse(
    val chapter: Int,
    val verse: Int,
    val text: String
)

data class ChapterSummary(
    val summary: String,
    val whatHappened: String,
    val application: String
)

object BibleDatabase {

    private var db: SQLiteDatabase? = null

    fun open(context: Context): SQLiteDatabase {
        if (db != null) {
            return db!!
        }

        val dbFile = context.getDatabasePath("bible.db")

        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()

            context.assets.open("bible.db").use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        db = SQLiteDatabase.openDatabase(
            dbFile.path,
            null,
            SQLiteDatabase.OPEN_READONLY
        )

        return db!!
    }

    fun getBooks(context: Context): List<Book> {
        val books = mutableListOf<Book>()

        val cursor = open(context).rawQuery(
            "SELECT id, key, name, testament FROM books ORDER BY id",
            null
        )

        while (cursor.moveToNext()) {
            books.add(
                Book(
                    id = cursor.getInt(0),
                    key = cursor.getString(1),
                    name = cursor.getString(2),
                    testament = cursor.getString(3)
                )
            )
        }

        cursor.close()
        return books
    }

    fun getVerses(
        context: Context,
        bookId: Int,
        chapter: Int
    ): List<Verse> {
        val verses = mutableListOf<Verse>()

        val cursor = open(context).rawQuery(
            """
            SELECT chapter, verse, text
            FROM verses
            WHERE book_id = ? AND chapter = ?
            ORDER BY verse
            """.trimIndent(),
            arrayOf(
                bookId.toString(),
                chapter.toString()
            )
        )

        while (cursor.moveToNext()) {
            verses.add(
                Verse(
                    chapter = cursor.getInt(0),
                    verse = cursor.getInt(1),
                    text = cursor.getString(2)
                )
            )
        }

        cursor.close()
        return verses
    }

    fun getMaxChapter(context: Context, bookId: Int): Int {
        val cursor = open(context).rawQuery(
            "SELECT MAX(chapter) FROM verses WHERE book_id = ?",
            arrayOf(bookId.toString())
        )

        var maxChapter = 1

        if (cursor.moveToFirst()) {
            maxChapter = cursor.getInt(0)
        }

        cursor.close()
        return maxChapter
    }

    fun getCommentary(
        context: Context,
        bookId: Int,
        chapter: Int
    ): String? {
        val cursor = open(context).rawQuery(
            """
            SELECT html_compressed
            FROM commentary
            WHERE book_id = ? AND chapter = ?
            """.trimIndent(),
            arrayOf(
                bookId.toString(),
                chapter.toString()
            )
        )

        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }

        val compressedData = cursor.getBlob(0)
        cursor.close()

        return try {
            val inflater = Inflater()
            inflater.setInput(compressedData)

            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)

            while (!inflater.finished()) {
                val bytesRead = inflater.inflate(buffer)

                if (bytesRead > 0) {
                    output.write(buffer, 0, bytesRead)
                }

                if (bytesRead == 0 && inflater.needsInput()) {
                    break
                }
            }

            inflater.end()

            output.toString("UTF-8")
        } catch (e: Exception) {
            null
        }
    }

    fun getSummary(
        context: Context,
        bookId: Int,
        chapter: Int
    ): ChapterSummary? {
        val cursor = open(context).rawQuery(
            """
            SELECT summary, what_happened, application
            FROM summaries
            WHERE book_id = ? AND chapter = ?
            """.trimIndent(),
            arrayOf(
                bookId.toString(),
                chapter.toString()
            )
        )

        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }

        val summary = ChapterSummary(
            summary = cursor.getString(0) ?: "",
            whatHappened = cursor.getString(1) ?: "",
            application = cursor.getString(2) ?: ""
        )

        cursor.close()
        return summary
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
    var selectedTab by remember { mutableStateOf(0) }
    var commentaryHtml by remember { mutableStateOf<String?>(null) }
    var summary by remember { mutableStateOf<ChapterSummary?>(null) }
    var maxChapter by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) {
        books = BibleDatabase.getBooks(context)

        if (books.isNotEmpty()) {
            selectedBook = books[0]
        }
    }

    LaunchedEffect(selectedBook) {
        selectedBook?.let { book ->
            maxChapter = BibleDatabase.getMaxChapter(context, book.id)
            selectedChapter = 1
        }
    }

    LaunchedEffect(selectedBook, selectedChapter) {
        selectedBook?.let { book ->
            verses = BibleDatabase.getVerses(
                context = context,
                bookId = book.id,
                chapter = selectedChapter
            )

            summary = BibleDatabase.getSummary(
                context = context,
                bookId = book.id,
                chapter = selectedChapter
            )

            commentaryHtml = null

            if (selectedTab == 2) {
                commentaryHtml = BibleDatabase.getCommentary(
                    context = context,
                    bookId = book.id,
                    chapter = selectedChapter
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Bible",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Offline - 66 books, 31,098 verses",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedBook == null) {
            LazyColumn {
                items(books) { book ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedBook = book
                            }
                            .padding(12.dp)
                    ) {
                        Text(
                            text = book.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = if (book.testament == "Old") {
                                "Old Testament"
                            } else {
                                "New Testament"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider()
                }
            }
        } else {
            val book = selectedBook!!

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        selectedBook = null
                    }
                ) {
                    Text(text = "← Books")
                }

                Text(
                    text = "${book.name} $selectedChapter",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    Button(
                        onClick = {
                            if (selectedChapter > 1) {
                                selectedChapter--
                            }
                        },
                        enabled = selectedChapter > 1
                    ) {
                        Text(text = "◀")
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = {
                            if (selectedChapter < maxChapter) {
                                selectedChapter++
                            }
                        },
                        enabled = selectedChapter < maxChapter
                    ) {
                        Text(text = "▶")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                    }
                ) {
                    Text(
                        text = "Verses",
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1

                        summary = BibleDatabase.getSummary(
                            context = context,
                            bookId = book.id,
                            chapter = selectedChapter
                        )
                    }
                ) {
                    Text(
                        text = "Summary",
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2

                        commentaryHtml = BibleDatabase.getCommentary(
                            context = context,
                            bookId = book.id,
                            chapter = selectedChapter
                        )
                    }
                ) {
                    Text(
                        text = "Commentary",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(verses) { verse ->
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = "${verse.verse} ",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = verse.text,
                                fontSize = 17.sp,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            } else if (selectedTab == 1) {
                if (summary == null) {
                    Text(
                        text = "No summary for this chapter.",
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                text = "Summary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = summary!!.summary,
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "What Happened",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = summary!!.whatHappened,
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Application",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = summary!!.application,
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            } else {
                if (commentaryHtml == null) {
                    Text(
                        text = "No commentary for this chapter.",
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                loadDataWithBaseURL(
                                    null,
                                    commentaryHtml!!,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
