import SwiftUI
import ComposeApp

struct SpecialistsView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    
    var body: some View {
        CircuitView(screen: SpecialistRouteSpecialists(), navigator: navigation) { (state: SpecialistsState) in
            VStack(spacing: 0) {
                // Top App Bar
                MiraTopBar {
                    Text("Salon Specialists")
                        .font(.headline)
                        .bold()
                }
                
                // Content
                if state.isLoading {
                    SpecialistsShimmerView()
                } else if let error = state.error {
                    MiraErrorView(message: error) {
                        state.eventSink(SpecialistsEventRetry())
                    }
                } else if state.specialists.isEmpty {
                    EmptyStateView(
                        message: "No specialists found",
                        description: "We couldn't find any specialists at the moment. Please check back later or try a different search.",
                        icon: "person.2"
                    )
                } else {
                    ScrollView {
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                            ForEach(state.specialists, id: \.id) { specialist in
                                SpecialistCard(specialist: specialist) {
                                    state.eventSink(SpecialistsEventSpecialistClicked(specialistId: specialist.id))
                                }
                            }
                        }
                        .padding(16)
                    }
                }
            }
            .background(MiraTheme.background)
        }
    }
}

struct SpecialistsShimmerView: View {
    var body: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                ForEach(0..<6, id: \.self) { _ in
                    MiraShimmerBlock(height: 160 / 0.7, cornerRadius: MiraTheme.radiusSmall)
                }
            }
            .padding(16)
        }
    }
}
