import { useState } from 'react';
import { Sparkles } from 'lucide-react';
import type { FocuslyStrings } from '../i18n/translations';
import type {
  AnalyticsPeriod,
  AppLanguage,
  FocusRecommendation,
  FocusSessionRecord,
  StudyTaskItem,
  SubjectItem,
} from '../types/focusly';
import { calculateProgressAnalytics } from '../utils/studyAnalytics';
import { getLocalDateString } from '../utils/timerEngine';

interface ProgressAnalyticsScreenProps {
  strings: FocuslyStrings;
  language: AppLanguage;
  sessions: FocusSessionRecord[];
  subjects: SubjectItem[];
  tasks: StudyTaskItem[];
  recommendation: FocusRecommendation;
  onStartRecommendedFocus: (minutes: number, recId: string) => void;
  onIgnoreRecommendation: (recId: string) => void;
}

export function ProgressAnalyticsScreen({
  strings,
  language,
  sessions,
  subjects,
  tasks,
  recommendation,
  onStartRecommendedFocus,
  onIgnoreRecommendation,
}: ProgressAnalyticsScreenProps) {
  const [period, setPeriod] = useState<AnalyticsPeriod>('WEEK');
  const todayDate = getLocalDateString();

  const summary = calculateProgressAnalytics(
    sessions,
    subjects,
    period,
    todayDate,
    language,
    strings.phase4.generalStudy,
  );

  const completedTasksCount = tasks.filter((t) => t.status === 'COMPLETED').length;
  const maxDayMinutes = Math.max(1, ...summary.dailyBreakdown.map((d) => d.focusMinutes));
  const chartBars =
    period === 'WEEK' ? summary.dailyBreakdown : summary.dailyBreakdown.slice(-14);

  return (
    <div className="space-y-6">
      {/* Header + Period Segmented Toggle */}
      <header className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl sm:text-3xl font-bold text-white">
            {strings.phase4.progressTitle}
          </h1>
          <p className="text-sm sm:text-base text-[#A7B4C8]">
            {strings.phase4.progressSubtitle}
          </p>
        </div>

        <div className="flex items-center gap-1.5 p-1.5 rounded-xl bg-[#101B32] border border-[#233554] w-fit">
          {([
            { id: 'WEEK', label: strings.phase4.periodWeek },
            { id: 'MONTH', label: strings.phase4.periodMonth },
          ] as { id: AnalyticsPeriod; label: string }[]).map((opt) => (
            <button
              key={opt.id}
              type="button"
              onClick={() => setPeriod(opt.id)}
              className={`min-h-[40px] px-4 py-1.5 rounded-lg text-sm font-semibold transition-colors whitespace-nowrap cursor-pointer ${
                period === opt.id
                  ? 'bg-[#3B82F6] text-white'
                  : 'text-[#A7B4C8] hover:text-white'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </header>

      {/* 4 KPI Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="rounded-2xl bg-[#16243D] border border-[#233554] p-5 space-y-1.5">
          <div className="text-xs text-[#A7B4C8]">{strings.phase4.totalFocus}</div>
          <div className="text-2xl font-bold text-[#3B82F6] font-mono-tabular">
            {summary.totalFocusMinutes} {strings.home.minutesUnit}
          </div>
        </div>
        <div className="rounded-2xl bg-[#16243D] border border-[#233554] p-5 space-y-1.5">
          <div className="text-xs text-[#A7B4C8]">{strings.phase4.completionRate}</div>
          <div className="text-2xl font-bold text-[#22C55E] font-mono-tabular">
            {Math.round(summary.completionRatePercent)}%
          </div>
        </div>
        <div className="rounded-2xl bg-[#16243D] border border-[#233554] p-5 space-y-1.5">
          <div className="text-xs text-[#A7B4C8]">{strings.phase4.avgSession}</div>
          <div className="text-2xl font-bold text-[#22D3EE] font-mono-tabular">
            {summary.averageSessionMinutes} {strings.home.minutesUnit}
          </div>
        </div>
        <div className="rounded-2xl bg-[#16243D] border border-[#233554] p-5 space-y-1.5">
          <div className="text-xs text-[#A7B4C8]">{strings.phase4.tasksDone}</div>
          <div className="text-2xl font-bold text-white font-mono-tabular">
            {completedTasksCount}/{tasks.length}
          </div>
        </div>
      </div>

      {/* Activity Chart + Subject Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
        <section className="lg:col-span-7 rounded-2xl bg-[#16243D] border border-[#233554] p-6 flex flex-col justify-between space-y-5">
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-base font-semibold text-white">
              {strings.phase4.activityTitle}
            </h2>
            <span className="text-xs font-medium text-[#22D3EE] font-mono-tabular">
              {strings.phase4.activeDays(
                summary.activeDaysCount,
                period === 'WEEK' ? 7 : 30,
              )}
            </span>
          </div>

          {summary.totalFocusMinutes === 0 && summary.completedSessions === 0 ? (
            <p className="text-sm text-[#A7B4C8] py-6">{strings.phase4.noActivity}</p>
          ) : (
            <div
              className="grid gap-2 items-end pt-2"
              style={{
                gridTemplateColumns: `repeat(${chartBars.length}, minmax(0, 1fr))`,
              }}
            >
              {chartBars.map((day) => {
                const ratio = day.focusMinutes / maxDayMinutes;
                const barHeightPx = Math.round(12 + ratio * 96);
                return (
                  <div key={day.date} className="flex flex-col items-center gap-2 min-w-0">
                    <span className="text-[11px] text-[#A7B4C8] font-mono-tabular truncate">
                      {day.focusMinutes > 0 ? `${day.focusMinutes}m` : '0'}
                    </span>
                    <div className="w-full max-w-[26px] h-[108px] flex items-end justify-center">
                      <div
                        className={`w-full rounded-md transition-all duration-200 ${
                          day.focusMinutes > 0 ? 'bg-[#3B82F6]' : 'bg-[#101B32]'
                        }`}
                        style={{ height: `${barHeightPx}px` }}
                      />
                    </div>
                    <span className="text-[11px] font-medium text-[#A7B4C8] truncate">
                      {day.dayShortLabel}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </section>

        {/* Subject Breakdown */}
        <section className="lg:col-span-5 rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-4">
          <h2 className="text-base font-semibold text-white">
            {strings.phase4.subjectBreakdownTitle}
          </h2>

          {summary.subjectBreakdown.length === 0 ? (
            <p className="text-sm text-[#A7B4C8] py-4">{strings.phase4.noSubjectData}</p>
          ) : (
            <div className="space-y-4">
              {summary.subjectBreakdown.map((item) => (
                <div key={item.subjectId ?? 'general'} className="space-y-1.5">
                  <div className="flex items-center justify-between gap-3 text-sm">
                    <div className="flex items-center gap-2 min-w-0">
                      <span
                        className="w-2.5 h-2.5 rounded-full shrink-0"
                        style={{ backgroundColor: item.subjectColor }}
                      />
                      <span className="text-white font-medium truncate">
                        {item.subjectName}
                      </span>
                    </div>
                    <span className="text-xs text-[#A7B4C8] font-mono-tabular shrink-0">
                      {item.focusMinutes} {strings.home.minutesUnit} · {Math.round(item.sharePercentage)}%
                    </span>
                  </div>
                  <div className="w-full h-2 rounded-full bg-[#101B32] overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all duration-200"
                      style={{
                        width: `${Math.min(100, Math.max(4, item.sharePercentage))}%`,
                        backgroundColor: item.subjectColor,
                      }}
                    />
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>

      {/* Adaptive Focus Engine Insights */}
      {recommendation.status !== 'IGNORED' && (
        <section className="rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-4">
          <div className="space-y-2">
            <div className="flex items-center gap-2.5">
              <Sparkles className="w-5 h-5 text-[#22D3EE] shrink-0" />
              <h2 className="text-base font-semibold text-white">
                {recommendation.hasEnoughData
                  ? strings.phase3.adaptiveTitle
                  : strings.phase3.adaptiveInsufficientTitle}
              </h2>
            </div>
            <p className="text-sm text-[#A7B4C8] leading-relaxed">
              {recommendation.hasEnoughData
                ? recommendation.reasonText
                : strings.phase3.adaptiveInsufficientDesc}
            </p>
            {recommendation.hasEnoughData && (
              <p className="text-xs text-[#22D3EE]">
                {strings.phase3.adaptiveRuleMeta(recommendation.basedOnSessionCount)}
              </p>
            )}
          </div>

          {recommendation.hasEnoughData && (
            <div className="flex flex-wrap items-center gap-3">
              <button
                type="button"
                onClick={() =>
                  onStartRecommendedFocus(recommendation.recommendedMinutes, recommendation.id)
                }
                className="min-h-[44px] px-5 py-2.5 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white text-sm font-semibold whitespace-nowrap cursor-pointer"
              >
                {strings.phase3.adaptiveStartRecommended(recommendation.recommendedMinutes)}
              </button>
              <button
                type="button"
                onClick={() => onIgnoreRecommendation(recommendation.id)}
                className="min-h-[44px] px-4 py-2 text-sm font-medium text-[#A7B4C8] hover:text-white cursor-pointer"
              >
                {strings.phase3.adaptiveIgnore}
              </button>
            </div>
          )}
        </section>
      )}
    </div>
  );
}
