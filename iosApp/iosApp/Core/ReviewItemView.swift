import SwiftUI
import ComposeApp

struct ReviewItemView: View {
    let userName: String
    let userAvatarUrl: String?
    let rating: Int
    let comment: String?
    let date: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .center, spacing: 12) {
                // Avatar
                MiraAvatar(url: userAvatarUrl, size: 40)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(userName)
                        .font(.subheadline)
                        .bold()
                        .foregroundColor(MiraTheme.onSurface)
                    
                    Text(date)
                        .font(.caption2)
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                }
                
                Spacer()
                
                // Stars
                HStack(spacing: 2) {
                    ForEach(0..<5) { index in
                        Image(systemName: "star.fill")
                            .font(.system(size: 12))
                            .foregroundColor(index < rating ? .yellow : MiraTheme.surfaceVariant)
                    }
                }
            }
            
            if let comment = comment, !comment.isEmpty {
                Text(comment)
                    .font(.caption)
                    .foregroundColor(MiraTheme.onSurface)
                    .lineLimit(nil)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.vertical, 12)
    }
}
