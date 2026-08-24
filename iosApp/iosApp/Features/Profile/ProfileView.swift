import SwiftUI
import ComposeApp

/// Mirrors Android `ProfileScreen.kt` (the "Settings" tab root).
struct ProfileView: View {
    @EnvironmentObject private var navigation: CircuitNavigation

    var body: some View {
        CircuitView(screen: BottomNavKeyProfile(), navigator: navigation) { (state: ProfileState) in
            VStack(spacing: 0) {
                MiraTopBar {
                    Text("Settings")
                        .font(MiraType.headlineSmall.weight(.bold))
                }

                if state.isLoading {
                    Spacer()
                    ProgressView()
                    Spacer()
                } else {
                    ScrollView {
                        VStack(spacing: 0) {
                            Spacer().frame(height: MiraTheme.spacingMedium)

                            ProfileCard {
                                ProfileHeader(
                                    name: state.profile?.fullName ?? "",
                                    email: state.profile?.email ?? "",
                                    avatarUrl: state.profile?.avatarUrl,
                                    onEditProfile: { state.eventSink(ProfileEventEditProfile()) }
                                )
                            }

                            MiraSectionHeader(title: "Account details")
                            ProfileCard {
                                VStack(spacing: 0) {
                                    ProfileMenuRow(icon: "person", label: "Account Info") {
                                        state.eventSink(ProfileEventEditProfile())
                                    }
                                    ProfileDivider()
                                    ProfileMenuRow(
                                        icon: "location",
                                        label: "Saved Addresses",
                                        trailingText: state.addressCount > 0 ? "\(state.addressCount)" : nil
                                    ) {
                                        state.eventSink(ProfileEventSavedAddresses())
                                    }
                                    ProfileDivider()
                                    ProfileMenuRow(
                                        icon: "calendar",
                                        label: "Appointments",
                                        badgeCount: Int(state.upcomingRemindersCount)
                                    ) {
                                        state.eventSink(ProfileEventMyAppointments())
                                    }
                                    ProfileDivider()
                                    ProfileMenuRow(icon: "doc.plaintext", label: "Orders") {
                                        state.eventSink(ProfileEventMyOrders())
                                    }
                                }
                            }

                            MiraSectionHeader(title: "Preferences")
                            ProfileCard {
                                VStack(spacing: 0) {
                                    ProfileMenuRow(icon: "heart", label: "Favourites") {
                                        state.eventSink(ProfileEventFavourites())
                                    }
                                    ProfileDivider()
                                    ProfileMenuRowWithTrailing(
                                        icon: "bell",
                                        label: "In-app Notifications",
                                        showChevron: true,
                                        trailing: {
                                            MiraRectangularSwitch(isOn: state.inAppNotificationsEnabled) { enabled in
                                                state.eventSink(ProfileEventToggleInAppNotifications(enabled: enabled))
                                            }
                                        },
                                        onClick: {
                                            state.eventSink(ProfileEventNotifications())
                                        }
                                    )
                                    ProfileDivider()
                                    ProfileMenuRow(icon: "globe", label: "Language", onClick: {})
                                    ProfileDivider()
                                    ProfileMenuRowWithTrailing(
                                        icon: "gearshape",
                                        label: "Light/Dark Mode",
                                        showChevron: false,
                                        trailing: {
                                            MiraRectangularSwitch(isOn: state.currentTheme == AppTheme.dark) { isDark in
                                                state.eventSink(ProfileEventSetTheme(theme: isDark ? AppTheme.dark : AppTheme.light))
                                            }
                                        },
                                        onClick: {}
                                    )
                                }
                            }

                            MiraSectionHeader(title: "Support")
                            ProfileCard {
                                ProfileMenuRow(
                                    icon: "rectangle.portrait.and.arrow.right",
                                    label: "Log out",
                                    tint: MiraTheme.error,
                                    showChevron: false
                                ) {
                                    state.eventSink(ProfileEventLogout())
                                }
                            }

                            Spacer().frame(height: 32)
                        }
                    }
                }
            }
            .background(MiraTheme.background.ignoresSafeArea())
        }
    }
}

// MARK: - Building blocks (1:1 with the private composables in ProfileScreen.kt)

private struct ProfileCard<Content: View>: View {
    @ViewBuilder let content: Content
    var body: some View {
        content
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(MiraTheme.surface)
            .cornerRadius(MiraTheme.radiusProfileCard)
            .padding(.horizontal, MiraTheme.spacingMedium)
    }
}

private struct ProfileDivider: View {
    var body: some View {
        Divider()
            .padding(.horizontal, MiraTheme.spacingMedium)
            .opacity(0.5)
    }
}

private struct ProfileHeader: View {
    let name: String
    let email: String
    let avatarUrl: String?
    let onEditProfile: () -> Void

    var body: some View {
        HStack(spacing: MiraTheme.spacingMedium) {
            ZStack {
                Circle().fill(MiraTheme.surfaceVariant).frame(width: MiraTheme.profileAvatarSize, height: MiraTheme.profileAvatarSize)
                if let url = avatarUrl, !url.isEmpty {
                    let resolvedUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: url)
                    AsyncImage(url: URL(string: resolvedUrl ?? "")) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable().aspectRatio(contentMode: .fill)
                        case .failure(let error):
                            let _ = print("ProfileHeader: Image load failed for \(resolvedUrl ?? "nil"): \(error.localizedDescription)")
                            MiraTheme.surfaceVariant
                        case .empty:
                            MiraTheme.surfaceVariant
                        @unknown default:
                            MiraTheme.surfaceVariant
                        }
                    }
                    .frame(width: MiraTheme.profileAvatarSize, height: MiraTheme.profileAvatarSize)
                    .clipShape(Circle())
                    .onAppear {
                        if let resolvedUrl = resolvedUrl {
                            print("ProfileHeader: Attempting to load \(resolvedUrl)")
                        }
                    }
                } else {
                    Image(systemName: "person")
                        .font(.system(size: MiraTheme.iconSizeIntermediate))
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                }
            }

            VStack(alignment: .leading, spacing: MiraTheme.spacingTiny) {
                Text(name.isEmpty ? "Your name" : name)
                    .font(MiraType.titleLarge.weight(.bold))
                Text(email)
                    .font(MiraType.bodySmall)
                    .foregroundColor(MiraTheme.onSurfaceVariant)
                Button(action: onEditProfile) {
                    HStack(spacing: 4) {
                        Image(systemName: "pencil")
                            .font(.system(size: 12))
                            .foregroundColor(MiraTheme.primary)
                        Text("Edit Profile")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(MiraTheme.primary)
                    }
                }
                .buttonStyle(.plain)
            }
            Spacer()
        }
        .padding(MiraTheme.spacingMedium)
    }
}

// Simple version without trailing view
private struct ProfileMenuRow: View {
    let icon: String
    let label: String
    var trailingText: String? = nil
    var badgeCount: Int = 0
    var tint: Color = MiraTheme.onSurface
    var showChevron: Bool = true
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack {
                HStack(spacing: MiraTheme.spacingMedium) {
                    Image(systemName: icon)
                        .font(.system(size: 22))
                        .foregroundColor(tint)
                        .frame(width: MiraTheme.iconSizeMedium)
                    Text(label)
                        .font(MiraType.bodyLarge.weight(.medium))
                        .foregroundColor(tint)
                }
                Spacer()
                HStack(spacing: MiraTheme.spacingSmall) {
                    if badgeCount > 0 {
                        Text("\(badgeCount)")
                            .font(MiraType.labelSmall)
                            .bold()
                            .foregroundColor(MiraTheme.onPrimary)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(MiraTheme.primary)
                            .clipShape(Capsule())
                    }
                    if let trailingText = trailingText {
                        Text(trailingText)
                            .font(MiraType.bodyMedium)
                            .foregroundColor(MiraTheme.onSurfaceVariant)
                    }
                    if showChevron {
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12))
                            .foregroundColor(MiraTheme.onSurfaceVariant)
                    }
                }
            }
            .padding(.horizontal, MiraTheme.spacingMedium)
            .padding(.vertical, MiraTheme.spacingMedium)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

// Generic version with trailing view for switches and custom views
private struct ProfileMenuRowWithTrailing<Trailing: View>: View {
    let icon: String
    let label: String
    var trailingText: String? = nil
    var badgeCount: Int = 0
    var tint: Color = MiraTheme.onSurface
    var showChevron: Bool = true
    @ViewBuilder let trailing: Trailing
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack {
                HStack(spacing: MiraTheme.spacingMedium) {
                    Image(systemName: icon)
                        .font(.system(size: 22))
                        .foregroundColor(tint)
                        .frame(width: MiraTheme.iconSizeMedium)
                    Text(label)
                        .font(MiraType.bodyLarge.weight(.medium))
                        .foregroundColor(tint)
                }
                Spacer()
                HStack(spacing: MiraTheme.spacingSmall) {
                    if badgeCount > 0 {
                        Text("\(badgeCount)")
                            .font(MiraType.labelSmall)
                            .bold()
                            .foregroundColor(MiraTheme.onPrimary)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(MiraTheme.primary)
                            .clipShape(Capsule())
                    }
                    if let trailingText = trailingText {
                        Text(trailingText)
                            .font(MiraType.bodyMedium)
                            .foregroundColor(MiraTheme.onSurfaceVariant)
                    }
                    trailing
                    if showChevron {
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12))
                            .foregroundColor(MiraTheme.onSurfaceVariant)
                    }
                }
            }
            .padding(.horizontal, MiraTheme.spacingMedium)
            .padding(.vertical, MiraTheme.spacingMedium)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
