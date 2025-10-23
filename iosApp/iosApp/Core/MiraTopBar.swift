import SwiftUI

/**
 * Top bars — pixel-faithful mirrors of
 * `core:designsystem/components/MiraTopAppBar.kt`
 * (MiraTopAppBar, MiraCenterAlignedTopAppBar, MiraBackButton).
 *
 * Material3 `TopAppBar`: leading navigation icon, left-aligned bold
 * title (titleLarge 22pt), trailing actions. `CenterAlignedTopAppBar`
 * centers the title instead.
 */

// MARK: - MiraTopAppBar (title string overload)
struct MiraTopAppBar<Actions: View>: View {
    let title: String
    var onBackClick: (() -> Void)? = nil
    var backgroundColor: Color = MiraTheme.surface
    let actions: Actions

    init(
        title: String,
        onBackClick: (() -> Void)? = nil,
        backgroundColor: Color = MiraTheme.surface,
        @ViewBuilder actions: () -> Actions = { EmptyView() }
    ) {
        self.title = title
        self.onBackClick = onBackClick
        self.backgroundColor = backgroundColor
        self.actions = actions()
    }

    var body: some View {
        HStack(spacing: MiraTheme.spacingSmall) {
            if let onBackClick {
                MiraBackButton(onClick: onBackClick)
            }

            Text(title)
                .font(MiraType.titleLarge.weight(.bold))
                .foregroundColor(MiraTheme.textPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)

            actions
        }
        .padding(.horizontal, MiraTheme.spacingMedium)
        .padding(.vertical, MiraTheme.spacingDefault)
        .frame(maxWidth: .infinity)
        .background(backgroundColor)
    }
}

// MARK: - MiraTopAppBar (custom title overload, legacy generic form)
/// Free-form variant used by the feature screens: custom title slot,
/// leading navigation slot and trailing actions slot.
struct MiraTopBar<Title: View, Navigation: View, Actions: View>: View {
    let title: Title
    let navigationIcon: Navigation
    let actions: Actions
    let backgroundColor: Color

    init(
        @ViewBuilder title: () -> Title,
        @ViewBuilder navigationIcon: () -> Navigation = { EmptyView() },
        @ViewBuilder actions: () -> Actions = { EmptyView() },
        backgroundColor: Color = MiraTheme.surface
    ) {
        self.title = title()
        self.navigationIcon = navigationIcon()
        self.actions = actions()
        self.backgroundColor = backgroundColor
    }

    var body: some View {
        HStack(spacing: MiraTheme.spacingDefault) {
            navigationIcon

            title
                .frame(maxWidth: .infinity, alignment: .leading)

            actions
        }
        .padding(.horizontal, MiraTheme.spacingLarge)
        .padding(.vertical, MiraTheme.spacingDefault)
        .background(backgroundColor)
    }
}

// MARK: - MiraCenterAlignedTopAppBar
struct MiraCenterAlignedTopAppBar<Actions: View>: View {
    let title: String
    var onBackClick: (() -> Void)? = nil
    var backgroundColor: Color = MiraTheme.surface
    let actions: Actions

    init(
        title: String,
        onBackClick: (() -> Void)? = nil,
        backgroundColor: Color = MiraTheme.surface,
        @ViewBuilder actions: () -> Actions = { EmptyView() }
    ) {
        self.title = title
        self.onBackClick = onBackClick
        self.backgroundColor = backgroundColor
        self.actions = actions()
    }

    var body: some View {
        ZStack {
            Text(title)
                .font(MiraType.titleLarge.weight(.bold))
                .foregroundColor(MiraTheme.textPrimary)
                .frame(maxWidth: .infinity)

            HStack(spacing: MiraTheme.spacingSmall) {
                if let onBackClick {
                    MiraBackButton(onClick: onBackClick)
                }
                Spacer()
                actions
            }
        }
        .padding(.horizontal, MiraTheme.spacingMedium)
        .padding(.vertical, MiraTheme.spacingDefault)
        .frame(maxWidth: .infinity)
        .background(backgroundColor)
    }
}

// MARK: - MiraBackButton (Icons.AutoMirrored.Filled.ArrowBack)
struct MiraBackButton: View {
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            Image(systemName: "arrow.left")
                .font(.system(size: 20, weight: .medium))
                .foregroundColor(MiraTheme.textPrimary)
                .frame(width: 48, height: 48) // Material IconButton touch target
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Back")
    }
}

// MARK: - Previews
#Preview("Top Bars") {
    VStack(spacing: 20) {
        MiraTopAppBar(title: "Standard Title")
        MiraTopAppBar(title: "With Back", onBackClick: {})
        MiraCenterAlignedTopAppBar(title: "Center Title", onBackClick: {})
        MiraTopBar {
            Text("Custom").bold()
        } actions: {
            Image(systemName: "bell")
        }
    }
}
