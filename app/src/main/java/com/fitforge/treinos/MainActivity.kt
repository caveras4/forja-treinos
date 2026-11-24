package com.fitforge.treinos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitforge.treinos.ui.home.HomeViewModel
import com.fitforge.treinos.ui.navigation.AppNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as TreinosApplication).workoutRepository

        setContent {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(repository)
            )
            AppNavGraph(homeViewModel = homeViewModel)
        }
    }
}
