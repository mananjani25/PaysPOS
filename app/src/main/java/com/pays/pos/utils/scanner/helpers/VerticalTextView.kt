package com.pays.pos.utils.scanner.helpers

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import com.pays.pos.utils.scanner.helpers.CustomProgressDialog
import com.pays.pos.utils.scanner.helpers.Foreground
import com.pays.pos.utils.scanner.helpers.ManagedVibrator
import com.zebra.scannercontrol.FirmwareUpdateEvent
import com.pays.pos.utils.scanner.helpers.ScannerAppEngine.IScannerAppEngineDevListDelegate
import com.pays.pos.utils.scanner.helpers.ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate
import com.pays.pos.utils.scanner.helpers.ScannerAppEngine.IScannerAppEngineDevEventsDelegate
import com.zebra.scannercontrol.DCSScannerInfo
import com.zebra.scannercontrol.DCSSDKDefs.DCSSDK_RESULT
import com.zebra.scannercontrol.DCSSDKDefs.DCSSDK_MODE
import com.zebra.scannercontrol.DCSSDKDefs.DCSSDK_COMMAND_OPCODE
import com.pays.pos.utils.scanner.helpers.SSASymbologyType
import com.zebra.scannercontrol.RMDAttributes
import com.pays.pos.utils.scanner.helpers.Symbology

/**
 * Created by BPallewela on 12/13/2017.
 */
class VerticalTextView(
    context: Context?,
    attrs: AttributeSet?
) : AppCompatTextView(
    context!!, attrs
) {
    var topDown = false
    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        super.onMeasure(
            heightMeasureSpec,
            widthMeasureSpec
        )
        setMeasuredDimension(
            measuredHeight,
            measuredWidth
        )
    }

    override fun onDraw(canvas: Canvas) {
        val textPaint = paint
        textPaint.color = currentTextColor
        textPaint.drawableState = drawableState
        canvas.save()
        if (topDown) {
            canvas.translate(width.toFloat(), 0f)
            canvas.rotate(90f)
        } else {
            canvas.translate(0f, height.toFloat())
            canvas.rotate(-90f)
        }
        canvas.translate(
            compoundPaddingLeft.toFloat(),
            extendedPaddingTop.toFloat()
        )
        layout.draw(canvas)
        canvas.restore()
    }

    init {
        val gravity = gravity
        topDown = if (Gravity.isVertical(gravity)
            && gravity and Gravity.VERTICAL_GRAVITY_MASK
            == Gravity.BOTTOM
        ) {
            setGravity(
                gravity and Gravity.HORIZONTAL_GRAVITY_MASK
                        or Gravity.TOP
            )
            false
        } else {
            true
        }
    }
}