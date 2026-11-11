data class ChapterSummary(val summary: String, val whatHappened: String, val application: String)

fun getSummary(context: Context, bookId: Int, chapter: Int): ChapterSummary? {
    val c = open(context).rawQuery(
        "SELECT summary, what_happened, application FROM summaries WHERE book_id=? AND chapter=?",
        arrayOf(bookId.toString(), chapter.toString())
    )
    if (!c.moveToFirst()) { c.close(); return null }
    val s = ChapterSummary(
        c.getString(0) ?: "",
        c.getString(1) ?: "",
        c.getString(2) ?: ""
    )
    c.close()
    return s
}
