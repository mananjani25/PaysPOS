package com.pays.pos.utils.scanner.helpers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.pays.pos.R
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
 * Created by pndv47 on 7/4/2016.
 */
class DotsProgressBar : View {
    private val mRadius = 0f
    private val mPaintFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mHandler = Handler()
    private var mIndex = 0
    private var widthSize = 0
    private var heightSize = 0
    private val margin = 4
    private var mDotCount = 3

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context)
    }

    private fun init(context: Context) {
        //TODO scanner
        //mRadius = context.getResources().getDimension(R.dimen.circle_indicator_radius);
        // dot fill color
        mPaintFill.style = Paint.Style.FILL
        mPaintFill.color = ContextCompat.getColor(
            context,
            R.color.txt_color_blue
        )
        // dot background color
        mPaint.style = Paint.Style.FILL
        mPaint.color =
            ContextCompat.getColor(context, R.color.gray_color)
        start()
    }

    fun setDotsCount(count: Int) {
        mDotCount = count
    }

    fun start() {
        mIndex = -1
        mHandler.removeCallbacks(mRunnable)
        mHandler.post(mRunnable)
    }

    fun stop() {
        mHandler.removeCallbacks(mRunnable)
    }

    private var step = 1
    private val mRunnable: Runnable = object : Runnable {
        override fun run() {
            mIndex += step
            if (mIndex < 0) {
                mIndex = 1
                step = 1
            } else if (mIndex > mDotCount - 1) {
                if (mDotCount - 2 >= 0) {
                    mIndex = mDotCount - 2
                    step = -1
                } else {
                    mIndex = 0
                    step = 1
                }
            }
            invalidate()
            mHandler.postDelayed(this, 300)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        widthSize = MeasureSpec.getSize(widthMeasureSpec)
        heightSize = mRadius.toInt() * 2 + paddingBottom + paddingTop
        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        var dX = (widthSize - mDotCount * mRadius * 2 - (mDotCount - 1) * margin) / 2.0f
        val dY = (heightSize / 2).toFloat()
        for (i in 0 until mDotCount) {
            if (i == mIndex) {
                canvas.drawCircle(dX, dY, mRadius, mPaintFill)
            } else {
                canvas.drawCircle(dX, dY, mRadius, mPaint)
            }
            dX += 2 * mRadius + margin
        }
    }
}