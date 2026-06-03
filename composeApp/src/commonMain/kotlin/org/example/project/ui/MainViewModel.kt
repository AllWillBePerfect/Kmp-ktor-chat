package org.example.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.example.project.data.PreferencesDataSource
import org.example.project.data.room.MessageDao
import org.example.project.ktor.WsClientContract
import org.example.project.ktor.isConnected

class MainViewModel(
    private val wsClient: WsClientContract,
    private val pref: PreferencesDataSource,
) : ViewModel() {



    private var connectionJob: Job? = null

    fun onStart() {
        if (connectionJob?.isActive == true) return
        
        connectionJob = viewModelScope.launch {
            while (true) {
                val userData = pref.userDataFlow.first()
                
                if (userData.isAuthenticated) {
                    try {
                        println("MainViewModel: Attempting to connect...")
                        wsClient.connect()

                        while (wsClient.connectionState.value.isConnected) {
                            delay(2000)
                        }
                        println("MainViewModel: Connection lost")
                    } catch (e: Exception) {
                        println("MainViewModel: Connection failed: ${e.message}. Retrying in 5s...")
                    }
                } else {
                    println("MainViewModel: User not authenticated, skipping connection")
                }
                
                delay(5000) // Пауза перед следующей попыткой
            }
        }
    }

    fun onStop() {
        println("MainViewModel: onStop called, closing connection")
        connectionJob?.cancel()
        viewModelScope.launch {
            wsClient.close()
        }
    }
}
