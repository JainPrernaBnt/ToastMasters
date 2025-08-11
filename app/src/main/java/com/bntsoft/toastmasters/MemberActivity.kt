package com.bntsoft.toastmasters

import android.os.Bundle
import com.bntsoft.toastmasters.databinding.ActivityMemberMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MemberActivity : BaseActivity() {
    private lateinit var binding: ActivityMemberMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}
