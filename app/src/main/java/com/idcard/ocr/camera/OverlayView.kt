package com.idcard.ocr.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.idcard.ocr.ui.theme.AppColors

/**
 * Custom overlay view for document frame guide
 * Displays a rectangular cutout for ID card alignment
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Document frame dimensions (CR80 ratio)
    private var frameRatio = 1.586f // 85.60 × 53.98 mm
    private var frameStrokeWidth = 4f
    private var cornerRadius = 24f

    private val backgroundPaint = Paint().apply {
        color = AppColors.OverlayBackground
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val framePaint = Paint().apply {
        color = AppColors.DocumentFrame
        style = Paint.Style.STROKE
        strokeWidth = frameStrokeWidth
        isAntiAlias = true
    }

    private var documentFrame: RectF? = null

    /**
     * Set document frame aspect ratio
     */
    fun setFrameRatio(ratio: Float) {
        frameRatio = ratio
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val frame = calculateDocumentFrame() ?: return
        documentFrame = frame

        // Draw semi-transparent background
        canvas.drawColor(Color.TRANSPARENT)
        drawBackgroundWithCutout(canvas, frame)

        // Draw document frame
        drawRoundedFrame(canvas, frame)
    }

    private fun calculateDocumentFrame(): RectF? {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        if (viewWidth <= 0 || viewHeight <= 0) return null

        // Calculate frame dimensions maintaining aspect ratio
        val maxWidth = viewWidth * 0.9f
        val maxHeight = viewHeight * 0.8f

        val frameWidth: Float
        val frameHeight: Float
        val frameX: Float
        val frameY: Float

        if (maxWidth / frameRatio <= maxHeight) {
            // Width is the limiting factor
            frameWidth = maxWidth
            frameHeight = frameWidth / frameRatio
        } else {
            // Height is the limiting factor
            frameHeight = maxHeight
            frameWidth = frameHeight * frameRatio
        }

        // Center the frame
        frameX = (viewWidth - frameWidth) / 2
        frameY = (viewHeight - frameHeight) / 2

        return RectF(frameX, frameY, frameX + frameWidth, frameY + frameHeight)
    }

    private fun drawBackgroundWithCutout(canvas: Canvas, frame: RectF) {
        val path = Path()

        // Add outer rectangle (full screen)
        path.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)

        // Add inner rectangle (document frame) - creates cutout effect
        path.addRect(
            frame.left - frameStrokeWidth,
            frame.top - frameStrokeWidth,
            frame.right + frameStrokeWidth,
            frame.bottom + frameStrokeWidth,
            Path.Direction.CCW
        )

        canvas.drawPath(path, backgroundPaint)
    }

    private fun drawRoundedFrame(canvas: Canvas, frame: RectF) {
        // Draw rounded rectangle with corners
        val cornerRadiusPx = cornerRadius * resources.displayMetrics.density

        // Top-left corner
        drawCorner(canvas, frame.left, frame.top, cornerRadiusPx, true, true)

        // Top-right corner
        drawCorner(canvas, frame.right, frame.top, cornerRadiusPx, false, true)

        // Bottom-left corner
        drawCorner(canvas, frame.left, frame.bottom, cornerRadiusPx, true, false)

        // Bottom-right corner
        drawCorner(canvas, frame.right, frame.bottom, cornerRadiusPx, false, false)

        // Draw frame edges
        canvas.drawRect(frame.left, frame.top + cornerRadiusPx, frame.right, frame.bottom - cornerRadiusPx, framePaint)
        canvas.drawRect(frame.left + cornerRadiusPx, frame.top, frame.right - cornerRadiusPx, frame.bottom, framePaint)
    }

    private fun drawCorner(
        canvas: Canvas,
        x: Float,
        y: Float,
        radius: Float,
        isLeft: Boolean,
        isTop: Boolean
    ) {
        val startAngle = when {
            isLeft && isTop -> 180f
            !isLeft && isTop -> 270f
            isLeft && !isTop -> 90f
            else -> 0f
        }

        val sweepAngle = 90f

        val rect = RectF(
            if (isLeft) x - radius else x,
            if (isTop) y - radius else y,
            if (isLeft) x else x + radius,
            if (isTop) y else y + radius
        )

        canvas.drawArc(rect, startAngle, sweepAngle, false, framePaint)
    }

    /**
     * Check if point is within document frame
     */
    fun isPointInFrame(x: Float, y: Float): Boolean {
        val frame = documentFrame ?: return false
        return frame.contains(x, y)
    }

    /**
     * Get document frame for reference
     */
    fun getDocumentFrame(): RectF? = documentFrame

    /**
     * Set frame corner radius
     */
    fun setCornerRadiusDp(radiusDp: Float) {
        cornerRadius = radiusDp
        invalidate()
    }
}
