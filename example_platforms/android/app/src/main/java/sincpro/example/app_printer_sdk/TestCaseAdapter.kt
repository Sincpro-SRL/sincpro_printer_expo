package sincpro.example.app_printer_sdk

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import sincpro.example.app_printer_sdk.databinding.ItemTestCaseBinding

class TestCaseAdapter(
    private val onRunClick: (TestCase) -> Unit
) : ListAdapter<TestCase, TestCaseAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTestCaseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemTestCaseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(testCase: TestCase) {
            binding.tvTestName.text = testCase.name
            binding.tvTestDescription.text = testCase.description
            binding.tvTestCategory.text = testCase.category.name

            val (statusText, statusColor) = when (testCase.status) {
                TestStatus.PENDING -> "PENDING" to android.R.color.darker_gray
                TestStatus.RUNNING -> "RUNNING..." to android.R.color.holo_blue_dark
                TestStatus.PASSED -> "✓ PASSED" to android.R.color.holo_green_dark
                TestStatus.FAILED -> "✗ FAILED" to android.R.color.holo_red_dark
                TestStatus.SKIPPED -> "SKIPPED" to android.R.color.holo_orange_dark
            }

            // Show result or error message
            val detailText = when {
                testCase.status == TestStatus.FAILED && testCase.errorMessage != null -> 
                    "Error: ${testCase.errorMessage}"
                testCase.status == TestStatus.PASSED && !testCase.resultMessage.isNullOrBlank() -> 
                    testCase.resultMessage
                else -> testCase.description
            }
            binding.tvTestDescription.text = detailText

            binding.tvStatus.text = statusText
            binding.tvStatus.setTextColor(
                ContextCompat.getColor(binding.root.context, statusColor)
            )

            binding.btnRun.isEnabled = testCase.status != TestStatus.RUNNING
            binding.btnRun.setOnClickListener { onRunClick(testCase) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TestCase>() {
        override fun areItemsTheSame(oldItem: TestCase, newItem: TestCase) = 
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TestCase, newItem: TestCase) = 
            oldItem.status == newItem.status && 
            oldItem.errorMessage == newItem.errorMessage &&
            oldItem.resultMessage == newItem.resultMessage
    }

    fun updateTestCase(testCase: TestCase) {
        val position = currentList.indexOfFirst { it.id == testCase.id }
        if (position >= 0) {
            notifyItemChanged(position)
        }
    }
}
