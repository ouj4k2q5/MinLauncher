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

private const val TAG = "OnSwipeTouchListener"
private const val SWIPE_THRESHOLD = 100
private const val SWIPE_VELOCITY_THRESHOLD = 100

/*
Swipe, double tap and long press touch listener for a view
Source: https://www.tutorialspoint.com/how-to-handle-swipe-gestures-in-kotlin
*/

internal open class OnSwipeTouchListener(
    c: Context?,
) : OnTouchListener {
    private var longPressOn = false
    private val handler = Handler(Looper.getMainLooper())
    private var longPressAction: Runnable? = null

    //    private var doubleTapOn = false
    private val gestureDetector: GestureDetector

    override fun onTouch(
        view: View,
        motionEvent: MotionEvent,
    ): Boolean {
        if (motionEvent.action == MotionEvent.ACTION_UP || motionEvent.action == MotionEvent.ACTION_CANCEL) {
            longPressOn = false
            longPressAction?.let(handler::removeCallbacks)
        }
        return gestureDetector.onTouchEvent(motionEvent)
    }

    private inner class GestureListener : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
//            if (doubleTapOn) {
//                doubleTapOn = false
//                onTripleClick()
//            }
            onClick()
            return super.onSingleTapUp(e)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
//            doubleTapOn = true
//            Timer().schedule(Constants.TRIPLE_TAP_DELAY_MS) {
//                if (doubleTapOn) {
//                    doubleTapOn = false
//                    onDoubleClick()
//                }
//            }
            onDoubleClick()
            return super.onDoubleTap(e)
        }

        override fun onLongPress(e: MotionEvent) {
            longPressOn = true
            longPressAction?.let(handler::removeCallbacks)
            val action =
                Runnable {
                    if (longPressOn) onLongClick()
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

    open fun onLongClick() {}

    open fun onDoubleClick() {}

    open fun onTripleClick() {}

    open fun onClick() {}

    init {
        gestureDetector = GestureDetector(c, GestureListener())
    }
}
