package org.mmh.virtual_assistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.mmh.virtual_assistant.api.response.Assessment
import org.mmh.virtual_assistant.core.AssessmentListAdapter
import org.mmh.virtual_assistant.domain.model.TestId

class AssessmentListFragment(
    private val assessments: List<Assessment>,
    private val width: Int = 0
) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_assessment_list, container, false)
        val adapter = view.findViewById<RecyclerView>(R.id.assessment_list_container)
        val searchAssessment: SearchView = view.findViewById(R.id.search_assessment)
        val testList = mutableListOf<TestId>()
        assessments.forEach { assessment ->
            testList.add(
                TestId(
                    id = assessment.TestId,
                    bodyRegionId = assessment.BodyRegionId,
                    bodyRegionName = assessment.BodyRegionName,
                    providerName = assessment.ProviderName,
                    providerId = assessment.ProviderId,
                    testDate = assessment.CreatedOnUtc.split("T")[0],
                    isReportReady = assessment.IsReportReady,
                    registrationType = assessment.RegistrationType,
                    totalExercises = assessment.TotalExercise
                )
            )
        }
        testList.sortBy { it.testDate }
        testList.reverse()
        searchAssessment.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(searchQuery: String): Boolean {
                if (searchQuery.isNotEmpty()) {
                    adapter.adapter = AssessmentListAdapter(
                        testList.filter { it.id.lowercase().contains(searchQuery.lowercase()) }
                    )
                    adapter.adapter?.notifyDataSetChanged()
                }
                searchAssessment.clearFocus()
                return true
            }

            override fun onQueryTextChange(searchQuery: String): Boolean {
                if (searchQuery.isNotEmpty()) {
                    adapter.adapter = AssessmentListAdapter(
                        testList.filter { it.id.lowercase().contains(searchQuery.lowercase()) }
                    )
                    adapter.adapter?.notifyDataSetChanged()
                }
                return true
            }
        })

        searchAssessment.setOnCloseListener {
            adapter.adapter = AssessmentListAdapter(testList)
            adapter.adapter?.notifyDataSetChanged()
            searchAssessment.clearFocus()
            true
        }
        if (width > 1300) {
            adapter.layoutManager = GridLayoutManager(context, 2)
        }
        adapter.adapter = AssessmentListAdapter(testList)
        return view
    }
}