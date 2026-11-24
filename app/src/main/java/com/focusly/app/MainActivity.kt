package com.focusly.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusly.app.core.designsystem.theme.FocuslyTheme
import com.focusly.app.navigation.FocuslyNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val preferences by mainViewModel.preferences.collectAsStateWithLifecycle()
            val studentName by mainViewModel.studentName.collectAsStateWithLifecycle()

            FocuslyTheme {
                FocuslyNavHost(
                    onboardingCompleted = preferences.onboardingCompleted,
                    studentName = studentName,
                    onSaveStudentName = mainViewModel::saveStudentName,
                    onLanguageChanged = { languageTag ->
                        val locales = LocaleListCompat.forLanguageTags(languageTag)
                        AppCompatDelegate.setApplicationLocales(locales)
                    }
                )
            }
        }
    }
}
