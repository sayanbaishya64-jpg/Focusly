import { useState } from 'react';
import { MainShell } from './components/MainShell';
import { OnboardingFlow } from './components/OnboardingFlow';
import { SplashScreen } from './components/SplashScreen';
import { STRINGS } from './i18n/translations';
import {
  clearPreferences,
  DEFAULT_PREFERENCES,
  loadPreferences,
  savePreferences,
  type AppLanguage,
  type OnboardingPreferences,
} from './types/focusly';

export default function App() {
  const [preferences, setPreferences] = useState<OnboardingPreferences>(() => loadPreferences());
  const [splashDone, setSplashDone] = useState(false);

  const strings = STRINGS[preferences.language];

  const handleUpdatePreferences = (next: OnboardingPreferences) => {
    setPreferences(next);
    savePreferences(next);
  };

  const handleFinishOnboarding = (finalPrefs: OnboardingPreferences) => {
    setPreferences(finalPrefs);
    savePreferences(finalPrefs);
  };

  const handleLanguageChange = (language: AppLanguage) => {
    const next = { ...preferences, language };
    setPreferences(next);
    savePreferences(next);
  };

  const handleResetOnboarding = () => {
    clearPreferences();
    setPreferences(DEFAULT_PREFERENCES);
  };

  const handleRestorePreferences = (restored: OnboardingPreferences) => {
    setPreferences(restored);
    savePreferences(restored);
  };

  if (!splashDone) {
    return <SplashScreen strings={strings} onComplete={() => setSplashDone(true)} />;
  }

  if (!preferences.onboardingCompleted) {
    return (
      <OnboardingFlow
        preferences={preferences}
        strings={strings}
        onUpdate={handleUpdatePreferences}
        onFinish={handleFinishOnboarding}
      />
    );
  }

  return (
    <MainShell
      preferences={preferences}
      strings={strings}
      onLanguageChange={handleLanguageChange}
      onRestorePreferences={handleRestorePreferences}
      onResetOnboarding={handleResetOnboarding}
    />
  );
}
