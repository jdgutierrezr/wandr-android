package com.kotlin.wandr.data.mapper

import com.kotlin.wandr.data.remote.dto.QuestDropoffDto
import com.kotlin.wandr.domain.model.DropoffPoint

fun QuestDropoffDto.toDomain() = DropoffPoint(
    questId = questId,
    questTitle = questTitle,
    totalSteps = totalSteps,
    lastStep = lastStep,
    lastStepTitle = lastStepTitle,
    abandonedCount = abandonedCount,
)
