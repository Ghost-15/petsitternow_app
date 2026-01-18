package www.com.petsitternow_app.ui.walk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import www.com.petsitternow_app.ui.walk.components.WalkHistoryScreen

/**
 * Fragment displaying walk history for owners.
 */
@AndroidEntryPoint
class WalkHistoryFragment : Fragment() {

    private val viewModel: WalkHistoryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                MaterialTheme {
                    WalkHistoryScreen(
                        walks = uiState.walks,
                        isLoading = uiState.isLoading,
                        onRefresh = { viewModel.refresh() }
                    )
                }
            }
        }
    }
}
