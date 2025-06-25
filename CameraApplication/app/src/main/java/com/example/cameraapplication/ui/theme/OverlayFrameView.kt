package com.example.cameraapplication.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.cameraapplication.R

class OverlayFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Frame drawing properties
    private val framePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = ContextCompat.getColor(context, R.color.yellow_green)
        pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
    }

    // Face/object detection drawing properties
    private val detectionPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = ContextCompat.getColor(context, R.color.red)
    }

    // Alignment success properties
    private val successPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = ContextCompat.getColor(context, R.color.green)
    }

    // Text properties for guidance
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private var frameRect: RectF = RectF()
    private var detectionRect: RectF? = null
    private var isAlignedState: Boolean = false
    private var cornerRadius: Float = 16f
    private var alignmentThreshold: Float = 0.8f
    private var sizeThreshold: Float = 0.7f
    private var showGuidanceText: Boolean = true

    // Customizable parameters
    var framePadding: Float = 50f
        set(value) {
            field = value
            calculateFrameRect()
            invalidate()
        }

    var isAligned: Boolean
        get() = isAlignedState
        set(value) {
            isAlignedState = value
            framePaint.color = ContextCompat.getColor(
                context,
                if (value) R.color.green else R.color.yellow_green
            )
            invalidate()
        }

    var showDetectionRect: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    init {
        // Handle attributes from XML if needed
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.OverlayFrameView,
            0, 0
        ).apply {
            try {
                framePadding = getDimension(
                    R.styleable.OverlayFrameView_framePadding,
                    framePadding
                )
                cornerRadius = getDimension(
                    R.styleable.OverlayFrameView_cornerRadius,
                    cornerRadius
                )
                alignmentThreshold = getFloat(
                    R.styleable.OverlayFrameView_alignmentThreshold,
                    alignmentThreshold
                )
                sizeThreshold = getFloat(
                    R.styleable.OverlayFrameView_sizeThreshold,
                    sizeThreshold
                )
                showGuidanceText = getBoolean(
                    R.styleable.OverlayFrameView_showGuidanceText,
                    showGuidanceText
                )
            } finally {
                recycle()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateFrameRect()
        textPaint.textSize = width * 0.04f // Responsive text size
    }

    private fun calculateFrameRect() {
        frameRect = RectF(
            framePadding,
            framePadding,
            width - framePadding,
            height - framePadding
        )
    }

    fun updateDetectionRect(rect: RectF?) {
        detectionRect = rect?.let {
            // Convert normalized coordinates (0-1) to view coordinates
            RectF(
                frameRect.left + it.left * frameRect.width(),
                frameRect.top + it.top * frameRect.height(),
                frameRect.left + it.right * frameRect.width(),
                frameRect.top + it.bottom * frameRect.height()
            )
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the alignment frame (rounded rectangle)
        drawFrame(canvas)

        // Draw detection rectangle if available and enabled
        if (showDetectionRect) {
            detectionRect?.let { rect ->
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, detectionPaint)

                // Draw center crosshair for better alignment
                drawCenterCrosshair(canvas, rect)
            }
        }

        // Draw success indicator if aligned
        if (isAlignedState) {
            drawSuccessIndicator(canvas)
        }

        // Draw guidance text
        if (showGuidanceText) {
            drawGuidanceText(canvas)
        }
    }

    private fun drawFrame(canvas: Canvas) {
        // Draw outer frame
        canvas.drawRoundRect(frameRect, cornerRadius, cornerRadius, framePaint)

        // Draw corner indicators
        val cornerSize = width * 0.08f // Responsive corner size
        val cornerStroke = 8f

        // Top-left corner
        canvas.drawLine(
            frameRect.left, frameRect.top + cornerSize,
            frameRect.left, frameRect.top,
            framePaint
        )
        canvas.drawLine(
            frameRect.left, frameRect.top,
            frameRect.left + cornerSize, frameRect.top,
            framePaint
        )

        // Top-right corner
        canvas.drawLine(
            frameRect.right - cornerSize, frameRect.top,
            frameRect.right, frameRect.top,
            framePaint
        )
        canvas.drawLine(
            frameRect.right, frameRect.top,
            frameRect.right, frameRect.top + cornerSize,
            framePaint
        )

        // Bottom-right corner
        canvas.drawLine(
            frameRect.right, frameRect.bottom - cornerSize,
            frameRect.right, frameRect.bottom,
            framePaint
        )
        canvas.drawLine(
            frameRect.right, frameRect.bottom,
            frameRect.right - cornerSize, frameRect.bottom,
            framePaint
        )

        // Bottom-left corner
        canvas.drawLine(
            frameRect.left + cornerSize, frameRect.bottom,
            frameRect.left, frameRect.bottom,
            framePaint
        )
        canvas.drawLine(
            frameRect.left, frameRect.bottom,
            frameRect.left, frameRect.bottom - cornerSize,
            framePaint
        )
    }

    private fun drawSuccessIndicator(canvas: Canvas) {
        // Draw pulsating effect
        val alpha = ((System.currentTimeMillis() / 10) % 255).toInt()
        successPaint.alpha = alpha

        // Draw animated border
        canvas.drawRoundRect(
            frameRect.left - 10,
            frameRect.top - 10,
            frameRect.right + 10,
            frameRect.bottom + 10,
            cornerRadius + 10,
            cornerRadius + 10,
            successPaint
        )

        // Request redraw for animation
        if (isAlignedState) {
            postInvalidateDelayed(16) // ~60fps
        }
    }

    private fun drawCenterCrosshair(canvas: Canvas, rect: RectF) {
        val centerPaint = Paint().apply {
            color = Color.YELLOW
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        val centerX = rect.centerX()
        val centerY = rect.centerY()
        val crossSize = rect.width() * 0.1f

        // Horizontal line
        canvas.drawLine(
            centerX - crossSize, centerY,
            centerX + crossSize, centerY,
            centerPaint
        )

        // Vertical line
        canvas.drawLine(
            centerX, centerY - crossSize,
            centerX, centerY + crossSize,
            centerPaint
        )
    }

    private fun drawGuidanceText(canvas: Canvas) {
        val text = if (isAlignedState) {
            "Perfect! Ready to capture"
        } else if (detectionRect != null) {
            "Adjust position to align"
        } else {
            "Position face within frame"
        }

        val textColor = if (isAlignedState) Color.GREEN else Color.WHITE
        textPaint.color = textColor

        val textY = frameRect.top - textPaint.textSize - 20f
        canvas.drawText(text, width / 2f, textY, textPaint)
    }

    /**
     * Enhanced alignment check with size and position thresholds
     * @param detectedRect Normalized coordinates (0-1) of detected face/object
     * @return true if properly aligned and sized
     */
    fun checkAlignment(detectedRect: RectF): Boolean {
        // Check if the detected rectangle is large enough
        val sizeOk = detectedRect.width() >= sizeThreshold &&
                detectedRect.height() >= sizeThreshold

        // Check if the detected rectangle is centered enough
        val centerX = detectedRect.centerX()
        val centerY = detectedRect.centerY()
        val positionOk = abs(centerX - 0.5f) < (1 - alignmentThreshold)/2 &&
                abs(centerY - 0.5f) < (1 - alignmentThreshold)/2

        return sizeOk && positionOk
    }

    companion object {
        private fun abs(value: Float) = if (value < 0) -value else value
    }
}