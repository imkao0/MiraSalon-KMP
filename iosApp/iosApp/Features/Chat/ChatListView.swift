import SwiftUI
import ComposeApp

/// Mirrors Android `ChatListScreen.kt` (route ChatRoute.ChatList / BottomNavKey.Chat).
struct ChatListView: View {
    @EnvironmentObject private var navigation: CircuitNavigation

    var body: some View {
        CircuitView(screen: ChatRouteChatList(), navigator: navigation) { (state: ChatListState) in
            VStack(spacing: 0) {
                if let content = state as? ChatListStateContent {
                    contentView(content)
                } else if let error = state as? ChatListStateError {
                    Spacer()
                    Text(error.message)
                        .font(.subheadline)
                        .foregroundColor(MiraTheme.error)
                    Spacer()
                } else {
                    Spacer()
                    ProgressView()
                    Spacer()
                }
            }
            .background(MiraTheme.surface.ignoresSafeArea())
        }
    }

    @ViewBuilder
    private func contentView(_ state: ChatListStateContent) -> some View {
        MiraTopBar {
            Text("Chats")
                .font(.headline)
                .bold()
        } actions: {
            HStack(spacing: 8) {
                MiraAvatar(url: state.currentUserAvatarUrl, size: 56)
                    .onTapGesture { state.eventSink(ChatListEventOpenProfile()) }

                Menu {
                    Button(role: .destructive) {
                        state.eventSink(ChatListEventDeleteHistory())
                    } label: {
                        Label("Delete History", systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .foregroundColor(MiraTheme.onSurface)
                        .padding(8)
                }
            }
            .padding(.trailing, 8)
        }

        ScrollView {
            if state.chats.isEmpty {
                EmptyStateView(
                    message: "No messages yet",
                    description: "Start a conversation with our specialists to get professional advice and personalized care.",
                    icon: "bubble.left.and.bubble.right"
                )
                .padding(.top, 100)
            } else {
                LazyVStack(spacing: 0) {
                    Spacer().frame(height: 16)
                    // Quick access row
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 16) {
                            QuickAccessSearchItem {
                                state.eventSink(ChatListEventOpenSearch(query: ""))
                            }
                            ForEach(state.quickAccessContacts, id: \.id) { contact in
                                QuickAccessContactItem(contact: contact) {
                                    state.eventSink(ChatListEventOpenQuickContact(contactId: contact.id))
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                    .padding(.top, 8)

                    Spacer().frame(height: 8)

                    ForEach(state.chats, id: \.id) { chat in
                        ChatRow(item: chat) {
                            state.eventSink(ChatListEventOpenChat(chatId: chat.id))
                        }
                        Divider().padding(.leading, 80).opacity(0.6)
                    }

                    Spacer().frame(height: 16)
                }
            }
        }
    }
}

private struct QuickAccessSearchItem: View {
    let onClick: () -> Void
    var body: some View {
        Button(action: onClick) {
            VStack(spacing: 6) {
                ZStack {
                    Circle().fill(MiraTheme.surfaceVariant).frame(width: 72, height: 72)
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 28))
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                }
                Text("Search")
                    .font(.caption2)
                    .foregroundColor(MiraTheme.onSurfaceVariant)
            }
        }
        .buttonStyle(.plain)
    }
}

private struct QuickAccessContactItem: View {
    let contact: QuickAccessContact
    let onClick: () -> Void
    var body: some View {
        Button(action: onClick) {
            VStack(spacing: 6) {
                MiraAvatar(url: contact.avatarUrl, size: 72)
                Text(contact.role)
                    .font(.caption2)
                    .foregroundColor(MiraTheme.onSurface)
                    .lineLimit(1)
                    .frame(width: 72)
            }
        }
        .buttonStyle(.plain)
    }
}

private struct ChatRow: View {
    let item: ChatItem
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 12) {
                ZStack(alignment: .bottomTrailing) {
                    MiraAvatar(url: item.avatarUrl, size: 52)
                    if item.isOnline {
                        Circle()
                            .fill(MiraTheme.success)
                            .frame(width: 13, height: 13)
                            .overlay(Circle().stroke(MiraTheme.surface, lineWidth: 1.5))
                    }
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text("Specialist")
                        .font(.body)
                        .fontWeight(.semibold)
                        .foregroundColor(MiraTheme.onSurface)
                        .lineLimit(1)
                    Text(item.lastMessage)
                        .font(.subheadline)
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                        .lineLimit(1)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 4) {
                    Text(item.timestamp)
                        .font(.caption2)
                        .foregroundColor(item.unreadCount > 0 ? MiraTheme.primary : MiraTheme.onSurfaceVariant)
                    if item.unreadCount > 0 {
                        Text("\(item.unreadCount)")
                            .font(.caption2.bold())
                            .foregroundColor(MiraTheme.error)
                    } else {
                        Image(systemName: "checkmark")
                            .font(.system(size: 14))
                            .foregroundColor(item.deliveryStatus == .read ? MiraTheme.success : MiraTheme.onSurfaceVariant)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
