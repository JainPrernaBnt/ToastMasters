package com.bntsoft.toastmasters.presentation.ui.vp.roles.model

import com.bntsoft.toastmasters.domain.model.Role

data class MemberRoleItem(
    val id: String,
    val name: String,
    val preferredRoles: List<Role>,
    val pastRoles: List<Role>,
    val availableRoles: List<Role>
)
