package www.com.petsitternow_app.view.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import www.com.petsitternow_app.R

class RatingBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var requestId: String = ""
    private var variant: String = "petsitter" // "petsitter" | "owner"
    private var targetName: String? = null

    private var selectedScore: Int = 0
    private val starButtons: MutableList<ImageButton> = mutableListOf()

    private var tvRatingTitle: TextView? = null
    private var tvRatingDescription: TextView? = null
    private var tvCommentCount: TextView? = null
    private var btnSubmitRating: MaterialButton? = null
    private var progressSubmit: ProgressBar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rating_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let { args ->
            requestId = args.getString(ARG_REQUEST_ID, "")
            variant = args.getString(ARG_VARIANT, "petsitter")
            targetName = args.getString(ARG_TARGET_NAME)?.takeIf { it.isNotBlank() }
        }

        tvRatingTitle = view.findViewById(R.id.tvRatingTitle)
        tvRatingDescription = view.findViewById(R.id.tvRatingDescription)
        tvCommentCount = view.findViewById(R.id.tvCommentCount)
        btnSubmitRating = view.findViewById(R.id.btnSubmitRating)
        progressSubmit = view.findViewById(R.id.progressSubmit)

        starButtons.clear()
        listOf(R.id.btnStar1, R.id.btnStar2, R.id.btnStar3, R.id.btnStar4, R.id.btnStar5).forEachIndexed { index, id ->
            val btn = view.findViewById<ImageButton>(id)
            starButtons.add(btn)
            val score = index + 1
            btn.setOnClickListener {
                selectedScore = score
                updateStarsUi()
                btnSubmitRating?.isEnabled = true
            }
        }

        val isOwnerVariant = variant == "owner"
        tvRatingTitle?.text = getString(if (isOwnerVariant) R.string.rating_title_owner else R.string.rating_title_petsitter)
        val desc = if (targetName != null) {
            if (isOwnerVariant) getString(R.string.rating_description_owner_with_name, targetName!!)
            else getString(R.string.rating_description_with_name, targetName!!)
        } else {
            getString(if (isOwnerVariant) R.string.rating_description_owner else R.string.rating_description_petsitter)
        }
        tvRatingDescription?.text = desc

        view.findViewById<EditText>(R.id.etComment).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tvCommentCount?.text = "${s?.length ?: 0}/500"
            }
        })

        btnSubmitRating?.setOnClickListener {
            if (selectedScore !in 1..5) return@setOnClickListener
            val comment = view.findViewById<EditText>(R.id.etComment).text?.toString()?.trim()?.take(500)
            progressSubmit?.visibility = View.VISIBLE
            btnSubmitRating?.isEnabled = false
            val commentResult = comment.let { if (it.isNullOrEmpty()) null else it }
            parentFragmentManager.setFragmentResult(RATING_REQUEST_KEY, Bundle().apply {
                putString(KEY_REQUEST_ID, requestId)
                putInt(KEY_SCORE, selectedScore)
                putString(KEY_COMMENT, commentResult)
                putString(KEY_VARIANT, variant)
            })
            dismiss()
        }
    }

    private fun updateStarsUi() {
        val filledResId = R.drawable.ic_star
        val outlineResId = R.drawable.ic_star_outline
        val filledTint = ContextCompat.getColor(requireContext(), R.color.star_filled)
        val outlineTint = ContextCompat.getColor(requireContext(), R.color.star_outline)
        starButtons.forEachIndexed { index, btn ->
            val score = index + 1
            if (score <= selectedScore) {
                btn.setImageResource(filledResId)
                btn.setColorFilter(filledTint)
            } else {
                btn.setImageResource(outlineResId)
                btn.setColorFilter(outlineTint)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tvRatingTitle = null
        tvRatingDescription = null
        tvCommentCount = null
        btnSubmitRating = null
        progressSubmit = null
        starButtons.clear()
    }

    companion object {
        const val RATING_REQUEST_KEY = "rating_result_request"

        const val KEY_REQUEST_ID = "requestId"
        const val KEY_SCORE = "score"
        const val KEY_COMMENT = "comment"
        const val KEY_VARIANT = "variant"

        private const val ARG_REQUEST_ID = "requestId"
        private const val ARG_VARIANT = "variant"
        private const val ARG_TARGET_NAME = "targetName"

        fun newInstance(
            requestId: String,
            variant: String,
            targetName: String? = null
        ): RatingBottomSheetDialogFragment {
            return RatingBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_REQUEST_ID, requestId)
                    putString(ARG_VARIANT, variant)
                    putString(ARG_TARGET_NAME, targetName ?: "")
                }
            }
        }
    }
}
