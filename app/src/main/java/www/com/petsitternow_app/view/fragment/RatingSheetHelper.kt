package www.com.petsitternow_app.view.fragment

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.domain.model.WalkRequest


object RatingSheetHelper {

    fun setupRatingResultListener(
        fragment: Fragment,
        onRefresh: () -> Unit,
        submit: (requestId: String, score: Int, comment: String?, variant: String) -> Flow<Result<Unit>>
    ) {
        fragment.childFragmentManager.setFragmentResultListener(
            RatingBottomSheetDialogFragment.RATING_REQUEST_KEY,
            fragment.viewLifecycleOwner
        ) { _, bundle ->
            val requestId = bundle.getString(RatingBottomSheetDialogFragment.KEY_REQUEST_ID) ?: return@setFragmentResultListener
            val score = bundle.getInt(RatingBottomSheetDialogFragment.KEY_SCORE)
            val comment = bundle.getString(RatingBottomSheetDialogFragment.KEY_COMMENT)
            val variant = bundle.getString(RatingBottomSheetDialogFragment.KEY_VARIANT, "petsitter")
            fragment.viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val result = submit(requestId, score, comment, variant).first()
                    result.fold(
                        onSuccess = {
                            fragment.view?.let { Snackbar.make(it, R.string.rating_success, Snackbar.LENGTH_SHORT).show() }
                            onRefresh()
                        },
                        onFailure = { e ->
                            fragment.view?.let {
                                Snackbar.make(it, e.message ?: fragment.getString(R.string.rating_error_send), Snackbar.LENGTH_LONG).show()
                            }
                        }
                    )
                } catch (e: Exception) {
                    fragment.view?.let {
                        Snackbar.make(it, e.message ?: fragment.getString(R.string.rating_error_send), Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun showRatingSheet(fragment: Fragment, walk: WalkRequest, variant: String) {
        val sheet = RatingBottomSheetDialogFragment.newInstance(
            requestId = walk.id,
            variant = variant,
            targetName = if (variant == "petsitter") walk.petsitter?.name else walk.owner.name
        )
        sheet.show(fragment.childFragmentManager, "RatingBottomSheet")
    }
}
