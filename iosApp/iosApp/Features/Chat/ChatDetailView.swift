import SwiftUI
import ComposeApp

/// Mirrors Android `ChatDetailScreen.kt`.
struct ChatDetailView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let screen: ChatRouteChatDetail

    @State private var text = ""

    var body: some View {
        CircuitView(screen: screen, navigator: navigation) { (state: ChatDetailState) in
            VStack(spacing: 0) {
                ChatDetailTopBar(
                    participantName: state.participantName ?? state.conversationId,
                    participantRole: state.participantRole ?? "Specialist",
                    participantAvatarUrl: state.participantAvatarUrl,
                    isOnline: state.isOnline,
                    onBack: { navigation.pop(result: nil) },
                    onTap: { state.eventSink(ChatDetailEventHeaderClicked()) }
                )

                // Messages grouped by date
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: MiraTheme.spacingMedium, pinnedViews: [.sectionHeaders]) {
                            ForEach(groupedByDate(state.messages), id: \.0) { dateKey, messages in
                                Section(header: 
                                    DateSeparator(text: MiraDateFormat.separator(epochSeconds: messages.first?.timestampEpochSeconds ?? 0))
                                        .background(MiraTheme.background)
                                ) {
                                    ForEach(messages, id: \.id) { message in
                                        MessageItem(
                                            message: message,
                                            participantName: state.participantName ?? "Specialist",
                                            participantAvatarUrl: state.participantAvatarUrl,
                                            currentUserName: state.currentUserName ?? "Me",
                                            currentUserAvatarUrl: state.currentUserAvatarUrl
                                        )
                                    }
                                }
                            }
                        }
                        .padding(.horizontal, MiraTheme.spacingMedium)
                        .padding(.vertical, MiraTheme.spacingLarge)
                    }
                }
                .background(MiraTheme.background)

                // Input bar
                ChatInputBar(text: $text) {
                    let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
                    if !trimmed.isEmpty {
                        state.eventSink(ChatDetailEventSendMessage(text: trimmed))
                        text = ""
                    }
                }
            }
            .background(MiraTheme.background.ignoresSafeArea())
        }
    }

    // Group messages by calendar day, preserving order
    private func groupedByDate(_ messages: [ChatMessage_]) -> [(String, [ChatMessage_])] {
        var result: [(String, [ChatMessage_])] = []
        var order: [String] = []
        var map: [String: [ChatMessage_]] = [:]
        let cal = Calendar.current
        for m in messages {
            let day = cal.startOfDay(for: Date(timeIntervalSince1970: TimeInterval(m.timestampEpochSeconds)))
            let key = "\(day.timeIntervalSince1970)"
            if map[key] == nil { order.append(key) }
            map[key, default: []].append(m)
        }
        for key in order { result.append((key, map[key] ?? [])) }
        return result
    }
}

private struct DateSeparator: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.caption)
            .fontWeight(.medium)
            .foregroundColor(Color.gray.opacity(0.6))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
    }
}

private struct MessageItem: View {
    let message: ChatMessage_
    let participantName: String
    let participantAvatarUrl: String?
    let currentUserName: String
    let currentUserAvatarUrl: String?

    private var isMe: Bool { message.senderId == "me" }
    private var name: String { isMe ? currentUserName : participantName }
    private var avatarUrl: String? { isMe ? currentUserAvatarUrl : participantAvatarUrl }

    var body: some View {
        VStack(alignment: isMe ? .trailing : .leading, spacing: MiraTheme.spacingTiny) {
            HStack(spacing: MiraTheme.spacingSmall) {
                if !isMe {
                    MiraAvatar(url: avatarUrl, size: MiraTheme.iconSizeIntermediate)
                    Text(name).font(.subheadline).bold().foregroundColor(MiraTheme.onSurface)
                } else {
                    Text(name).font(.subheadline).bold().foregroundColor(MiraTheme.onSurface)
                    MiraAvatar(url: avatarUrl, size: MiraTheme.iconSizeIntermediate)
                }
            }

            VStack(alignment: isMe ? .trailing : .leading, spacing: MiraTheme.spacingTiny) {
                Text(message.text)
                    .font(.body)
                    .foregroundColor(isMe ? MiraTheme.onPrimaryContainer : MiraTheme.onSurfaceVariant)
                    .padding(MiraTheme.spacingMedium)
                    .background(isMe ? MiraTheme.primaryContainer : MiraTheme.surfaceVariant)
                    .cornerRadius(MiraTheme.radiusSmall, corners: isMe ? [.topLeft, .bottomLeft, .bottomRight] : [.topRight, .bottomLeft, .bottomRight])
                    .shadow(color: .black.opacity(0.05), radius: 1, x: 0, y: 1)

                HStack(spacing: 4) {
                    Text(MiraDateFormat.time(epochSeconds: message.timestampEpochSeconds))
                        .font(.caption2)
                        .foregroundColor(.gray)
                    
                    if isMe {
                        WhatsAppStatusIcon(status: message.status)
                    }
                }
            }
            .frame(maxWidth: UIScreen.main.bounds.width * 0.85, alignment: isMe ? .trailing : .leading)
        }
        .frame(maxWidth: .infinity, alignment: isMe ? .trailing : .leading)
    }
}

private struct DoubleCheckmark: View {
    let color: Color
    var body: some View {
        ZStack {
            Image(systemName: "checkmark")
                .offset(x: -3)
            Image(systemName: "checkmark")
        }
        .font(.system(size: 8, weight: .bold))
        .foregroundColor(color)
    }
}

private struct WhatsAppStatusIcon: View {
    let status: MessageStatus
    
    var body: some View {
        switch status {
        case .sending:
            Image(systemName: "clock")
                .font(.system(size: 10))
                .foregroundColor(.gray.opacity(0.5))
        case .sent:
            Image(systemName: "checkmark")
                .font(.system(size: 10, weight: .bold))
                .foregroundColor(.gray.opacity(0.5))
        case .delivered:
            DoubleCheckmark(color: .gray.opacity(0.5))
        case .read:
            DoubleCheckmark(color: Color(red: 52/255, green: 183/255, blue: 241/255))
        default:
            EmptyView()
        }
    }
}

private struct ChatInputBar: View {
    @Binding var text: String
    let onSend: () -> Void

    var body: some View {
        HStack(spacing: MiraTheme.spacingDefault) {
            HStack(spacing: MiraTheme.spacingDefault) {
                TextField("Type here..", text: $text)
                    .font(.system(size: 15))
                    .submitLabel(.send)
                    .onSubmit(onSend)
                Image(systemName: "paperclip")
                    .foregroundColor(MiraTheme.onSurfaceVariant)
                    .font(.system(size: 18))
                Image(systemName: "camera")
                    .foregroundColor(MiraTheme.onSurfaceVariant)
                    .font(.system(size: 18))
            }
            .padding(.horizontal, MiraTheme.spacingMedium)
            .padding(.vertical, 10)
            .background(MiraTheme.surfaceVariant)
            .cornerRadius(MiraTheme.radiusExtraLarge)

            Button(action: onSend) {
                Image(systemName: text.trimmingCharacters(in: .whitespaces).isEmpty ? "mic" : "paperplane.fill")
                    .foregroundColor(text.trimmingCharacters(in: .whitespaces).isEmpty ? MiraTheme.onSurfaceVariant : MiraTheme.onPrimary)
                    .frame(width: MiraTheme.iconSizeLarge, height: MiraTheme.iconSizeLarge)
                    .background(text.trimmingCharacters(in: .whitespaces).isEmpty ? MiraTheme.surfaceVariant : MiraTheme.primary)
                    .clipShape(Circle())
            }
        }
        .padding(MiraTheme.spacingMedium)
        .background(Color.clear)
    }
}

private struct ChatDetailTopBar: View {
    let participantName: String
    let participantRole: String
    let participantAvatarUrl: String?
    let isOnline: Bool
    let onBack: () -> Void
    let onTap: () -> Void

    var body: some View {
        HStack(spacing: MiraTheme.spacingSmall) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.headline)
                    .foregroundColor(MiraTheme.onSurface)
            }

            HStack(spacing: MiraTheme.spacingSmall) {
                MiraAvatar(url: participantAvatarUrl, size: 40)
                VStack(alignment: .leading, spacing: 2) {
                    Text(participantName)
                        .font(.headline)
                        .bold()
                        .foregroundColor(MiraTheme.onSurface)
                    HStack(spacing: 4) {
                        Text(participantRole)
                            .font(.caption)
                            .foregroundColor(MiraTheme.onSurfaceVariant)
                        Circle()
                            .fill(isOnline ? Color.green : Color.gray)
                            .frame(width: 8, height: 8)
                    }
                }
            }
            .contentShape(Rectangle())
            .onTapGesture(perform: onTap)

            Spacer()
        }
        .padding(.horizontal, MiraTheme.spacingMedium)
        .padding(.vertical, MiraTheme.spacingSmall)
        .background(MiraTheme.surface)
    }
}
