package app.minlauncher.listener

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import app.minlauncher.data.Constants
import kotlin.math.abs

private const val TAG = "ViewSwipeTouchListener"
private const val SWIPE_THRESHOLD = 100
private const val SWIPE_VELOCITY_THRESHOLD = 100

internal open class ViewSwipeTouchListener(
    c: Context?,
    v: View,
) : OnTouchListener {
    private var longPressOn = false
    private val handler = Handler(Looper.getMainLooper())
    private var longPressAction: Runnable? = null
    private val gestureDetector: GestureDetector

    override fun onTouch(
        view: View,
        motionEvent: MotionEvent,
    ): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> view.isPressed = true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.isPressed = false
                longPressOn = false
                longPressAction?.let(handler::removeCallbacks)
            }
        }
        return gestureDetector.onTouchEvent(motionEvent)
    }

    private inner class GestureListener(
        private val view: View,
    ) : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onClick(view)
            return super.onSingleTapUp(e)
        }

        override fun onLongPress(e: MotionEvent) {
            longPressOn = true
            longPressAction?.let(handler::removeCallbacks)
            val action =
                Runnable {
                    if (longPressOn) onLongClick(view)
                }
            longPressAction = action
            handler.postDelayed(action, Constants.LONG_PRESS_DELAY_MS)
            super.onLongPress(e)
        }

        override fun onFling(
            event1: MotionEvent?,
            event2: MotionEvent,
            velocityX: Float,
            velocityY: Float,
        ): Boolean {
            try {
                val diffY = event2.y - (event1?.y ?: 0F)
                val diffX = event2.x - (event1?.x ?: 0F)
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) onSwipeRight() else onSwipeLeft()
                    }
                } else {
                    if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY < 0) onSwipeUp() else onSwipeDown()
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to handle fling gesture", exception)
            }
            return false
        }
    }

    open fun onSwipeRight() {}

    open fun onSwipeLeft() {}

    open fun onSwipeUp() {}

    open fun onSwipeDown() {}

    open fun onLongClick(view: View) {}

    open fun onClick(view: View) {}

    init {
        gestureDetector = GestureDetector(c, GestureListener(v))
        v.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(p0: View) {
                    // Nothing to do when the view is attached
                }

                override fun onViewDetachedFromWindow(p0: View) {
                    longPressOn = false
                    longPressAction?.let(handler::removeCallbacks)
                }
            },
        )
    }
}
