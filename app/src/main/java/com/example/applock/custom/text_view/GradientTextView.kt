package com.example.applock.custom.text_view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.example.applock.R
import kotlin.math.tan

class GradientTextView : AppCompatTextView {
    private var isVertical = false
    private var startColor = Color.BLUE
    private var endColor = Color.GREEN
    private var angle = 0f
    private val listColor by lazy { ArrayList<String>() }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    fun setColorText(startColor: Int, endColor: Int) {
        this.startColor = startColor
        this.endColor = endColor
        requestLayout()
    }

    fun setListColor(listColor: List<String>) {
        this.listColor.clear()
        this.listColor.addAll(listColor)
        requestLayout()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            val attributes = context.obtainStyledAttributes(attrs, R.styleable.GradientTextView)
            isVertical = attributes.getBoolean(R.styleable.GradientTextView_isVertical, true)
            startColor = attributes.getColor(R.styleable.GradientTextView_endColor, startColor)
            endColor = attributes.getColor(R.styleable.GradientTextView_startColor, endColor)
            angle = attributes.getFloat(R.styleable.GradientTextView_angle, angle)
            attributes.recycle()
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (listColor.isEmpty()) drawTwoColor()
        else drawListColor()


    }

    private fun drawListColor() {
        val paint: Paint = paint
        val width = paint.measureText(text.toString())

        val colorsArray = IntArray(listColor.size)
        val positionArray = FloatArray(listColor.size)
        listColor.forEachIndexed { index, colorHex ->
            colorsArray[index] = Color.parseColor(colorHex)
            positionArray[index] = index * 1f/listColor.size - index * 1f/listColor.size * 1/tan(angle) / 1000f
        }


        if (isVertical) {
            val matrix = Matrix()
            matrix.setRotate(angle)
            val shader = LinearGradient(
                0f, 0f, width, lineHeight.toFloat(),
                colorsArray,
                if (angle % 360 == 0f) null else positionArray,
                Shader.TileMode.CLAMP
            )
            shader.setLocalMatrix(matrix)
            getPaint().setShader(shader)
        } else {
            getPaint().setShader(
                LinearGradient(
                    0f, 0f, 0f, lineHeight.toFloat(),
                    colorsArray,
                    null,
                    Shader.TileMode.CLAMP
                )
            )
        }
    }

    private fun drawTwoColor() {
        val paint: Paint = paint
        val width = paint.measureText(text.toString())

        if (isVertical) {
            val matrix = Matrix()
            matrix.setRotate(45f)
            val shader = LinearGradient(
                0f, 0f, width, lineHeight.toFloat(),
                endColor,
                startColor,
                Shader.TileMode.CLAMP
            )
            shader.setLocalMatrix(matrix)
            getPaint().setShader(shader)
        } else {
            getPaint().setShader(
                LinearGradient(
                    0f, 0f, 0f, lineHeight.toFloat(),
                    endColor,
                    startColor,
                    Shader.TileMode.CLAMP
                )
            )
        }
    }
}