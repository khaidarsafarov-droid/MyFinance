"use client";

import { useAuth } from "@/contexts/AuthContext";
import { EquipmentSelectionModal } from "@/components/EquipmentSelectionModal";

/** Shows EquipmentSelectionModal when user has no equipment_type (onboarding) */
export function EquipmentOnboardingGuard({
  children,
}: {
  children: React.ReactNode;
}) {
  const { user, profile, isLoading, refreshProfile } = useAuth();

  const needsEquipment =
    !isLoading &&
    user != null &&
    profile != null &&
    (profile.equipment_type == null || String(profile.equipment_type).trim() === "");

  return (
    <>
      {children}
      {needsEquipment && (
        <EquipmentSelectionModal
          onSaved={() => {
            refreshProfile();
          }}
        />
      )}
    </>
  );
}
