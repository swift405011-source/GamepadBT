package com.gamepad.bt

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

class GamepadView(context: Context, private val pad: GamepadHid) : View(context) {

    private class Btn(
        val rect: RectF, val label: String, val bit: Int,
        val color: Int, val round: Boolean
    )

    private val btns = ArrayList<Btn>()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = 0xFF888888.toInt()
    }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    private var r = 0f
    private var lsx = 0f
    private var lsy = 0f
    private var rsx = 0f
    private var rsy = 0f
    private var dpx = 0f
    private var dpy = 0f
    private var dpr = 0f

    private var leftId = -1
    private var rightId = -1

    private var curBits = 0
    private var curHat = 0
    private var cLx = 0
    private var cLy = 0
    private var cRx = 0
    private var cRy = 0

    init {
        keepScreenOn = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val wf = w.toFloat()
        val hf = h.toFloat()
        r = hf * 0.17f
        lsx = wf * 0.13f
        lsy = hf * 0.42f
        rsx = wf * 0.70f
        rsy = hf * 0.72f
        dpx = wf * 0.30f
        dpy = hf * 0.76f
        dpr = hf * 0.15f
        txt.textSize = hf * 0.05f

        btns.clear()
        val gray = 0xFF444444.toInt()
        fun box(l: Float, t: Float, rt: Float, b: Float) = RectF(l * wf, t * hf, rt * wf, b * hf)

        btns.add(Btn(box(0.02f, 0.02f, 0.14f, 0.14f), "LB", GamepadHid.BTN_LB, gray, false))
        btns.add(Btn(box(0.15f, 0.02f, 0.27f, 0.14f), "LT", GamepadHid.BTN_LT, gray, false))
        btns.add(Btn(box(0.73f, 0.02f, 0.85f, 0.14f), "RT", GamepadHid.BTN_RT, gray, false))
        btns.add(Btn(box(0.86f, 0.02f, 0.98f, 0.14f), "RB", GamepadHid.BTN_RB, gray, false))
        btns.add(Btn(box(0.38f, 0.04f, 0.47f, 0.12f), "BACK", GamepadHid.BTN_BACK, gray, false))
        btns.add(Btn(box(0.53f, 0.04f, 0.62f, 0.12f), "START", GamepadHid.BTN_START, gray, false))

        val bx = wf * 0.87f
        val by = hf * 0.42f
        val d = hf * 0.15f
        val br = hf * 0.085f
        fun circ(cx: Float, cy: Float) = RectF(cx - br, cy - br, cx + br, cy + br)
        btns.add(Btn(circ(bx, by + d), "A", GamepadHid.BTN_A, 0xFF2E9E3E.toInt(), true))
        btns.add(Btn(circ(bx + d, by), "B", GamepadHid.BTN_B, 0xFFD13438.toInt(), true))
        btns.add(Btn(circ(bx - d, by), "X", GamepadHid.BTN_X, 0xFF2D6CDF.toInt(), true))
        btns.add(Btn(circ(bx, by - d), "Y", GamepadHid.BTN_Y, 0xFFE0A800.toInt(), true))
    }

    private fun hitBtn(b: Btn, x: Float, y: Float): Boolean {
        val m = b.rect.width() * 0.12f
        return x >= b.rect.left - m && x <= b.rect.right + m &&
            y >= b.rect.top - m && y <= b.rect.bottom + m
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val action = e.actionMasked
        val idx = e.actionIndex

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            val x = e.getX(idx)
            val y = e.getY(idx)
            val id = e.getPointerId(idx)
            if (leftId == -1 && hypot(x - lsx, y - lsy) < r * 1.6f) {
                leftId = id
            } else if (rightId == -1 && hypot(x - rsx, y - rsy) < r * 1.6f) {
                rightId = id
            }
        }

        var skip = -1
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            skip = idx
            val id = e.getPointerId(idx)
            if (id == leftId) leftId = -1
            if (id == rightId) rightId = -1
        }
        val cancel = action == MotionEvent.ACTION_CANCEL
        if (cancel) {
            leftId = -1
            rightId = -1
        }

        var bits = 0
        var up = false
        var down = false
        var left = false
        var right = false
        var lx = 0f
        var ly = 0f
        var rx = 0f
        var ry = 0f

        if (!cancel) {
            for (i in 0 until e.pointerCount) {
                if (i == skip) continue
                val x = e.getX(i)
                val y = e.getY(i)
                val id = e.getPointerId(i)
                if (id == leftId) {
                    var dx = (x - lsx) / r
                    var dy = (y - lsy) / r
                    val len = hypot(dx, dy)
                    if (len > 1f) {
                        dx /= len
                        dy /= len
                    }
                    lx = dx
                    ly = dy
                } else if (id == rightId) {
                    var dx = (x - rsx) / r
                    var dy = (y - rsy) / r
                    val len = hypot(dx, dy)
                    if (len > 1f) {
                        dx /= len
                        dy /= len
                    }
                    rx = dx
                    ry = dy
                } else {
                    val dx = x - dpx
                    val dy = y - dpy
                    if (hypot(dx, dy) < dpr * 1.6f) {
                        val t = dpr * 0.35f
                        if (dy < -t) up = true
                        if (dy > t) down = true
                        if (dx < -t) left = true
                        if (dx > t) right = true
                    }
                    for (b in btns) {
                        if (hitBtn(b, x, y)) bits = bits or (1 shl b.bit)
                    }
                }
            }
        }

        val hat = when {
            up && right -> 2
            down && right -> 4
            down && left -> 6
            up && left -> 8
            up -> 1
            right -> 3
            down -> 5
            left -> 7
            else -> 0
        }
        val ax = (lx * 127).toInt()
        val ay = (ly * 127).toInt()
        val bx = (rx * 127).toInt()
        val by = (ry * 127).toInt()

        if (bits != curBits || hat != curHat || ax != cLx || ay != cLy || bx != cRx || by != cRy) {
            curBits = bits
            curHat = hat
            cLx = ax
            cLy = ay
            cRx = bx
            cRy = by
            pad.send(bits, hat, ax, ay, bx, by)
            invalidate()
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(0xFF111111.toInt())

        drawStick(canvas, lsx, lsy, cLx / 127f, cLy / 127f)
        drawStick(canvas, rsx, rsy, cRx / 127f, cRy / 127f)

        val h = curHat
        drawDir(canvas, dpx, dpy - dpr * 0.9f, h == 1 || h == 2 || h == 8)
        drawDir(canvas, dpx, dpy + dpr * 0.9f, h == 4 || h == 5 || h == 6)
        drawDir(canvas, dpx - dpr * 0.9f, dpy, h == 6 || h == 7 || h == 8)
        drawDir(canvas, dpx + dpr * 0.9f, dpy, h == 2 || h == 3 || h == 4)

        for (b in btns) {
            val pressed = ((curBits shr b.bit) and 1) == 1
            fill.color = if (pressed) Color.WHITE else b.color
            if (b.round) {
                canvas.drawCircle(b.rect.centerX(), b.rect.centerY(), b.rect.width() / 2, fill)
            } else {
                canvas.drawRoundRect(b.rect, 24f, 24f, fill)
            }
            txt.color = if (pressed) Color.BLACK else Color.WHITE
            canvas.drawText(b.label, b.rect.centerX(), b.rect.centerY() + txt.textSize / 3, txt)
        }
    }

    private fun drawStick(canvas: Canvas, cx: Float, cy: Float, kx: Float, ky: Float) {
        canvas.drawCircle(cx, cy, r, line)
        fill.color = 0xFF666666.toInt()
        canvas.drawCircle(cx + kx * r, cy + ky * r, r * 0.45f, fill)
    }

    private fun drawDir(canvas: Canvas, cx: Float, cy: Float, active: Boolean) {
        fill.color = if (active) Color.WHITE else 0xFF444444.toInt()
        canvas.drawCircle(cx, cy, dpr * 0.45f, fill)
    }
}
