package org.parallel_sekai.kanade.data.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.data.model.LyricLine
import org.parallel_sekai.kanade.data.model.MusicModel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object LyricImageUtils {

    suspend fun generateLyricImage(
        context: Context,
        song: MusicModel,
        allLyrics: List<LyricLine>,
        selectedIndices: Set<Int>,
        gradientColors: List<Color>,
        quality: Float = 1.0f,
        alignment: Int = 0, // 0 = Left, 1 = Center, 2 = Right
        artistJoinString: String = ", "
    ): Bitmap? = withContext(Dispatchers.IO) {
        val width = (1080 * quality).toInt()
        val padding = 80f * quality
        val lineSpacing = 40f * quality
        val fontSizeTitle = 64f * quality
        val fontSizeArtist = 44f * quality
        val fontSizeLyric = 52f * quality
        val fontSizeTranslation = 38f * quality
        val fontSizeBranding = 36f * quality

        val selectedLines = allLyrics.filterIndexed { index, _ -> selectedIndices.contains(index) }
        if (selectedLines.isEmpty()) return@withContext null

        val textAlignment = when (alignment) {
            1 -> Layout.Alignment.ALIGN_CENTER
            2 -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }

        // 1. Measure Metadata Height - Use separate paints to avoid style leakage
        val paintTitle = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.toArgb()
            textSize = fontSizeTitle
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        val paintArtist = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.toArgb()
            textSize = fontSizeArtist
            typeface = Typeface.DEFAULT
            alpha = (255 * 0.7).toInt()
        }
        
        val paintAlbum = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.toArgb()
            textSize = fontSizeArtist * 0.8f
            typeface = Typeface.DEFAULT
            alpha = (255 * 0.5).toInt()
        }
        
        val coverSize = 220f * quality
        val textMaxWidth = width - padding * 3 - coverSize
        val titleLayout = createStaticLayout(song.title, paintTitle, textMaxWidth.toInt(), Layout.Alignment.ALIGN_NORMAL)
        
        val artistNames = song.artists.joinToString(artistJoinString)
        val artistLayout = createStaticLayout(artistNames, paintArtist, textMaxWidth.toInt(), Layout.Alignment.ALIGN_NORMAL)
        
        val albumLayout = createStaticLayout(song.album, paintAlbum, textMaxWidth.toInt(), Layout.Alignment.ALIGN_NORMAL)
        
        val totalTextH = titleLayout.height + 20f * quality + artistLayout.height + 15f * quality + albumLayout.height
        val metadataHeight = maxOf(coverSize, totalTextH)

        // 2. Measure Lyrics Height
        val paintLyric = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSizeLyric
            color = Color.White.toArgb()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintTranslation = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSizeTranslation
            color = Color.White.toArgb()
            alpha = (255 * 0.7).toInt()
        }

        val lyricWidth = (width - padding * 2).toInt()
        val lyricLayouts = selectedLines.map { line ->
            val main = createStaticLayout(line.content, paintLyric, lyricWidth, textAlignment)
            val trans = if (!line.translation.isNullOrBlank()) {
                createStaticLayout(line.translation, paintTranslation, lyricWidth, textAlignment)
            } else null
            main to trans
        }

        var lyricsTotalHeight = 0f
        lyricLayouts.forEach { (main, trans) ->
            lyricsTotalHeight += main.height
            if (trans != null) {
                lyricsTotalHeight += 10f * quality + trans.height
            }
            lyricsTotalHeight += lineSpacing
        }

        val footerHeight = 150f * quality
        var totalHeight = padding + metadataHeight + 60f * quality + lyricsTotalHeight + footerHeight + padding
        
        // Safety Limit: Prevent OOM by capping height (e.g. 10000px)
        val maxHeight = 10000f
        if (totalHeight > maxHeight) {
            totalHeight = maxHeight
        }

        val bitmap = Bitmap.createBitmap(width, totalHeight.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 3. Draw Background
        val paintBg = Paint(Paint.ANTI_ALIAS_FLAG)
        if (gradientColors.size >= 2) {
            val gradient = LinearGradient(
                0f, 0f, 0f, totalHeight,
                gradientColors[0].toArgb(), gradientColors[1].toArgb(),
                Shader.TileMode.CLAMP
            )
            paintBg.shader = gradient
        } else {
            paintBg.color = android.graphics.Color.BLACK
        }
        canvas.drawRect(0f, 0f, width.toFloat(), totalHeight, paintBg)
        canvas.drawColor(android.graphics.Color.argb(100, 0, 0, 0))

        // 4. Draw Metadata
        var currentY = padding + (metadataHeight - totalTextH) / 2f
        if (currentY < padding) currentY = padding

        canvas.save()
        canvas.translate(padding, currentY)
        titleLayout.draw(canvas)
        canvas.restore()
        
        currentY += titleLayout.height + 20f * quality
        canvas.save()
        canvas.translate(padding, currentY)
        artistLayout.draw(canvas)
        canvas.restore()
        
        currentY += artistLayout.height + 15f * quality
        canvas.save()
        canvas.translate(padding, currentY)
        albumLayout.draw(canvas)
        canvas.restore()

        // Draw Cover
        val coverBitmap = fetchBitmap(context, song.coverUrl)
        if (coverBitmap != null) {
            val rect = RectF(width - padding - coverSize, padding, width - padding, padding + coverSize)
            val path = Path().apply { addRoundRect(rect, 24f * quality, 24f * quality, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(path)
            canvas.drawBitmap(coverBitmap, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        }

        // 5. Draw Divider
        val paintDivider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.toArgb()
            alpha = (255 * 0.2).toInt()
            strokeWidth = 2f * quality
        }
        val dividerY = padding + metadataHeight + 30f * quality
        canvas.drawLine(padding, dividerY, width - padding, dividerY, paintDivider)

        // 6. Draw Lyrics
        currentY = dividerY + 60f * quality
        lyricLayouts.forEach { (main, trans) ->
            if (currentY + main.height > totalHeight - footerHeight) return@forEach // Stop if exceeds max height

            canvas.save()
            canvas.translate(padding, currentY)
            main.draw(canvas)
            canvas.restore()
            currentY += main.height
            
            if (trans != null) {
                if (currentY + 10f * quality + trans.height > totalHeight - footerHeight) return@forEach
                currentY += 10f * quality
                canvas.save()
                canvas.translate(padding, currentY)
                trans.draw(canvas)
                canvas.restore()
                currentY += trans.height
            }
            currentY += lineSpacing
        }

        // 7. Draw Footer Divider and Branding
        val footerDividerY = minOf(currentY - lineSpacing / 2, totalHeight - footerHeight + 50f)
        canvas.drawLine(padding, footerDividerY, width - padding, footerDividerY, paintDivider)
        
        val paintBranding = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSizeBranding
            color = Color.White.toArgb()
            alpha = (255 * 0.6).toInt()
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(context.getString(R.string.app_branding), width - padding, totalHeight - padding, paintBranding)

        bitmap
    }

    private fun createStaticLayout(text: String, paint: TextPaint, width: Int, alignment: Layout.Alignment): StaticLayout {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(alignment)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, paint, width, alignment, 1f, 0f, false)
        }
    }

    private suspend fun fetchBitmap(context: Context, url: String): Bitmap? {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        return (result as? SuccessResult)?.drawable?.let { (it as android.graphics.drawable.BitmapDrawable).bitmap }
    }

    suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        val filename = "Kanade_Lyrics_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null
        var imageUri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Kanade")
                }
                imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                val imagesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                val kanadeDir = File(imagesDir, "Kanade")
                if (!kanadeDir.exists()) kanadeDir.mkdirs()
                val image = File(kanadeDir, filename)
                imageUri = Uri.fromFile(image)
                fos = FileOutputStream(image)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
        imageUri
    }

    fun shareBitmap(context: Context, bitmap: Bitmap) {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val fileName = "shared_lyrics_${System.currentTimeMillis()}.png"
        val imageFile = File(cachePath, fileName)
        val stream = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)

        if (contentUri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            val chooser = Intent.createChooser(shareIntent, "Share Lyrics")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }
}