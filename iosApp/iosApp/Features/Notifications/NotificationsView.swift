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
                            // Extract dates for grouping (simplified for Swift)
                            let todayNotifications = state.notifications.filter { item in
                                let date = Date(timeIntervalSince1970: TimeInterval(item.timestamp / 1000))
                                return Calendar.current.isDateInToday(date)
                            }
                            let earlierNotifications = state.notifications.filter { item in
                                !todayNotifications.contains(where: { $0.id == item.id })
                            }

                            if !todayNotifications.isEmpty {
                                Text("Today")
                                    .font(MiraType.titleLarge.weight(.bold))
                                    .foregroundColor(MiraTheme.onSurface)
                                    .padding(.horizontal, MiraTheme.spacingLarge)
                                    .padding(.vertical, MiraTheme.spacingLarge)

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
                            .fill(MiraTheme.success)
                            .frame(width: MiraTheme.spacingSmall, height: MiraTheme.spacingSmall)
                    }
                }
                .frame(width: MiraTheme.spacingLarge)

                // Avatar + Icon Overlay
                ZStack(alignment: .bottomTrailing) {
                    if item.type == .promo && item.senderAvatarUrl == nil {
                        Circle()
                            .fill(MiraTheme.primaryContainer)
                            .frame(width: MiraTheme.profileAvatarSize, height: MiraTheme.profileAvatarSize)
                            .overlay(
                                Image(systemName: "envelope")
                                    .font(.system(size: 20))
                                    .foregroundColor(MiraTheme.onPrimaryContainer)
                            )
                    } else {
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
                    }

                    Image(systemName: systemIconForType(item.type))
                        .font(.system(size: 8))
                        .foregroundColor(iconColorForType(item.type))
                        .frame(width: 20, height: 20)
                        .background(MiraTheme.surface)
                        .clipShape(Circle())
                        .shadow(radius: 1)
                        .offset(x: 2, y: 2)
                }

                Spacer().frame(width: MiraTheme.spacingMedium)

                // Text content
                VStack(alignment: .leading, spacing: 2) {
                    HStack(alignment: .firstTextBaseline, spacing: MiraTheme.spacingTiny) {
                        let displaySender = item.senderName.lowercased() == "mira salon" || item.senderName.lowercased() == "staff" ? "Mira Salon" : item.senderName

                        Text(displaySender)
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
            }
            .padding(.horizontal, MiraTheme.spacingMedium)
            .padding(.vertical, MiraTheme.spacingMedium)
        }
        .buttonStyle(.plain)
    }
    
    private func systemIconForType(_ type: NotificationType) -> String {
        switch type {
        case NotificationType.comment: return "bubble.left.fill"
        case NotificationType.promo: return "envelope.fill"
        case NotificationType.message: return "message.fill"
        case NotificationType.reminder: return "bell.fill"
        default: return "bell.fill"
        }
    }

    private func iconColorForType(_ type: NotificationType) -> Color {
        switch type {
        case NotificationType.promo: return Color(red: 255/255, green: 112/255, blue: 67/255) // MiraCoral
        case NotificationType.reminder: return Color.orange
        default: return MiraTheme.primary
        }
    }
}
