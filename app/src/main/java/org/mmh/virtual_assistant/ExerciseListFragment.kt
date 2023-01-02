package org.mmh.virtual_assistant

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import org.mmh.virtual_assistant.core.ExerciseListAdapter
import org.mmh.virtual_assistant.exercise.home.HomeExercise

class ExerciseListFragment(
    private val assessmentId: String,
    private val assessmentDate: String,
    private val patientId: String,
    private val tenant: String,
    private val exerciseList: List<HomeExercise>
) : Fragment() {
    private lateinit var adapter: RecyclerView

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_exercise_list, container, false)
        adapter = view.findViewById(R.id.exercise_list_container)
        val displayTestId: TextView = view.findViewById(R.id.test_id_display)
        val displayTestDate: TextView = view.findViewById(R.id.test_date_display)
        val searchExercise: SearchView = view.findViewById(R.id.search_exercise)
        displayTestId.text = displayTestId.context.getString(R.string.test_id).format(assessmentId)
        displayTestDate.text =
            displayTestDate.context.getString(R.string.test_date).format(assessmentDate)

        searchExercise.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(searchQuery: String): Boolean {
                if (searchQuery.isNotEmpty()) {
                    adapter.adapter = ExerciseListAdapter(
                        testId = assessmentId,
                        testDate = assessmentDate,
                        exerciseList = exerciseList.filter {
                            it.name.lowercase().contains(searchQuery.lowercase())
                        },
                        fullExerciseList = exerciseList,
                        manager = parentFragmentManager,
                        patientId = patientId,
                        tenant = tenant
                    )
                    adapter.adapter?.notifyDataSetChanged()
                }
                searchExercise.clearFocus()
                return true
            }

            override fun onQueryTextChange(searchQuery: String): Boolean {
                if (searchQuery.isNotEmpty()) {
                    adapter.adapter = ExerciseListAdapter(
                        testId = assessmentId,
                        testDate = assessmentDate,
                        exerciseList = exerciseList.filter {
                            it.name.lowercase().contains(searchQuery.lowercase())
                        },
                        fullExerciseList = exerciseList,
                        manager = parentFragmentManager,
                        patientId = patientId,
                        tenant = tenant
                    )
                    adapter.adapter?.notifyDataSetChanged()
                }
                return true
            }
        })

        searchExercise.setOnCloseListener {
            adapter.adapter = ExerciseListAdapter(
                assessmentId,
                assessmentDate,
                exerciseList,
                exerciseList,
                parentFragmentManager,
                patientId = patientId,
                tenant = tenant
            )
            adapter.adapter?.notifyDataSetChanged()
            searchExercise.clearFocus()
            true
        }
        adapter.adapter = ExerciseListAdapter(
            assessmentId,
            assessmentDate,
            exerciseList,
            exerciseList,
            parentFragmentManager,
            patientId = patientId,
            tenant = tenant
        )
        return view
    }

    override fun onResume() {
        super.onResume()
        adapter.adapter = ExerciseListAdapter(
            assessmentId,
            assessmentDate,
            exerciseList,
            exerciseList,
            parentFragmentManager,
            patientId = patientId,
            tenant = tenant
        )
        adapter.adapter?.notifyDataSetChanged()
    }
}
