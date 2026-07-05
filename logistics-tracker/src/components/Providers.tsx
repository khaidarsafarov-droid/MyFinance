"use client";

import { AuthProvider } from "@/contexts/AuthContext";
import { LogisticsSyncProvider } from "@/components/LogisticsSyncProvider";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import { EquipmentOnboardingGuard } from "@/components/EquipmentOnboardingGuard";

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <AuthProvider>
      <LogisticsSyncProvider>
        <EquipmentOnboardingGuard>
          <ErrorBoundary>{children}</ErrorBoundary>
        </EquipmentOnboardingGuard>
      </LogisticsSyncProvider>
    </AuthProvider>
  );
}
