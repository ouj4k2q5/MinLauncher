package app.minlauncher.ui.home

import androidx.compose.runtime.Composable
import app.minlauncher.HomeViewModel

/**
 * The home screen's host: adapts the [HomeViewModel] state and the resolved
 * colors/callbacks into [HomeScreen]. Moved out of the old fragment (Stage 1 of
 * the fragment removal) so the UI no longer depends on the fragment's held
 * state; HomeEntry now owns the observers that populate the ViewModel.
 */
@Composable
internal fun HomeScreenHost(
    viewModel: HomeViewModel,
    colors: HomeColors,
    callbacks: HomeCallbacks,
) {
    HomeScreen(
        state =
            HomeUiState(
                slots = viewModel.slots,
                homeAppAlignment = viewModel.homeAppAlignment,
                homeBottomAlignment = viewModel.homeBottomAlignment,
                dateTimeVisibility = viewModel.dateTimeVisibility,
                showStatusBar = viewModel.showStatusBar,
                showFirstRunTips = viewModel.showFirstRunTips,
                showSetDefaultLauncher = viewModel.showSetDefaultLauncher,
                screenTime = viewModel.screenTimeText,
            ),
        colors = colors,
        clock = HomeClockSource.clock,
        callbacks = callbacks,
    )
}
