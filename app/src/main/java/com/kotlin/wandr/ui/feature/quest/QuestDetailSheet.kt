package com.kotlin.wandr.ui.feature.quest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlin.wandr.ui.theme.WandrTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestDetailSheet(
    questId: String,
    onDismiss: () -> Unit,
    onOpenTracker: (questId: String) -> Unit,
    viewModel: QuestDetailViewModel = hiltViewModel(key = questId),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(questId) { viewModel.load(questId) }
    LaunchedEffect(state.startedQuestId) {
        state.startedQuestId?.let {
            viewModel.onNavigatedToTracker()
            onDismiss()
            onOpenTracker(it)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxHeight(0.9f).navigationBarsPadding()) {
            QuestDetailBody(state = state, modifier = Modifier.weight(1f))
            if (state.detail != null) {
                state.errorMessage?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = WandrTheme.spacing.screen),
                    )
                }
                QuestDetailAction(
                    state = state,
                    onAccept = viewModel::startQuest,
                    onContinue = {
                        onDismiss()
                        onOpenTracker(questId)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
