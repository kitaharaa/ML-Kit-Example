package com.google.mlkit.vision.demo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

data class OcrWord(val text: String, val rect: Rect)

class TextOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val boxes = mutableListOf<OcrWord>()
    private val selected = mutableSetOf<OcrWord>()

    private val paintBox = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val paintHighlight = Paint().apply {
        color = Color.argb(120, 0, 150, 255)
        style = Paint.Style.FILL
    }

    private val paintTextBg = Paint().apply {
        color = Color.argb(180, 0, 0, 0) // semi-transparent black
        style = Paint.Style.FILL
    }

    private val paintText = TextPaint().apply {
        color = Color.WHITE
        textSize = 42f
        isAntiAlias = true
    }

    fun setWords(words: List<OcrWord>) {
        boxes.clear()
        boxes.addAll(words)
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // draw OCR bounding boxes
        for (word in boxes) {
            val rect = word.rect
            if (selected.contains(word)) {
                canvas.drawRect(rect, paintHighlight)
            }
            canvas.drawRect(rect, paintBox)
        }

        // draw selected text banner
        val selectedText = getSelectedText()
        if (selectedText.isNotEmpty()) {
            val padding = 24
            val maxWidth = width - padding * 2

            val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder
                    .obtain(selectedText, 0, selectedText.length, paintText, maxWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(true)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(
                    selectedText,
                    paintText,
                    maxWidth,
                    Layout.Alignment.ALIGN_NORMAL,
                    1.0f,
                    0.0f,
                    true
                )
            }

            val textHeight = staticLayout.height.toFloat()

            val bgRect = RectF(
                padding.toFloat(),
                padding.toFloat(),
                (padding + staticLayout.width).toFloat(),
                (padding + textHeight).toFloat()
            )
            canvas.drawRoundRect(bgRect, 16f, 16f, paintTextBg)

            canvas.save()
            canvas.translate(padding.toFloat(), padding.toFloat())
            staticLayout.draw(canvas)
            canvas.restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x.toInt()
            val y = event.y.toInt()
            val hit = boxes.firstOrNull { it.rect.contains(x, y) }
            hit?.let {
                if (selected.contains(it)) selected.remove(it) else selected.add(it)
                invalidate()
            }
        }
        return true
    }

    fun getSelectedText(): String = selected.joinToString(" ") { it.text }
}
