import {
  ArrowLeft,
  CheckCircle2,
  Coins,
  Flame,
  Lock,
  Sprout,
  Trophy,
  X,
} from 'lucide-react';
import type { FocuslyStrings } from '../i18n/translations';
import type {
  GardenGrowthStage,
  RewardPurchaseResult,
  RewardsDashboardState,
} from '../types/focusly';

interface RewardsScreenProps {
  strings: FocuslyStrings;
  dashboard: RewardsDashboardState;
  purchaseMessage: RewardPurchaseResult | null;
  onBack: () => void;
  onPurchaseItem: (itemId: string) => void;
  onSetFeaturedItem: (itemId: string) => void;
  onDismissPurchaseMessage: () => void;
}

function PlantIllustration({
  stage,
  accentColor,
  size = 64,
}: {
  stage: GardenGrowthStage;
  accentColor: string;
  size?: number;
}) {
  const stemTop = stage === 'SEEDLING' ? 30 : stage === 'SPROUTING' ? 22 : 16;
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 64 64"
      fill="none"
      aria-hidden="true"
    >
      {/* Planter pot */}
      <rect x="16" y="46" width="32" height="14" rx="5" fill="#2B3E5E" />
      <rect x="14" y="44" width="36" height="5" rx="2.5" fill="#374E75" />

      {/* Stem */}
      <line
        x1="32"
        y1="44"
        x2="32"
        y2={stemTop}
        stroke={accentColor}
        strokeWidth="4"
        strokeLinecap="round"
      />

      {/* Leaves */}
      <path
        d="M32 34 C19 28, 17 38, 22 41 C26 43, 30 39, 32 34 Z"
        fill={accentColor}
        fillOpacity="0.85"
      />
      {stage !== 'SEEDLING' && (
        <path
          d="M32 31 C45 25, 47 35, 42 38 C38 40, 34 36, 32 31 Z"
          fill={accentColor}
        />
      )}

      {/* Stage Crown */}
      {stage === 'SEEDLING' && (
        <circle cx="32" cy={stemTop} r="4.5" fill={accentColor} fillOpacity="0.95" />
      )}
      {stage === 'SPROUTING' && (
        <>
          <circle cx="32" cy={stemTop} r="6.5" fill={accentColor} />
          <circle cx="32" cy={stemTop} r="2.8" fill="#FFFFFF" fillOpacity="0.65" />
        </>
      )}
      {stage === 'BLOOMING' && (
        <>
          <circle
            cx="32"
            cy={stemTop}
            r="12"
            stroke={accentColor}
            strokeOpacity="0.35"
            strokeWidth="3"
          />
          <circle cx="32" cy={stemTop} r="7.5" fill={accentColor} />
          <circle cx="32" cy={stemTop} r="3.5" fill="#F59E0B" />
        </>
      )}
    </svg>
  );
}

export function RewardsScreen({
  strings,
  dashboard,
  purchaseMessage,
  onBack,
  onPurchaseItem,
  onSetFeaturedItem,
  onDismissPurchaseMessage,
}: RewardsScreenProps) {
  const featuredItem =
    dashboard.storeItems.find((item) => item.isFeatured) ??
    dashboard.storeItems.find((item) => item.isUnlocked) ??
    dashboard.storeItems[0];

  const stage = featuredItem?.growthStage ?? 'SEEDLING';
  const stageLabel =
    stage === 'BLOOMING'
      ? strings.phase5.stageBlooming
      : stage === 'SPROUTING'
        ? strings.phase5.stageSprouting
        : strings.phase5.stageSeedling;

  const gardenHint =
    stage === 'BLOOMING'
      ? strings.phase5.gardenHintBlooming
      : stage === 'SPROUTING'
        ? strings.phase5.gardenHintSprouting
        : strings.phase5.gardenHintSeedling;

  const bannerConfig =
    purchaseMessage === 'SUCCESS'
      ? { text: strings.phase5.purchaseSuccess, color: 'text-[#22C55E] border-[#22C55E]/50' }
      : purchaseMessage === 'INSUFFICIENT_COINS'
        ? { text: strings.phase5.purchaseInsufficient, color: 'text-[#F59E0B] border-[#F59E0B]/50' }
        : purchaseMessage === 'ALREADY_UNLOCKED'
          ? { text: strings.phase5.purchaseAlready, color: 'text-[#22D3EE] border-[#22D3EE]/50' }
          : purchaseMessage === 'NOT_FOUND'
            ? { text: strings.phase5.purchaseNotFound, color: 'text-[#F59E0B] border-[#F59E0B]/50' }
            : null;

  return (
    <div className="space-y-6">
      <header className="flex items-start gap-3">
        <button
          type="button"
          onClick={onBack}
          aria-label={strings.backLabel}
          className="min-h-[44px] min-w-[44px] rounded-xl bg-[#101B32] border border-[#233554] hover:border-[#3B82F6] flex items-center justify-center text-[#A7B4C8] hover:text-white transition-colors shrink-0 cursor-pointer"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white">
            {strings.phase5.rewardsTitle}
          </h1>
          <p className="text-sm sm:text-base text-[#A7B4C8] mt-1">
            {strings.phase5.rewardsSubtitle}
          </p>
        </div>
      </header>

      {bannerConfig && (
        <div
          className={`rounded-xl bg-[#101B32] border px-4 py-3 flex items-center justify-between gap-3 ${bannerConfig.color}`}
        >
          <span className="text-sm font-medium">{bannerConfig.text}</span>
          <button
            type="button"
            onClick={onDismissPurchaseMessage}
            aria-label={strings.phase4.closeLabel}
            className="min-h-[36px] min-w-[36px] flex items-center justify-center text-[#A7B4C8] hover:text-white cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Coins + Streak + Virtual Garden Hero */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
        <div className="lg:col-span-5 grid grid-cols-2 gap-4">
          <div className="rounded-2xl bg-[#16243D] border border-[#233554] p-5 flex flex-col justify-between space-y-3">
            <div className="flex items-center gap-2 text-xs font-medium text-[#A7B4C8]">
              <Coins className="w-4 h-4 text-[#F59E0B]" />
              <span>{strings.dashboard.focusCoins}</span>
            </div>
            <div>
              <div className="text-3xl font-bold text-white font-mono-tabular">
                {dashboard.coinBalance}
              </div>
              <div className="text-xs text-[#A7B4C8] font-mono-tabular mt-1">
                {strings.phase5.lifetimeEarned(dashboard.totalEarnedCoins)}
              </div>
            </div>
          </div>

          <div className="rounded-2xl bg-[#16243D] border border-[#233554] p-5 flex flex-col justify-between space-y-3">
            <div className="flex items-center gap-2 text-xs font-medium text-[#A7B4C8]">
              <Flame className="w-4 h-4 text-[#22D3EE]" />
              <span>{strings.phase5.streakLabel}</span>
            </div>
            <div>
              <div className="text-3xl font-bold text-white font-mono-tabular">
                {dashboard.currentStreakDays}d
              </div>
              <div className="text-xs text-[#A7B4C8] font-mono-tabular mt-1">
                {strings.phase5.sessionsCompleted(dashboard.completedFocusSessionsCount)}
              </div>
            </div>
          </div>
        </div>

        <section className="lg:col-span-7 rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-4">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <Sprout className="w-5 h-5 text-[#22C55E]" />
              <h2 className="text-base font-semibold text-white">
                {strings.phase5.gardenTitle}
              </h2>
            </div>
            <span className="text-sm font-semibold text-[#22D3EE]">{stageLabel}</span>
          </div>

          {featuredItem && (
            <div className="flex flex-col sm:flex-row items-center gap-5">
              <div className="w-24 h-24 rounded-2xl bg-[#101B32] border border-[#233554] flex items-center justify-center shrink-0">
                <PlantIllustration
                  stage={featuredItem.growthStage}
                  accentColor={featuredItem.accentColor}
                  size={68}
                />
              </div>
              <div className="flex-1 text-center sm:text-left space-y-1">
                <div className="text-lg font-bold text-white">{featuredItem.title}</div>
                <p className="text-sm text-[#A7B4C8]">{featuredItem.description}</p>
                <p className="text-xs font-medium text-[#22C55E] pt-1">{gardenHint}</p>
              </div>
            </div>
          )}
        </section>
      </div>

      {/* Achievements + Store */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Achievement Badges */}
        <section className="lg:col-span-5 rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-4">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <Trophy className="w-5 h-5 text-[#F59E0B]" />
              <h2 className="text-base font-semibold text-white">
                {strings.dashboard.achievements}
              </h2>
            </div>
            <span className="text-sm font-semibold text-[#22D3EE] font-mono-tabular">
              {dashboard.unlockedAchievementsCount} / {dashboard.totalAchievementsCount}
            </span>
          </div>

          <div className="space-y-3">
            {dashboard.achievements.map((achievement) => (
              <div
                key={achievement.id}
                className="rounded-xl bg-[#101B32] border border-[#233554] p-3.5 flex items-center gap-3"
              >
                <div
                  className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 ${
                    achievement.isUnlocked
                      ? 'bg-[#22C55E]/15 text-[#22C55E]'
                      : 'bg-[#16243D] text-[#A7B4C8]'
                  }`}
                >
                  {achievement.isUnlocked ? (
                    <CheckCircle2 className="w-5 h-5" />
                  ) : (
                    <Lock className="w-4 h-4" />
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <div
                    className={`text-sm font-semibold ${
                      achievement.isUnlocked ? 'text-white' : 'text-[#A7B4C8]'
                    }`}
                  >
                    {achievement.title}
                  </div>
                  <div className="text-xs text-[#A7B4C8]">{achievement.description}</div>
                </div>
                <div
                  className={`text-sm font-bold font-mono-tabular shrink-0 ${
                    achievement.isUnlocked ? 'text-[#22C55E]' : 'text-[#F59E0B]'
                  }`}
                >
                  +{achievement.coinBonus}
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Garden Companion Store */}
        <section className="lg:col-span-7 rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-4">
          <h2 className="text-base font-semibold text-white">{strings.phase5.storeTitle}</h2>

          <div className="space-y-3">
            {dashboard.storeItems.map((item) => {
              const canAfford = dashboard.coinBalance >= item.costCoins;
              return (
                <div
                  key={item.id}
                  className="rounded-xl bg-[#101B32] border border-[#233554] p-4 flex items-center gap-4"
                >
                  <div className="w-14 h-14 rounded-xl bg-[#16243D] border border-[#233554] flex items-center justify-center shrink-0">
                    <PlantIllustration
                      stage={item.growthStage}
                      accentColor={item.accentColor}
                      size={42}
                    />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-semibold text-white">{item.title}</div>
                    <p className="text-xs text-[#A7B4C8] mt-0.5">{item.description}</p>
                  </div>
                  <div className="shrink-0">
                    {item.isUnlocked ? (
                      item.isFeatured ? (
                        <span className="inline-flex items-center min-h-[44px] px-3 text-sm font-semibold text-[#22C55E] whitespace-nowrap">
                          {strings.phase5.featuredBadge}
                        </span>
                      ) : (
                        <button
                          type="button"
                          onClick={() => onSetFeaturedItem(item.id)}
                          className="min-h-[44px] px-4 py-2 rounded-xl bg-[#16243D] border border-[#233554] hover:border-[#3B82F6] text-xs font-semibold text-white whitespace-nowrap cursor-pointer"
                        >
                          {strings.phase5.setFeatured}
                        </button>
                      )
                    ) : (
                      <button
                        type="button"
                        onClick={() => onPurchaseItem(item.id)}
                        disabled={!canAfford}
                        className="min-h-[44px] px-4 py-2 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] disabled:bg-[#16243D] disabled:text-[#A7B4C8] text-white text-sm font-semibold font-mono-tabular whitespace-nowrap cursor-pointer disabled:cursor-not-allowed"
                      >
                        {item.costCoins}c
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      </div>

      {/* Coin Ledger */}
      <section className="rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-4">
        <h2 className="text-base font-semibold text-white">{strings.phase5.ledgerTitle}</h2>
        {dashboard.recentTransactions.length === 0 ? (
          <p className="text-sm text-[#A7B4C8]">{strings.phase5.ledgerEmpty}</p>
        ) : (
          <div className="divide-y divide-[#233554]">
            {dashboard.recentTransactions.map((tx) => {
              const isEarn = tx.amount >= 0;
              return (
                <div
                  key={tx.id}
                  className="py-3 flex items-center justify-between gap-4 text-sm"
                >
                  <div className="min-w-0">
                    <div className="font-medium text-white truncate">{tx.title}</div>
                    <div className="text-xs text-[#A7B4C8] font-mono-tabular">{tx.localDate}</div>
                  </div>
                  <div
                    className={`font-bold font-mono-tabular shrink-0 ${
                      isEarn ? 'text-[#22C55E]' : 'text-[#F59E0B]'
                    }`}
                  >
                    {isEarn ? `+${tx.amount}` : tx.amount}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
