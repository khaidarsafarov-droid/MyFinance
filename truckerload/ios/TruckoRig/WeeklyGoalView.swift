import SwiftUI
import TruckerLoadShared

/// First iOS screen: weekly-goal math from `:shared:domain` via `TruckerLoadShared`.
/// Journal UI (loads, diesel, Room) stays on Android until those types move into KMP.
struct WeeklyGoalView: View {
    @State private var goalText = "3500"
    @State private var grossText = "1200"
    @State private var daysRemainingText = "4"

    private var dailyNeeded: Double {
        let cents = SharedBusinessLogic.shared.dailyTargetCents(
            goalCents: (Int64(goalText) ?? 0) * 100,
            totalGrossCents: (Int64(grossText) ?? 0) * 100,
            daysRemaining: Int32(Int(daysRemainingText) ?? 1)
        )
        return Double(cents) / 100
    }

    private var expectedByDayOne: Double {
        let cents = SharedBusinessLogic.shared.expectedGrossByNowCents(
            goalCents: (Int64(goalText) ?? 0) * 100,
            daysActive: 1
        )
        return Double(cents) / 100
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Weekly goal") {
                    TextField("Goal $", text: $goalText)
                        .keyboardType(.decimalPad)
                    TextField("Gross so far $", text: $grossText)
                        .keyboardType(.decimalPad)
                    TextField("Days left this week", text: $daysRemainingText)
                        .keyboardType(.numberPad)
                }
                Section("From shared Kotlin") {
                    LabeledContent("Need per remaining day") {
                        Text(dailyNeeded, format: .currency(code: "USD"))
                    }
                    LabeledContent("Linear day-1 marker") {
                        Text(expectedByDayOne, format: .currency(code: "USD"))
                    }
                    LabeledContent("Push platform") {
                        Text(SharedBusinessLogic.shared.iosPushPlatform())
                    }
                }
            }
            .navigationTitle("TruckoRig")
        }
    }
}

#Preview {
    WeeklyGoalView()
}
