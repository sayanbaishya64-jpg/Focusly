import { useState } from 'react';
import {
  ArrowLeft,
  Backpack,
  Bell,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Circle,
  Clock,
  Compass,
  Focus,
  GraduationCap,
  Languages,
  ShieldCheck,
  Smartphone,
  SlidersHorizontal,
  Target,
  VolumeX,
} from 'lucide-react';
import { FocuslyBrandMark } from './FocuslyBrandMark';
import {
  DISTRACTION_CANDIDATES,
  type AppLanguage,
  type OnboardingPreferences,
  type StudentCategory,
  type StudyGoal,
} from '../types/focusly';
import type { FocuslyStrings } from '../i18n/translations';

interface OnboardingFlowProps {
  preferences: OnboardingPreferences;
  strings: FocuslyStrings;
  onUpdate: (next: OnboardingPreferences) => void;
  onFinish: (finalPrefs: OnboardingPreferences) => void;
}

const TOTAL_FORM_STEPS = 5;

export function OnboardingFlow({
  preferences,
  strings,
  onUpdate,
  onFinish,
}: OnboardingFlowProps) {
  const [step, setStep] = useState<number>(0); // 0 = Welcome, 1..5 = Category, Goals, Target, Language, Distraction
  const [customMode, setCustomMode] = useState<boolean>(
    ![30, 60, 120, 180].includes(preferences.dailyFocusTargetMinutes),
  );
  const [customInput, setCustomInput] = useState<string>(
    ![30, 60, 120, 180].includes(preferences.dailyFocusTargetMinutes)
      ? String(preferences.dailyFocusTargetMinutes)
      : '',
  );

  const parsedCustom = Number.parseInt(customInput, 10);
  const isCustomValid = !Number.isNaN(parsedCustom) && parsedCustom >= 10 && parsedCustom <= 480;

  const selectCategory = (category: StudentCategory) => {
    onUpdate({ ...preferences, studentCategory: category });
  };

  const toggleGoal = (goal: StudyGoal) => {
    const exists = preferences.studyGoals.includes(goal);
    if (exists && preferences.studyGoals.length === 1) {
      return;
    }
    const nextGoals = exists
      ? preferences.studyGoals.filter((g) => g !== goal)
      : [...preferences.studyGoals, goal];
    onUpdate({ ...preferences, studyGoals: nextGoals });
  };

  const selectPresetMinutes = (minutes: number) => {
    setCustomMode(false);
    onUpdate({ ...preferences, dailyFocusTargetMinutes: minutes });
  };

  const handleCustomMinutesChange = (val: string) => {
    const digits = val.replace(/\D/g, '').slice(0, 3);
    setCustomMode(true);
    setCustomInput(digits);
    const num = Number.parseInt(digits, 10);
    if (!Number.isNaN(num) && num >= 10 && num <= 480) {
      onUpdate({ ...preferences, dailyFocusTargetMinutes: num });
    }
  };

  const selectLanguage = (language: AppLanguage) => {
    onUpdate({ ...preferences, language });
  };

  const toggleDistractionApp = (packageName: string) => {
    const exists = preferences.selectedDistractionApps.includes(packageName);
    const nextApps = exists
      ? preferences.selectedDistractionApps.filter((p) => p !== packageName)
      : [...preferences.selectedDistractionApps, packageName];
    onUpdate({
      ...preferences,
      selectedDistractionApps: nextApps,
      distractionPreferencesSkipped: false,
    });
  };

  const completeSetup = (skipped: boolean) => {
    const finalPrefs: OnboardingPreferences = {
      ...preferences,
      selectedDistractionApps: skipped ? [] : preferences.selectedDistractionApps,
      distractionPreferencesSkipped: skipped,
      onboardingCompleted: true,
    };
    onFinish(finalPrefs);
  };

  if (step === 0) {
    return (
      <main className="min-h-screen w-full bg-[#080F1F] flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-4xl grid grid-cols-1 lg:grid-cols-12 gap-10 items-center">
          <div className="lg:col-span-7 space-y-6">
            <div className="flex items-center gap-4">
              <FocuslyBrandMark size={60} />
              <div>
                <p className="text-sm font-semibold tracking-[0.14em] text-[#22D3EE]">
                  {strings.appName}
                </p>
                <p className="text-sm text-[#A7B4C8]">{strings.tagline}</p>
              </div>
            </div>

            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-bold tracking-tight text-white text-balance leading-tight">
              {strings.welcomeTitle}
            </h1>

            <p className="text-base sm:text-lg text-[#A7B4C8] leading-relaxed max-w-xl">
              {strings.welcomeSubtitle}
            </p>

            <div className="pt-2">
              <button
                type="button"
                onClick={() => setStep(1)}
                className="w-full sm:w-auto min-h-[52px] px-8 py-3.5 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] active:scale-[0.99] text-white font-semibold text-base transition-all duration-150 whitespace-nowrap shrink-0 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#22D3EE]"
              >
                {strings.getStarted}
              </button>
            </div>
          </div>

          <div className="lg:col-span-5">
            <div className="rounded-2xl bg-[#101B32] border border-[#233554] p-6 sm:p-7 space-y-5">
              <h2 className="text-sm font-semibold text-[#22D3EE]">
                {strings.appName}
              </h2>
              <ul className="space-y-4">
                {strings.welcomeHighlights.map((item) => (
                  <li key={item} className="flex items-start gap-3 text-sm sm:text-base text-[#A7B4C8]">
                    <CheckCircle2 className="w-5 h-5 text-[#22C55E] shrink-0 mt-0.5" />
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      </main>
    );
  }

  const categoryCards: { id: StudentCategory; icon: typeof GraduationCap }[] = [
    { id: 'school_student', icon: Backpack },
    { id: 'college_student', icon: GraduationCap },
    { id: 'competitive_aspirant', icon: BookOpen },
    { id: 'self_learner', icon: Compass },
  ];

  const goalCards: { id: StudyGoal; icon: typeof Focus }[] = [
    { id: 'improve_concentration', icon: Focus },
    { id: 'reduce_distractions', icon: VolumeX },
    { id: 'build_consistency', icon: CalendarDays },
    { id: 'complete_daily_goals', icon: Target },
  ];

  const targetPresets = [30, 60, 120, 180];

  const progressPercent = Math.round((step / TOTAL_FORM_STEPS) * 100);

  return (
    <main className="min-h-screen w-full bg-[#080F1F] flex flex-col items-center px-5 sm:px-8 py-6 sm:py-10">
      <div className="w-full max-w-2xl flex-1 flex flex-col justify-between gap-8">
        <div className="space-y-7">
          {/* Header & Step Progress */}
          <header className="space-y-3">
            <div className="flex items-center justify-between gap-4">
              <button
                type="button"
                onClick={() => setStep((s) => Math.max(0, s - 1))}
                aria-label={strings.backLabel}
                className="min-h-[44px] min-w-[44px] -ml-2 px-3 inline-flex items-center gap-2 rounded-xl text-sm font-medium text-[#A7B4C8] hover:text-white hover:bg-[#101B32] transition-colors duration-150 whitespace-nowrap shrink-0 cursor-pointer"
              >
                <ArrowLeft className="w-4 h-4" />
                <span>{strings.backLabel}</span>
              </button>
              <span className="text-xs sm:text-sm font-medium text-[#A7B4C8] font-mono-tabular whitespace-nowrap shrink-0">
                {strings.stepOf(step, TOTAL_FORM_STEPS)}
              </span>
            </div>
            <div className="w-full h-1.5 rounded-full bg-[#16243D] overflow-hidden">
              <div
                className="h-full bg-[#3B82F6] transition-all duration-200 rounded-full"
                style={{ width: `${progressPercent}%` }}
              />
            </div>
          </header>

          {/* Step 1: Student Category */}
          {step === 1 && (
            <section className="space-y-6">
              <div className="space-y-2">
                <h1 className="text-2xl sm:text-3xl font-bold text-white text-balance">
                  {strings.categoryTitle}
                </h1>
                <p className="text-sm sm:text-base text-[#A7B4C8]">
                  {strings.categorySubtitle}
                </p>
              </div>
              <div className="space-y-3">
                {categoryCards.map(({ id, icon: Icon }) => {
                  const selected = preferences.studentCategory === id;
                  const meta = strings.categories[id];
                  return (
                    <button
                      key={id}
                      type="button"
                      onClick={() => selectCategory(id)}
                      className={`w-full min-h-[68px] p-4 sm:p-5 rounded-2xl border text-left flex items-center justify-between gap-4 transition-colors duration-150 cursor-pointer ${
                        selected
                          ? 'bg-[#3B82F6]/15 border-[#3B82F6]'
                          : 'bg-[#16243D] border-[#233554] hover:border-[#3B82F6]/50'
                      }`}
                    >
                      <div className="flex items-start gap-3.5">
                        <Icon
                          className={`w-5 h-5 mt-0.5 shrink-0 ${
                            selected ? 'text-[#22D3EE]' : 'text-[#A7B4C8]'
                          }`}
                        />
                        <div>
                          <div className="text-base font-semibold text-white">
                            {meta.title}
                          </div>
                          <div className="text-sm text-[#A7B4C8] mt-1">
                            {meta.desc}
                          </div>
                        </div>
                      </div>
                      {selected ? (
                        <CheckCircle2 className="w-5 h-5 text-[#3B82F6] shrink-0" />
                      ) : (
                        <Circle className="w-5 h-5 text-[#A7B4C8]/50 shrink-0" />
                      )}
                    </button>
                  );
                })}
              </div>
            </section>
          )}

          {/* Step 2: Study Goals */}
          {step === 2 && (
            <section className="space-y-6">
              <div className="space-y-2">
                <h1 className="text-2xl sm:text-3xl font-bold text-white text-balance">
                  {strings.goalsTitle}
                </h1>
                <p className="text-sm sm:text-base text-[#A7B4C8]">
                  {strings.goalsSubtitle}
                </p>
              </div>
              <div className="space-y-3">
                {goalCards.map(({ id, icon: Icon }) => {
                  const selected = preferences.studyGoals.includes(id);
                  return (
                    <button
                      key={id}
                      type="button"
                      onClick={() => toggleGoal(id)}
                      className={`w-full min-h-[60px] p-4 sm:p-5 rounded-2xl border text-left flex items-center justify-between gap-4 transition-colors duration-150 cursor-pointer ${
                        selected
                          ? 'bg-[#3B82F6]/15 border-[#3B82F6]'
                          : 'bg-[#16243D] border-[#233554] hover:border-[#3B82F6]/50'
                      }`}
                    >
                      <div className="flex items-center gap-3.5">
                        <Icon
                          className={`w-5 h-5 shrink-0 ${
                            selected ? 'text-[#22D3EE]' : 'text-[#A7B4C8]'
                          }`}
                        />
                        <span className="text-base font-semibold text-white">
                          {strings.goals[id]}
                        </span>
                      </div>
                      {selected ? (
                        <CheckCircle2 className="w-5 h-5 text-[#3B82F6] shrink-0" />
                      ) : (
                        <Circle className="w-5 h-5 text-[#A7B4C8]/50 shrink-0" />
                      )}
                    </button>
                  );
                })}
              </div>
            </section>
          )}

          {/* Step 3: Daily Focus Target */}
          {step === 3 && (
            <section className="space-y-6">
              <div className="space-y-2">
                <h1 className="text-2xl sm:text-3xl font-bold text-white text-balance">
                  {strings.targetTitle}
                </h1>
                <p className="text-sm sm:text-base text-[#A7B4C8]">
                  {strings.targetSubtitle}
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {targetPresets.map((minutes) => {
                  const selected = !customMode && preferences.dailyFocusTargetMinutes === minutes;
                  return (
                    <button
                      key={minutes}
                      type="button"
                      onClick={() => selectPresetMinutes(minutes)}
                      className={`min-h-[64px] p-4 rounded-2xl border text-left flex items-center justify-between gap-3 transition-colors duration-150 cursor-pointer ${
                        selected
                          ? 'bg-[#3B82F6]/15 border-[#3B82F6]'
                          : 'bg-[#16243D] border-[#233554] hover:border-[#3B82F6]/50'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <Clock
                          className={`w-5 h-5 shrink-0 ${
                            selected ? 'text-[#22D3EE]' : 'text-[#A7B4C8]'
                          }`}
                        />
                        <span className="text-base font-semibold text-white font-mono-tabular">
                          {strings.minutesLabel(minutes)}
                        </span>
                      </div>
                      {selected ? (
                        <CheckCircle2 className="w-5 h-5 text-[#3B82F6] shrink-0" />
                      ) : (
                        <Circle className="w-5 h-5 text-[#A7B4C8]/50 shrink-0" />
                      )}
                    </button>
                  );
                })}
              </div>

              <div className="space-y-3">
                <button
                  type="button"
                  onClick={() => {
                    setCustomMode(true);
                    if (!customInput) setCustomInput('90');
                  }}
                  className={`w-full min-h-[60px] p-4 rounded-2xl border text-left flex items-center justify-between gap-3 transition-colors duration-150 cursor-pointer ${
                    customMode
                      ? 'bg-[#3B82F6]/15 border-[#3B82F6]'
                      : 'bg-[#16243D] border-[#233554] hover:border-[#3B82F6]/50'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <SlidersHorizontal
                      className={`w-5 h-5 shrink-0 ${
                        customMode ? 'text-[#22D3EE]' : 'text-[#A7B4C8]'
                      }`}
                    />
                    <span className="text-base font-semibold text-white">
                      {strings.customDuration}
                    </span>
                  </div>
                  {customMode ? (
                    <CheckCircle2 className="w-5 h-5 text-[#3B82F6] shrink-0" />
                  ) : (
                    <Circle className="w-5 h-5 text-[#A7B4C8]/50 shrink-0" />
                  )}
                </button>

                {customMode && (
                  <div className="space-y-2">
                    <label htmlFor="custom-minutes" className="block text-sm text-[#A7B4C8]">
                      {strings.customDurationHint}
                    </label>
                    <input
                      id="custom-minutes"
                      type="text"
                      inputMode="numeric"
                      value={customInput}
                      onChange={(e) => handleCustomMinutesChange(e.target.value)}
                      placeholder="90"
                      className="w-full min-h-[52px] px-4 rounded-xl bg-[#16243D] border border-[#233554] focus:border-[#3B82F6] focus:outline-none text-white text-base font-mono-tabular"
                    />
                    {!isCustomValid && (
                      <p className="text-sm text-[#F59E0B]">{strings.customDurationError}</p>
                    )}
                  </div>
                )}
              </div>
            </section>
          )}

          {/* Step 4: Language */}
          {step === 4 && (
            <section className="space-y-6">
              <div className="space-y-2">
                <h1 className="text-2xl sm:text-3xl font-bold text-white text-balance">
                  {strings.languageTitle}
                </h1>
                <p className="text-sm sm:text-base text-[#A7B4C8]">
                  {strings.languageSubtitle}
                </p>
              </div>
              <div className="space-y-3">
                {[
                  {
                    lang: 'en' as AppLanguage,
                    title: strings.englishLabel,
                    desc: strings.englishDesc,
                  },
                  {
                    lang: 'bn' as AppLanguage,
                    title: strings.bengaliLabel,
                    desc: strings.bengaliDesc,
                  },
                ].map((item) => {
                  const selected = preferences.language === item.lang;
                  return (
                    <button
                      key={item.lang}
                      type="button"
                      onClick={() => selectLanguage(item.lang)}
                      className={`w-full min-h-[72px] p-4 sm:p-5 rounded-2xl border text-left flex items-center justify-between gap-4 transition-colors duration-150 cursor-pointer ${
                        selected
                          ? 'bg-[#3B82F6]/15 border-[#3B82F6]'
                          : 'bg-[#16243D] border-[#233554] hover:border-[#3B82F6]/50'
                      }`}
                    >
                      <div className="flex items-start gap-3.5">
                        <Languages
                          className={`w-5 h-5 mt-0.5 shrink-0 ${
                            selected ? 'text-[#22D3EE]' : 'text-[#A7B4C8]'
                          }`}
                        />
                        <div>
                          <div className="text-base font-semibold text-white">
                            {item.title}
                          </div>
                          <div className="text-sm text-[#A7B4C8] mt-1">{item.desc}</div>
                        </div>
                      </div>
                      {selected ? (
                        <CheckCircle2 className="w-5 h-5 text-[#3B82F6] shrink-0" />
                      ) : (
                        <Circle className="w-5 h-5 text-[#A7B4C8]/50 shrink-0" />
                      )}
                    </button>
                  );
                })}
              </div>
            </section>
          )}

          {/* Step 5: Distraction Preferences & Permission Explanation */}
          {step === 5 && (
            <section className="space-y-6">
              <div className="space-y-2">
                <h1 className="text-2xl sm:text-3xl font-bold text-white text-balance">
                  {strings.distractionTitle}
                </h1>
                <p className="text-sm sm:text-base text-[#A7B4C8]">
                  {strings.distractionSubtitle}
                </p>
              </div>

              <div className="rounded-2xl bg-[#101B32] border border-[#233554] p-5 space-y-4">
                <div className="flex items-start gap-3">
                  <ShieldCheck className="w-5 h-5 text-[#22D3EE] shrink-0 mt-0.5" />
                  <div>
                    <h2 className="text-sm font-semibold text-white">
                      {strings.usagePermissionTitle}
                    </h2>
                    <p className="text-sm text-[#A7B4C8] mt-1 leading-relaxed">
                      {strings.usagePermissionDesc}
                    </p>
                  </div>
                </div>
                <div className="h-px bg-[#233554]" />
                <div className="flex items-start gap-3">
                  <Bell className="w-5 h-5 text-[#22D3EE] shrink-0 mt-0.5" />
                  <div>
                    <h2 className="text-sm font-semibold text-white">
                      {strings.notifPermissionTitle}
                    </h2>
                    <p className="text-sm text-[#A7B4C8] mt-1 leading-relaxed">
                      {strings.notifPermissionDesc}
                    </p>
                  </div>
                </div>
              </div>

              <div className="space-y-3">
                <h3 className="text-sm font-medium text-[#A7B4C8]">
                  {strings.optionalAppsHeading}
                </h3>
                <div className="space-y-2.5">
                  {DISTRACTION_CANDIDATES.map((app) => {
                    const selected = preferences.selectedDistractionApps.includes(app.packageName);
                    return (
                      <button
                        key={app.packageName}
                        type="button"
                        onClick={() => toggleDistractionApp(app.packageName)}
                        className={`w-full min-h-[60px] p-4 rounded-2xl border text-left flex items-center justify-between gap-4 transition-colors duration-150 cursor-pointer ${
                          selected
                            ? 'bg-[#3B82F6]/15 border-[#3B82F6]'
                            : 'bg-[#16243D] border-[#233554] hover:border-[#3B82F6]/50'
                        }`}
                      >
                        <div className="flex items-center gap-3.5 min-w-0">
                          <Smartphone
                            className={`w-5 h-5 shrink-0 ${
                              selected ? 'text-[#22D3EE]' : 'text-[#A7B4C8]'
                            }`}
                          />
                          <div className="min-w-0">
                            <div className="text-base font-semibold text-white truncate">
                              {app.label}
                            </div>
                            <div className="text-xs text-[#A7B4C8] truncate">
                              {app.category} · {app.packageName}
                            </div>
                          </div>
                        </div>
                        {selected ? (
                          <CheckCircle2 className="w-5 h-5 text-[#3B82F6] shrink-0" />
                        ) : (
                          <Circle className="w-5 h-5 text-[#A7B4C8]/50 shrink-0" />
                        )}
                      </button>
                    );
                  })}
                </div>
              </div>
            </section>
          )}
        </div>

        {/* Bottom CTA */}
        <footer className="pt-2 space-y-3">
          {step < 5 ? (
            <button
              type="button"
              disabled={step === 3 && customMode && !isCustomValid}
              onClick={() => setStep((s) => s + 1)}
              className="w-full min-h-[52px] px-6 py-3.5 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] disabled:bg-[#16243D] disabled:text-[#A7B4C8]/50 text-white font-semibold text-base transition-colors duration-150 whitespace-nowrap shrink-0 cursor-pointer disabled:cursor-not-allowed"
            >
              {strings.continueLabel}
            </button>
          ) : (
            <div className="space-y-2.5">
              <button
                type="button"
                onClick={() => completeSetup(false)}
                className="w-full min-h-[52px] px-6 py-3.5 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white font-semibold text-base transition-colors duration-150 whitespace-nowrap shrink-0 cursor-pointer"
              >
                {strings.finishSetup}
              </button>
              <button
                type="button"
                onClick={() => completeSetup(true)}
                className="w-full min-h-[44px] px-6 py-2.5 rounded-xl text-[#A7B4C8] hover:text-white hover:bg-[#101B32] text-sm font-medium transition-colors duration-150 whitespace-nowrap shrink-0 cursor-pointer"
              >
                {strings.skipLabel}
              </button>
            </div>
          )}
        </footer>
      </div>
    </main>
  );
}
