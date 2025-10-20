import SwiftUI
import ComposeApp

struct SpecialistDetailView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let specialistId: String
    
    var body: some View {
        CircuitView(screen: SpecialistRouteSpecialistDetail(specialistId: specialistId), navigator: navigation) { (state: SpecialistDetailState) in
            ZStack(alignment: .bottom) {
                VStack(spacing: 0) {
                    // Top App Bar
                    MiraTopBar {
                        Text("")
                    }
                    
                    if state.isLoading {
                        Spacer()
                        ProgressView()
                        Spacer()
                    } else if let error = state.error {
                        Spacer()
                        Text(error).foregroundColor(MiraTheme.error)
                        Spacer()
                    } else if let specialist = state.specialist {
                        ScrollView {
                            VStack(alignment: .leading, spacing: 0) {
                                // Header (Image + Name + Role)
                                SpecialistDetailHeader(specialist: specialist)
                                
                                // Stats
                                StatsRowView(specialist: specialist)
                                    .padding(.top, 16)
                                
                                // About
                                if !specialist.bio.isEmpty {
                                    SectionHeaderTitle(text: "About")
                                    Text(specialist.bio)
                                        .font(.body)
                                        .foregroundColor(MiraTheme.onSurfaceVariant)
                                        .padding(.horizontal, 16)
                                        .padding(.vertical, 4)
                                }
                                
                                // Services
                                SectionHeaderTitle(text: "Services")
                                if specialist.services.isEmpty {
                                    Text("No services listed yet")
                                        .font(.subheadline)
                                        .foregroundColor(MiraTheme.onSurfaceVariant)
                                        .padding(16)
                                } else {
                                    VStack(spacing: 0) {
                                        ForEach(specialist.services, id: \.id) { service in
                                            ServiceRowView(service: service) {
                                                state.eventSink(SpecialistDetailEventBookServiceClicked(serviceId: service.id))
                                            }
                                        }
                                    }
                                }
                                
                                // Reviews
                                HStack {
                                    SectionHeaderTitle(text: "Reviews (\(specialist.reviews.count))")
                                    Spacer()
                                    Button("Write a Review") {
                                        state.eventSink(SpecialistDetailEventWriteReviewClicked())
                                    }
                                    .font(.subheadline)
                                    .foregroundColor(MiraTheme.primary)
                                    .padding(.trailing, 16)
                                }
                                
                                if specialist.reviews.isEmpty {
                                    Text("No reviews yet")
                                        .font(.subheadline)
                                        .foregroundColor(MiraTheme.onSurfaceVariant)
                                        .padding(16)
                                } else {
                                    VStack(spacing: 16) {
                                        ForEach(specialist.reviews, id: \.id) { review in
                                            ReviewRowView(review: review)
                                        }
                                    }
                                    .padding(.horizontal, 16)
                                }
                                
                                Spacer().frame(height: 120)
                            }
                        }
                    }
                }
                
                // Bottom Bar & FAB
                if let specialist = state.specialist {
                    VStack {
                        HStack {
                            Spacer()
                            Button {
                                state.eventSink(SpecialistDetailEventChatClicked(specialist: specialist))
                            } label: {
                                Image(systemName: "bubble.left.fill")
                                    .font(.title2)
                                    .foregroundColor(.white)
                                    .padding(16)
                                    .background(MiraTheme.primary)
                                    .clipShape(Circle())
                                    .shadow(radius: 4)
                            }
                            .padding(.trailing, 16)
                            .padding(.bottom, 16)
                        }
                    }
                }
            }
            .background(MiraTheme.background)
        }
    }
}

struct SpecialistDetailHeader: View {
    let specialist: Specialist
    
    var body: some View {
        HStack(spacing: MiraTheme.spacingMedium) {
            MiraAvatar(url: specialist.imageUrl, size: MiraTheme.iconSizeExtraLarge + MiraTheme.spacingLarge)
            
            VStack(alignment: .leading, spacing: MiraTheme.spacingSmall) {
                Text(specialist.name)
                    .font(.title2)
                    .bold()
                Text(specialist.role ?? "Specialist")
                    .font(.subheadline)
                    .foregroundColor(MiraTheme.primary)
                
                HStack(spacing: MiraTheme.spacingSmall) {
                    Circle()
                        .fill(specialist.isOnline ? Color(hex: 0xFF4CAF50) : .gray)
                        .frame(width: 12, height: 12)
                    
                    Text(specialist.isOnline ? "Online" : "Offline")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(specialist.isOnline ? Color(hex: 0xFF4CAF50) : .gray)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, MiraTheme.spacingMedium)
        .padding(.vertical, MiraTheme.spacingLarge)
    }
}

struct StatsRowView: View {
    let specialist: Specialist
    
    var body: some View {
        HStack {
            Spacer()
            StatItem(label: "Rating", value: String(format: "%.1f", specialist.rating), icon: "star.fill")
            Spacer()
            StatItem(label: "Experience", value: "\(specialist.yearsOfExperience) Yrs", icon: "checkmark")
            Spacer()
            StatItem(label: "Patients", value: "\(specialist.customersCount)+", icon: "person.fill")
            Spacer()
        }
    }
}

struct StatItem: View {
    let label: String
    let value: String
    let icon: String
    
    var body: some View {
        VStack(spacing: MiraTheme.spacingTiny) {
            HStack(spacing: MiraTheme.spacingTiny) {
                Image(systemName: icon)
                    .foregroundColor(MiraTheme.onSurfaceVariant)
                    .font(.system(size: MiraTheme.iconSizeSmall))
                Text(value)
                    .font(MiraType.labelMedium.weight(.semibold))
                    .foregroundColor(MiraTheme.onSurfaceVariant)
            }
            Text(label)
                .font(MiraType.labelSmall)
                .foregroundColor(MiraTheme.onSurfaceVariant)
        }
    }
}

struct SectionHeaderTitle: View {
    let text: String
    var body: some View {
        Text(text)
            .font(MiraType.titleMedium)
            .padding(.horizontal, MiraTheme.spacingMedium)
            .padding(.vertical, MiraTheme.spacingSmall)
    }
}

struct ServiceRowView: View {
    let service: Service
    let onBook: () -> Void
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: MiraTheme.spacingTiny) {
                Text(service.name)
                    .font(MiraType.bodyLarge)
                    .bold()
                Text(Double(service.price).miraPrice())
                    .font(MiraType.bodyMedium)
                    .foregroundColor(MiraTheme.primary)
            }
            Spacer()
            Button("Book") {
                onBook()
            }
            .font(MiraType.labelLarge)
            .bold()
            .padding(.horizontal, MiraTheme.spacingMedium)
            .padding(.vertical, 6)
            .background(MiraTheme.primary)
            .foregroundColor(MiraTheme.onPrimary)
            .cornerRadius(MiraTheme.radiusMedium)
        }
        .padding(MiraTheme.spacingMedium)
        Divider().padding(.horizontal, MiraTheme.spacingMedium)
    }
}

struct ReviewRowView: View {
    let review: SpecialistReview
    
    var body: some View {
        ReviewItemView(
            userName: review.userName,
            userAvatarUrl: review.userAvatarUrl,
            rating: Int(review.rating),
            comment: review.comment,
            date: Date(timeIntervalSince1970: TimeInterval(review.createdAtEpochSeconds)).formatted(date: .abbreviated, time: .omitted)
        )
        Divider().opacity(0.5)
    }
}
