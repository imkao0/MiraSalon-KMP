import SwiftUI
import ComposeApp

struct NotificationsView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let screen: NotificationRouteNotifications
    
    var body: some View {
        CircuitView(screen: screen, navigator: navigation) { (state: NotificationState) in
            VStack(spacing: 0) {
                // Top App Bar
                MiraCenterAlignedTopAppBar(
                    title: "Notifications",
                    onBackClick: { state.eventSink(NotificationEventBackClicked()) },
                    actions: {
                        HStack(spacing: 8) {
                            Menu {
                                Button("All") {
                                    state.eventSink(NotificationEventFilterChanged(type: nil))
                                }
                                let types: [NotificationType] = [.comment, .promo, .message, .reminder]
                                ForEach(types, id: \.self) { type in
                                    Button(type.name) {
                                        state.eventSink(NotificationEventFilterChanged(type: type))
                                    }
                                }
                            } label: {
                                Image(systemName: "slider.horizontal.3")
                                    .foregroundColor(MiraTheme.onSurface)
                            }

                            Button {
                                state.eventSink(NotificationEventClearAll())
                            } label: {
                                Image(systemName: "trash")
                                    .foregroundColor(MiraTheme.onSurface)
                            }
                        }
                    }
                )

                if state.isLoading {
                    MiraLoadingView()
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 0) {
                            let unreadCount = state.notifications.filter { $0.isUnread }.count
                            Text("You have \(unreadCount) Notifications today.")
                                .font(MiraType.bodyMedium)
                                .foregroundColor(MiraTheme.onSurfaceVariant)
                                .padding(.horizontal, MiraTheme.spacingLarge)
                                .padding(.vertical, MiraTheme.spacingTiny)

                            Spacer().frame(height: MiraTheme.spacingLarge)

                            let todayNotifications = state.notifications.filter { $0.time.contains("ago") }
                            let earlierNotifications = state.notifications.filter { !$0.time.contains("ago") }

                            if !todayNotifications.isEmpty {
                                ForEach(todayNotifications, id: \.id) { item in
                                    NotificationRowView(item: item) {
                                        state.eventSink(NotificationEventNotificationClicked(id: item.id))
                                    }
                                    Divider()
                                        .background(MiraTheme.outlineVariant)
                                        .padding(.horizontal, MiraTheme.spacingLarge)
                                }
                            }

                            if !earlierNotifications.isEmpty {
                                Text("This Week")
                                    .font(MiraType.titleLarge.weight(.bold))
                                    .foregroundColor(MiraTheme.onSurface)
                                    .padding(.horizontal, MiraTheme.spacingLarge)
                                    .padding(.vertical, MiraTheme.spacingLarge)

                                ForEach(earlierNotifications, id: \.id) { item in
                                    NotificationRowView(item: item) {
                                        state.eventSink(NotificationEventNotificationClicked(id: item.id))
                                    }
                                    Divider()
                                        .background(MiraTheme.outlineVariant)
                                        .padding(.horizontal, MiraTheme.spacingLarge)
                                }
                            }
                            
                            if state.notifications.isEmpty {
                                EmptyStateView(
                                    message: "No notifications yet",
                                    description: "We'll notify you when something important happens, like booking updates or special offers.",
                                    icon: "bell"
                                )
                                .padding(.top, 50)
                            }
                        }
                    }
                }
            }
            .background(MiraTheme.background)
        }
    }
}

private struct NotificationRowView: View {
    let item: NotificationItem
    let onClick: () -> Void
    
    var body: some View {
        Button(action: onClick) {
            HStack(alignment: .center, spacing: 0) {
                // Unread dot
                ZStack {
                    if item.isUnread {
                        Circle()
                            .fill(Color.red)
                            .frame(width: MiraTheme.spacingSmall, height: MiraTheme.spacingSmall)
                    }
                }
                .frame(width: MiraTheme.spacingLarge)

                // Avatar + Icon Overlay
                ZStack(alignment: .topTrailing) {
                    let avatarUrl = item.senderAvatarUrl ?? "https://api.dicebear.com/7.x/avataaars/svg?seed=\(item.senderName)"
                    let resolvedUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: avatarUrl)
                    AsyncImage(url: URL(string: resolvedUrl ?? "")) { image in
                        image.resizable()
                            .aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Circle().fill(MiraTheme.surfaceVariant)
                    }
                    .frame(width: MiraTheme.profileAvatarSize, height: MiraTheme.profileAvatarSize)
                    .clipShape(Circle())

                    Text(iconForType(item.type))
                        .font(.system(size: 10))
                        .frame(width: MiraTheme.iconSizeMedium, height: MiraTheme.iconSizeMedium)
                        .background(MiraTheme.surface)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(MiraTheme.surfaceVariant, lineWidth: 0.5))
                }

                Spacer().frame(width: MiraTheme.spacingMedium)

                // Text content
                VStack(alignment: .leading, spacing: 2) {
                    HStack(alignment: .firstTextBaseline, spacing: MiraTheme.spacingTiny) {
                        Text(item.senderName)
                            .font(MiraType.bodyMedium.weight(.bold))
                            .foregroundColor(MiraTheme.primary)
                        
                        Text(item.message)
                            .font(MiraType.bodyMedium)
                            .foregroundColor(MiraTheme.onSurface)
                            .multilineTextAlignment(.leading)
                    }
                    
                    Text(item.time)
                        .font(MiraType.bodySmall)
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                    
                    if item.type == .reminder, let details = item.reminderTimeDetails {
                        Text("Reminder: \(details)")
                            .font(MiraType.bodySmall.weight(.semibold))
                            .foregroundColor(MiraTheme.primary)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                // Thumbnail
                if let thumbnailUrl = item.thumbnail {
                    let resolvedThumbnail = ApiEndpoints.shared.resolveImageUrl(imagePath: thumbnailUrl)
                    AsyncImage(url: URL(string: resolvedThumbnail ?? "")) { image in
                        image.resizable()
                            .aspectRatio(contentMode: .fill)
                    } placeholder: {
                        RoundedRectangle(cornerRadius: MiraTheme.radiusMedium).fill(MiraTheme.surfaceVariant)
                    }
                    .frame(width: MiraTheme.profileAvatarSize, height: MiraTheme.profileAvatarSize)
                    .cornerRadius(MiraTheme.radiusMedium)
                }
            }
            .padding(.horizontal, MiraTheme.spacingMedium)
            .padding(.vertical, MiraTheme.spacingMedium)
        }
        .buttonStyle(.plain)
    }
    
    private func iconForType(_ type: NotificationType) -> String {
        switch type {
        case NotificationType.comment: return "bubble.left.fill"
        case NotificationType.promo: return "tag.fill"
        case NotificationType.message: return "envelope.fill"
        case NotificationType.reminder: return "bell.fill"
        default: return "bell.fill"
        }
    }
}
