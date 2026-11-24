import { useEffect } from 'react';
import { FocuslyBrandMark } from './FocuslyBrandMark';
import type { FocuslyStrings } from '../i18n/translations';

interface SplashScreenProps {
  strings: FocuslyStrings;
  onComplete: () => void;
}

export function SplashScreen({ strings, onComplete }: SplashScreenProps) {
  useEffect(() => {
    const timer = window.setTimeout(onComplete, 1100);
    return () => window.clearTimeout(timer);
  }, [onComplete]);

  return (
    <main className="min-h-screen w-full bg-[#080F1F] flex flex-col items-center justify-center px-6 py-12">
      <div className="flex flex-col items-center text-center max-w-md transition-opacity duration-200">
        <FocuslyBrandMark size={76} />
        <h1 className="mt-6 text-3xl sm:text-4xl font-extrabold tracking-[0.14em] text-white">
          {strings.appName}
        </h1>
        <p className="mt-3 text-base text-[#A7B4C8] text-balance">
          {strings.tagline}
        </p>
        <button
          type="button"
          onClick={onComplete}
          className="mt-8 min-h-[44px] px-5 py-2.5 text-sm font-medium text-[#A7B4C8] hover:text-white transition-colors duration-150 whitespace-nowrap shrink-0 cursor-pointer"
        >
          {strings.continueLabel}
        </button>
      </div>
    </main>
  );
}
