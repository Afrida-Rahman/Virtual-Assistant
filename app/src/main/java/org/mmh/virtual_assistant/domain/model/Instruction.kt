package org.mmh.virtual_assistant.domain.model

import android.media.MediaPlayer

data class Instruction(
    val text: String,
    val player: MediaPlayer?
)
