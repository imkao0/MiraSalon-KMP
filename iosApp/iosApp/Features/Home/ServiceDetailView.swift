import SwiftUI
import Foundation
import ComposeApp

struct ServiceDetailView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let serviceId: String
    @State private var showReviewSheet = false
    
    var body: some View {
        CircuitView(screen: ServiceRouteServiceDetail(serviceId: serviceId), navigator: navigation) { (state: ServiceDetailState) in
            VStack(spacing: 0) {
                MiraTopBar {
                    Text("")
                } actions: {
                    Button {
                        state.eventSink(ServiceDetailEventNotificationClicked())
                    } label: {
                        ZStack(alignment: .topTrailing) {
                            Image(systemName: "bell")
                                .font(.system(size: 20))
                                .foregroundColor(MiraTheme.onSurface)
                                .accessibilityLabel("Notifications")
                            
                            if state.unreadNotificationCount > 0 {
                                Text(state.unreadNotificationCount > 9 ? "9+" : "\(state.unreadNotificationCount)")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundColor(MiraTheme.error)
                                    .offset(x: 4, y: -4)
                                    .accessibilityLabel("\(state.unreadNotificationCount) unread notifications")
                            }
                        }
                    }
                    
                    Button {
                        state.eventSink(ServiceDetailEventToggleFavorite())
                    } label: {
                        Image(systemName: state.isFavorited ? "bookmark.fill" : "bookmark")
                            .foregroundColor(MiraTheme.onSurface)
                    }
                }
                
                ScrollView {
                    if state.isLoading {
                        ServiceDetailShimmerView()
                    } else if let error = state.error {
                        VStack {
                            Text(error).foregroundColor(MiraTheme.error)
                            Button("Retry") { state.eventSink(ServiceDetailEventRetry()) }
                        }
                        .padding(.top, 50)
                    } else if let service = state.service {
                        VStack(alignment: .leading, spacing: 0) {
                            BoxView {
                                if let imageUrl = service.imageUrl {
                                    let resolvedUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: imageUrl)
                                    AsyncImage(url: URL(string: resolvedUrl ?? "")) { image in
                                        image.resizable()
                                            .aspectRatio(contentMode: .fit)
                                    } placeholder: {
                                        MiraShimmer()
                                    }
                                } else {
                                    Image(systemName: "spa")
                                        .resizable()
                                        .aspectRatio(contentMode: .fit)
                                        .frame(width: 220, height: 220)
                                        .foregroundStyle(
                                            LinearGradient(
                                                colors: [MiraTheme.onSurfaceVariant.opacity(0.3), Color.clear],
                                                startPoint: .top,
                                                endPoint: .bottom
                                            )
                                        )
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 300)
                            
                            if service.rating > 0 {
                                HStack {
                                    Spacer()
                                    RatingCircleView(rating: service.rating, reviewCount: service.reviews.count)
                                    Spacer()
                                }
                                .padding(.vertical, 24)
                            }
                            
                            VStack(alignment: .leading, spacing: 16) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(service.name)
                                        .font(.title2)
                                        .bold()
                                        .foregroundColor(MiraTheme.onSurface)
                                    
                                    Text("SERVICE ID: #\(service.id.suffix(6).uppercased())")
                                        .font(.caption2)
                                        .foregroundColor(MiraTheme.onSurfaceVariant)
                                }
                                
                                if service.rating > 0 {
                                    RatingChip(rating: String(format: "%.1f", service.rating))
                                }
                                
                                HStack(alignment: .firstTextBaseline, spacing: 12) {
                                    if service.discountPercent > 0 {
                                        Text("$" + String(format: "%.2f", service.discountedPrice))
                                            .font(.title)
                                            .bold()
                                            .foregroundColor(MiraTheme.primary)
                                        
                                        Text("$" + String(format: "%.2f", service.price))
                                            .font(.headline)
                                            .strikethrough()
                                            .foregroundColor(MiraTheme.onSurfaceVariant.opacity(0.6))
                                        
                                        Text("-\(service.discountPercent)%")
                                            .font(.caption)
                                            .bold()
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background(Color.red.opacity(0.1))
                                            .foregroundColor(.red)
                                            .cornerRadius(4)
                                    } else {
                                        Text("$" + String(format: "%.2f", service.price))
                                            .font(.title)
                                            .bold()
                                            .foregroundColor(MiraTheme.primary)
                                    }
                                }
                                
                                HStack(spacing: 16) {
                                    InfoChip(label: "Duration", value: "\(service.durationMinutes) MIN")
                                    InfoChip(label: "Category", value: state.categoryName ?? "General")
                                }
                                
                                VStack(alignment: .leading, spacing: 8) {
                                    Text("Description")
                                        .font(.caption)
                                        .bold()
                                        .foregroundColor(MiraTheme.onSurfaceVariant)
                                    
                                    Text(formattedDescription(service.description_))
                                        .font(.body)
                                        .lineSpacing(4)
                                        .foregroundColor(MiraTheme.onSurface)
                                }
                                .padding(.top, 8)
                                
                                Button {
                                    showReviewSheet = true
                                } label: {
                                    Text("Leave a review")
                                        .font(.headline)
                                        .foregroundColor(MiraTheme.primary)
                                }
                                .padding(.top, 16)
                                
                                if !service.reviews.isEmpty {
                                    VStack(alignment: .leading, spacing: 16) {
                                        Text("Reviews (\(service.reviews.count))")
                                            .font(.headline)
                                        
                                        ForEach(service.reviews, id: \.id) { review in
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
                                    .padding(.top, 24)
                                } else {
                                    Text("No reviews yet. Be the first to review!")
                                        .font(.subheadline)
                                        .foregroundColor(MiraTheme.onSurfaceVariant)
                                        .padding(.top, 16)
                                }
                                
                                if !state.specialists.isEmpty {
                                    Text("Specialists")
                                        .font(.headline)
                                        .padding(.top, 16)
                                    
                                    ScrollView(.horizontal, showsIndicators: false) {
                                        HStack(spacing: 12) {
                                            ForEach(state.specialists, id: \.id) { specialist in
                                                SpecialistChipView(specialist: specialist)
                                            }
                                        }
                                    }
                                }
                                
                                Spacer().frame(height: 32)
                            }
                            .padding(.horizontal, 24)
                            .padding(.vertical, 16)
                        }
                    }
                }
                
                if let _ = state.service {
                    HStack(spacing: 16) {
                        Button {
                            state.eventSink(ServiceDetailEventSaveClicked())
                        } label: {
                            Text("SAVE")
                                .font(.headline)
                                .bold()
                                .frame(maxWidth: .infinity)
                                .frame(height: 56)
                                .background(MiraTheme.primaryContainer)
                                .foregroundColor(MiraTheme.onPrimaryContainer)
                                .cornerRadius(8)
                        }
                        
                        Button {
                            state.eventSink(ServiceDetailEventBookClicked())
                        } label: {
                            Text("BOOK")
                                .font(.headline)
                                .bold()
                                .frame(maxWidth: .infinity)
                                .frame(height: 56)
                                .background(MiraTheme.primary)
                                .foregroundColor(MiraTheme.onPrimary)
                                .cornerRadius(8)
                        }
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 12)
                    .padding(.bottom, (UIApplication.shared.windows.first?.safeAreaInsets.bottom ?? 0) + 12)
                    .background(MiraTheme.surface)
                    .shadow(radius: 4)
                }
            }
            .background(MiraTheme.background)
        }
    }

    private func formattedDescription(_ desc: String) -> String {
        if desc.starts(with: "{") && desc.contains("\":\"") {
            if let data = desc.data(using: .utf8),
               let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let value = dict.values.first as? String {
                return value
            }
        }
        return desc
    }
}

struct ServiceDetailShimmerView: View {
    private let baseColor = MiraTheme.surfaceVariant.opacity(0.4)
    private let highlightColor = MiraTheme.surfaceVariant

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Large Image Shimmer
            MiraShimmerBlock(height: 300, cornerRadius: 12)
                .padding(.horizontal, 24)
            
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    // Title Shimmer
                    MiraShimmerBlock(width: 200, height: 28, cornerRadius: 4)
                    
                    // ID Shimmer
                    MiraShimmerBlock(width: 120, height: 14, cornerRadius: 4)
                }
                
                // Rating Shimmer
                MiraShimmerBlock(width: 80, height: 24, cornerRadius: 8)
                
                // Price Shimmer
                MiraShimmerBlock(width: 100, height: 32, cornerRadius: 4)
                
                // Info Chips Shimmer
                HStack(spacing: 16) {
                    MiraShimmerBlock(height: 50, cornerRadius: 8)
                    MiraShimmerBlock(height: 50, cornerRadius: 8)
                }
                
                // Description Shimmer
                VStack(alignment: .leading, spacing: 12) {
                    MiraShimmerBlock(width: 80, height: 14, cornerRadius: 4)
                    
                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(0..<4, id: \.self) { _ in
                            MiraShimmerBlock(height: 14, cornerRadius: 4)
                        }
                    }
                }
                .padding(.top, 8)
                
                Spacer().frame(height: 100)
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
        }
    }
}

// Box wrapper for SwiftUI
struct BoxView<Content: View>: View {
    let content: Content
    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }
    var body: some View { content }
}

struct SpecialistChipView: View {
    let specialist: Specialist
    
    var body: some View {
        VStack(spacing: 8) {
            let resolvedUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: specialist.imageUrl)
            AsyncImage(url: URL(string: resolvedUrl ?? "")) { image in
                image.resizable()
                    .aspectRatio(contentMode: .fill)
            } placeholder: {
                Image(systemName: "person.fill")
                    .foregroundColor(MiraTheme.onSurfaceVariant)
            }
            .frame(width: 60, height: 60)
            .clipShape(Circle())
            .background(MiraTheme.surfaceVariant)
            
            Text(specialist.name)
                .font(.caption)
                .bold()
                .foregroundColor(MiraTheme.onSurface)
                .lineLimit(1)
            
            if specialist.rating > 0 {
                HStack(spacing: 2) {
                    Image(systemName: "star.fill")
                        .resizable()
                        .frame(width: 12, height: 12)
                        .foregroundColor(.yellow)
                    Text(String(format: "%.1f", specialist.rating))
                        .font(.caption2)
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                }
            }
        }
        .padding(12)
        .frame(width: 100)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .stroke(MiraTheme.surfaceVariant, lineWidth: 1)
        )
    }
}

struct RatingCircleView: View {
    let rating: Double
    let reviewCount: Int
    
    var body: some View {
        ZStack {
            Circle()
                .fill(Color.yellow)
                .frame(width: 60, height: 60)
            
            VStack(spacing: 2) {
                Text(String(format: "%.1f", rating))
                    .font(.title)
                    .bold()
                    .foregroundColor(.black)
                
                Text("\(reviewCount) reviews")
                    .font(.caption2)
                    .foregroundColor(.black.opacity(0.7))
            }
        }
    }
}
