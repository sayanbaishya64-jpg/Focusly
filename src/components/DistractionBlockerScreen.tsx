import { useState } from 'react';
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  Info,
  Shield,
  Smartphone,
} from 'lucide-react';
import type { FocuslyStrings } from '../i18n/translations';
import {
  DISTRACTION_CANDIDATES,
  type DistractionBlockerSettings,
  type DistractionPermissionStatus,
} from '../types/focusly';
import { resolveDistractionBlockerMode } from '../utils/adaptiveFocusEngine';

interface DistractionBlockerScreenProps {
  strings: FocuslyStrings;
  settings: DistractionBlockerSettings;
  onBack: () => void;
  onToggleEnabled: (enabled: boolean) => void;
  onToggleApp: (packageName: string) => void;
  onSetPermissionStatus: (status: DistractionPermissionStatus) => void;
}

export function DistractionBlockerScreen({
  strings,
  settings,
  onBack,
  onToggleEnabled,
  onToggleApp,
  onSetPermissionStatus,
}: DistractionBlockerScreenProps) {
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const mode = resolveDistractionBlockerMode(settings);
  const granted = settings.permissionStatus === 'GRANTED';

  const modeLabel =
    mode === 'ACTIVE_DETECTION'
      ? strings.phase3.blockerStatusActive
      : mode === 'CHECKLIST_ONLY'
        ? strings.phase3.blockerStatusChecklist
        : strings.phase3.blockerStatusDisabled;

  const handleTestStatus = () => {
    if (!settings.isEnabled) {
      setStatusMessage('Distraction awareness is off. Your Focus Timer works normally.');
    } else if (settings.selectedPackageNames.length === 0) {
      setStatusMessage('Enabled, with 0 apps selected. Pick apps below to track.');
    } else if (granted) {
      setStatusMessage(
        `Ready: monitoring ${settings.selectedPackageNames.length} app(s) during active focus blocks.`,
      );
    } else {
      setStatusMessage(
        `Checklist mode: ${settings.selectedPackageNames.length} app(s) selected without Usage Access.`,
      );
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={onBack}
          aria-label={strings.phase3.blockerBack}
          className="min-h-[44px] min-w-[44px] rounded-xl bg-[#101B32] border border-[#233554] hover:border-[#3B82F6] flex items-center justify-center text-white cursor-pointer"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-white">
            {strings.phase3.blockerTitle}
          </h1>
          <p className="text-sm text-[#A7B4C8]">{strings.phase3.blockerSubtitle}</p>
        </div>
      </div>

      {/* Enable Toggle & Current Mode */}
      <section className="rounded-2xl bg-[#16243D] border border-[#233554] p-6 flex items-center justify-between gap-4">
        <div className="space-y-1">
          <h2 className="text-base font-semibold text-white">
            {strings.phase3.blockerEnableLabel}
          </h2>
          <p className="text-sm text-[#22D3EE]">{modeLabel}</p>
        </div>
        <button
          type="button"
          role="switch"
          aria-checked={settings.isEnabled}
          onClick={() => onToggleEnabled(!settings.isEnabled)}
          className={`relative inline-flex h-8 w-14 shrink-0 items-center rounded-full transition-colors cursor-pointer ${
            settings.isEnabled ? 'bg-[#3B82F6]' : 'bg-[#101B32] border border-[#233554]'
          }`}
        >
          <span
            className={`inline-block h-6 w-6 transform rounded-full bg-white transition-transform ${
              settings.isEnabled ? 'translate-x-7' : 'translate-x-1'
            }`}
          />
        </button>
      </section>

      {/* Permission Status + Controls */}
      <section className="rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-4">
        <div className="flex items-center gap-2.5">
          {granted ? (
            <CheckCircle2 className="w-5 h-5 text-[#22C55E] shrink-0" />
          ) : (
            <AlertTriangle className="w-5 h-5 text-[#F59E0B] shrink-0" />
          )}
          <h2 className="text-base font-semibold text-white">
            {granted ? strings.phase3.blockerPermGranted : strings.phase3.blockerPermDenied}
          </h2>
        </div>

        <p className="text-sm text-[#A7B4C8] leading-relaxed">
          {strings.phase3.blockerPrivacyNote}
        </p>

        <div className="rounded-xl bg-[#101B32] border border-[#233554] p-3.5 flex items-start gap-2.5 text-xs text-[#A7B4C8]">
          <Info className="w-4 h-4 text-[#22D3EE] shrink-0 mt-0.5" />
          <span>{strings.phase3.blockerWebBoundaryNote}</span>
        </div>

        <div className="flex flex-wrap gap-3 pt-1">
          <button
            type="button"
            onClick={() => onSetPermissionStatus(granted ? 'DENIED' : 'GRANTED')}
            className="min-h-[44px] px-4 py-2 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white text-sm font-semibold whitespace-nowrap cursor-pointer"
          >
            {granted ? strings.phase3.blockerSimRevoke : strings.phase3.blockerSimGrant}
          </button>
          <button
            type="button"
            onClick={handleTestStatus}
            className="min-h-[44px] px-4 py-2 rounded-xl bg-[#101B32] border border-[#233554] hover:border-[#3B82F6] text-white text-sm font-semibold whitespace-nowrap cursor-pointer"
          >
            {strings.phase3.blockerTestStatus}
          </button>
        </div>

        {statusMessage && (
          <p className="text-sm text-[#22D3EE]">{statusMessage}</p>
        )}
      </section>

      {/* Selectable Apps */}
      <section className="space-y-3">
        <div className="flex items-center gap-2 text-base font-semibold text-white">
          <Shield className="w-5 h-5 text-[#22D3EE]" />
          <span>
            {strings.phase3.blockerSelectedApps} ({settings.selectedPackageNames.length})
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {DISTRACTION_CANDIDATES.map((app) => {
            const selected = settings.selectedPackageNames.includes(app.packageName);
            return (
              <button
                key={app.packageName}
                type="button"
                onClick={() => onToggleApp(app.packageName)}
                className={`min-h-[64px] p-4 rounded-2xl border text-left flex items-center justify-between gap-3 transition-colors cursor-pointer ${
                  selected
                    ? 'bg-[#3B82F6]/15 border-[#3B82F6]'
                    : 'bg-[#16243D] border-[#233554] hover:border-[#3B82F6]/50'
                }`}
              >
                <div className="flex items-center gap-3 min-w-0">
                  <Smartphone className="w-5 h-5 text-[#22D3EE] shrink-0" />
                  <div className="min-w-0">
                    <div className="text-sm font-semibold text-white truncate">{app.label}</div>
                    <div className="text-xs text-[#A7B4C8] truncate">{app.category}</div>
                  </div>
                </div>
                <CheckCircle2
                  className={`w-5 h-5 shrink-0 ${
                    selected ? 'text-[#3B82F6]' : 'text-[#A7B4C8]/40'
                  }`}
                />
              </button>
            );
          })}
        </div>
      </section>
    </div>
  );
}
