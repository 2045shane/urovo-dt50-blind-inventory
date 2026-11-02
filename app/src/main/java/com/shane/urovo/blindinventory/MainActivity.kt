package com.shane.urovo.blindinventory

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shane.urovo.blindinventory.export.CsvShare
import com.shane.urovo.blindinventory.ui.BlindInventoryScreen
import com.shane.urovo.blindinventory.ui.BlindInventoryViewModel
import com.shane.urovo.blindinventory.ui.IndustrialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            IndustrialTheme {
                val viewModel: BlindInventoryViewModel = viewModel(
                    factory = BlindInventoryViewModel.factory(),
                )
                val state by viewModel.state.collectAsState()
                BlindInventoryScreen(
                    state = state,
                    onToggleScan = viewModel::toggleScan,
                    onSimulate = viewModel::simulateOne,
                    onClear = viewModel::clear,
                    onExport = {
                        val payload = viewModel.buildExport()
                        if (payload == null) {
                            Toast.makeText(
                                this,
                                getString(R.string.export_empty),
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            CsvShare.share(this, payload)
                        }
                    },
                )
            }
        }
    }
}
