package com.todoapp.mobile.ui.creationhub

import com.todoapp.mobile.R

/**
 * Coordinated example placeholders for the create flow. The title and description at the same index form
 * a themed pair (picked via [CreationHubContract.UiState.placeholderIndex], re-rolled on type select),
 * so the two fields always read as a matching example. Step examples cycle by row index.
 */
internal object CreationHubPlaceholders {
    private val titles = listOf(
        R.string.creation_title_ph_1,
        R.string.creation_title_ph_2,
        R.string.creation_title_ph_3,
        R.string.creation_title_ph_4,
    )
    private val descriptions = listOf(
        R.string.creation_desc_ph_1,
        R.string.creation_desc_ph_2,
        R.string.creation_desc_ph_3,
        R.string.creation_desc_ph_4,
    )
    private val steps = listOf(
        R.string.creation_step_ph_1,
        R.string.creation_step_ph_2,
        R.string.creation_step_ph_3,
        R.string.creation_step_ph_4,
    )

    val count: Int = titles.size

    fun titleRes(index: Int): Int = titles[index.mod(titles.size)]

    fun descriptionRes(index: Int): Int = descriptions[index.mod(descriptions.size)]

    fun stepRes(rowIndex: Int): Int = steps[rowIndex.mod(steps.size)]
}
