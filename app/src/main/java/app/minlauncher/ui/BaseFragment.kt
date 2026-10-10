package app.minlauncher.ui

import android.animation.Animator
import android.animation.ValueAnimator
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.minlauncher.helper.isEinkDisplay
import app.minlauncher.helper.isSystemAnimationsDisabled
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

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

    /**
     * Collects [flow] while this fragment's view is at least STARTED,
     * cancelling and restarting collection across stop/start cycles.
     * Centralizes the lifecycleScope + repeatOnLifecycle nesting that every
     * screen's observers would otherwise repeat.
     */
    protected fun <T> collectOnStart(
        flow: Flow<T>,
        block: (T) -> Unit,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collect { block(it) }
            }
        }
    }
}
