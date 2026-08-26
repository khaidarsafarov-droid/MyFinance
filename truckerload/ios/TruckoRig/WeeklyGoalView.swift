import SwiftUI
import TruckerLoadShared

/// First iOS screen: weekly-goal math from `:shared:domain` via `TruckerLoadShared`.
/// Journal UI (loads, diesel, Room) stays on Android until those types move into KMP.
struct WeeklyGoalView: View {
    @State private var goalText = "3500"
    @State private var grossText = "1200"
    @State private var daysRemainingText = "4"

    private var dailyNeeded: Double {
        SharedBusinessLogic.shared.dailyTarget(
            goal: Double(goalText) ?? 0,
            totalGross: Double(grossText) ?? 0,
            daysRemaining: Int32(Int(daysRemainingText) ?? 1)
        )
    }

    private var expectedByDayOne: Double {
        SharedBusinessLogic.shared.expectedGrossByNow(goal: Double(goalText) ?? 0, daysActive: 1)
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
