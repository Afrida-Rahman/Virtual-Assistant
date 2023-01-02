package org.mmh.virtual_assistant.domain.model

import android.graphics.Color
import android.util.Log

data class Constraint(
    val type: ConstraintType,
    val startPointIndex: Int,
    val middlePointIndex: Int,
    val endPointIndex: Int,
    val clockWise: Boolean = false,
    var color: Int = Color.WHITE,
    var minValue: Int,
    var maxValue: Int,
    var looseMin: Int,
    var looseMax: Int,
    var standardMin: Int,
    var standardMax: Int,
    var storedValues: ArrayList<Int> = ArrayList()
) {
    fun getLooseConstraints(): looseValues {
        return looseValues(looseMin, looseMax)
    }

    fun getStandardConstraints(): standardValue {
        return standardValue(standardMin, standardMax)
    }

    fun getMinMaxMedian(): trimmedValues {
        val trimmedStoredValues = trimStoredValues(storedValues = storedValues)
        Log.d("looseConstraint", "trimmedStoredValues:: $trimmedStoredValues")
        if (trimmedStoredValues.isEmpty()) {
            return trimmedValues(0, 0, 0)
        }
        val median = calculateMedianValue(storedValues = trimmedStoredValues)
        val min = trimmedStoredValues[0]
        val max = trimmedStoredValues[trimmedStoredValues.count() - 1]

        return trimmedValues(min, max, median)
    }

    fun setRefinedConstraints(min: Int, max: Int) {
        minValue = min
        maxValue = max
    }

    fun setLooseConstraints() {
        minValue = looseMin
        maxValue = looseMax
    }

    fun setStandardConstraints() {
        minValue = standardMin
        maxValue = standardMax
    }

    fun calculateAverageValue(storedValues: IntArray): Int {
        return storedValues.sum() / storedValues.count()
    }

    fun calculatePercentile(observationCount: Int, percentile: Int): Int {
        return (percentile * (observationCount + 1)) / 100
    }

    fun calculateMedianValue(storedValues: IntArray): Int {
        val n = storedValues.count()
        var median = 0
        if (n == 1) {
            return storedValues[0]
        }
        median = if ((n % 2) != 0) {
            storedValues[n / 2]
        } else {
            (storedValues[(n - 1) / 2] + storedValues[(n + 1) / 2]) / 2
        }
        return median
    }

    fun trimStoredValues(storedValues: ArrayList<Int>): IntArray {
        val trimCount = 2
        val storedValueSorted = storedValues.sorted()
        if (storedValueSorted.count() > 10) {
            for (i in 0..trimCount) {
                storedValueSorted.toMutableList().apply { removeAt(0) }
                storedValueSorted.toMutableList().apply { storedValueSorted.count() - 1 }
            }
        }
        return storedValueSorted.toIntArray()
    }
}

