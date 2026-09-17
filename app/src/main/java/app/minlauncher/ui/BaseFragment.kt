package app.minlauncher.ui

import android.animation.Animator
import android.animation.ValueAnimator
import androidx.fragment.app.Fragment
import app.minlauncher.helper.isEinkDisplay
import app.minlauncher.helper.isSystemAnimationsDisabled

open class BaseFragment : Fragment() {
    override fun onCreateAnimator(
        transit: Int,
        enter: Boolean,
        nextAnim: Int,
    ): Animator? {
        if (nextAnim != 0 && (requireContext().isSystemAnimationsDisabled() || requireContext().isEinkDisplay())) {
            return ValueAnimator.ofFloat(0f, 1f).setDuration(0)
        }
        return null
    }
}
