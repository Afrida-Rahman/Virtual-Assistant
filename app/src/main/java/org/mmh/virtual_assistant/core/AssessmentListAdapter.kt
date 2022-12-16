package org.mmh.virtual_assistant.core

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.mmh.virtual_assistant.ExerciseListActivity
import org.mmh.virtual_assistant.R
import org.mmh.virtual_assistant.domain.model.TestId

class AssessmentListAdapter(
    private val testList: List<TestId>
) : RecyclerView.Adapter<AssessmentListAdapter.AssessmentItemViewHolder>() {

    class AssessmentItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val testId: TextView = view.findViewById(R.id.test_id)
        val testDate: TextView = view.findViewById(R.id.test_date)
        val reportReadyIcon: ImageView = view.findViewById(R.id.report_ready_icon)
        val providerName: TextView = view.findViewById(R.id.provider_name)
        val bodyRegion: TextView = view.findViewById(R.id.body_region)
        val registrationType: TextView = view.findViewById(R.id.registration_type)
        val exerciseCount: TextView = view.findViewById(R.id.exercise_count)
        val goToExerciseList: Button = view.findViewById(R.id.go_to_exercise_list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssessmentItemViewHolder {
        return AssessmentItemViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_assessment, parent, false)
        )
    }

    override fun onBindViewHolder(holder: AssessmentItemViewHolder, position: Int) {
        val item = testList[position]
        val context = holder.testId.context
        holder.apply {
            reportReadyIcon.setImageResource(if (item.isReportReady) R.drawable.ic_check else R.drawable.ic_cross)
            context.apply {
                testId.text = getString(R.string.test_id, item.id)
                testDate.text = getString(R.string.test_date, item.testDate)
                providerName.text =
                    getString(R.string.provider_name_value, item.providerName ?: "Unknown")
                bodyRegion.text = getString(R.string.body_region_value, item.bodyRegionName)
                registrationType.text =
                    getString(R.string.registration_type_value, item.registrationType)
                exerciseCount.text = getString(R.string.exercise_count, item.totalExercises)
                if (item.totalExercises <= 0) {
                    goToExerciseList.isEnabled = false
                } else {
                    goToExerciseList.isEnabled = true
                    goToExerciseList.setOnClickListener {
                        val intent = Intent(context, ExerciseListActivity::class.java)
                        intent.apply {
                            putExtra(ExerciseListActivity.TEST_ID, item.id)
                            putExtra(ExerciseListActivity.TEST_DATE, item.testDate)
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }

    override fun getItemCount() = testList.size
}