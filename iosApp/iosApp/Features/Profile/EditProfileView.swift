import SwiftUI
import ComposeApp
import PhotosUI
import UIKit

/// Mirrors Android `EditProfileScreen.kt`.
struct EditProfileView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    @State private var selectedPhoto: PhotosPickerItem?
    @FocusState private var focusedField: Field?

    enum Field {
        case fullName, phoneNumber
    }

    var body: some View {
        CircuitView(screen: ProfileRouteEditProfile(), navigator: navigation) { (state: EditProfileState) in
            VStack(spacing: 0) {
                MiraTopBar {
                    Text("Edit Profile")
                        .font(.headline)
                        .bold()
                } navigationIcon: {
                    Button { state.eventSink(EditProfileEventBack()) } label: {
                        Image(systemName: "chevron.left")
                            .foregroundColor(MiraTheme.onBackground)
                    }
                } actions: {
                    Button {
                        state.eventSink(EditProfileEventSave())
                    } label: {
                        if state.isSaving {
                            ProgressView().frame(width: 20, height: 20)
                        } else {
                            Text("Save")
                                .bold()
                                .foregroundColor(MiraTheme.primary)
                        }
                    }
                    .disabled(state.isSaving || state.fullName.trimmingCharacters(in: .whitespaces).isEmpty)
                }

                if state.isLoading {
                    Spacer()
                    ProgressView()
                    Spacer()
                } else {
                    ScrollView {
                        VStack(spacing: 0) {
                            if let error = state.saveError {
                                Text(error)
                                    .font(.subheadline)
                                    .foregroundColor(MiraTheme.error)
                                    .padding(.bottom, 16)
                            }

                            // Avatar
                            ZStack {
                                Circle().fill(MiraTheme.surfaceVariant).frame(width: 120, height: 120)
                                if let url = state.avatarUrl, !url.isEmpty {
                                    let resolvedUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: url)
                                    AsyncImage(url: URL(string: resolvedUrl ?? "")) { image in
                                        image.resizable().aspectRatio(contentMode: .fill)
                                    } placeholder: {
                                        MiraTheme.surfaceVariant
                                    }
                                    .frame(width: 120, height: 120)
                                    .clipShape(Circle())
                                } else {
                                    Image(systemName: "person")
                                        .font(.system(size: 60))
                                        .foregroundColor(MiraTheme.onSurfaceVariant)
                                }
                            }

                            Spacer().frame(height: 12)

                            // Update photo button
                            PhotosPicker(selection: $selectedPhoto, matching: .images) {
                                HStack(spacing: 8) {
                                    if state.isUploadingImage {
                                        ProgressView().frame(width: 18, height: 18)
                                    } else {
                                        Image(systemName: "photo")
                                            .font(.system(size: 18))
                                    }
                                    Text(state.isUploadingImage ? "Uploading..." : "Update photo")
                                        .font(.subheadline)
                                }
                                .foregroundColor(MiraTheme.onSurface)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 8)
                                .background(MiraTheme.surfaceVariant.opacity(0.5))
                                .cornerRadius(12)
                            }
                            .disabled(state.isUploadingImage)
                            .onChange(of: selectedPhoto) { item in
                                Task {
                                    guard let item = item else { return }
                                    if let data = try? await item.loadTransferable(type: Data.self),
                                       let uiImage = UIImage(data: data),
                                       let resizedImage = uiImage.downscaled(),
                                       let resizedData = resizedImage.jpegData(compressionQuality: 0.7) {
                                        state.eventSink(EditProfileEventImageSelected(bytes: KotlinByteArray.from(data: resizedData)))
                                    }
                                }
                            }

                            Spacer().frame(height: 32)

                            EditProfileTextField(
                                label: "Full Name",
                                text: Binding(
                                    get: { state.fullName },
                                    set: { state.eventSink(EditProfileEventFullNameChanged(value: $0)) }
                                ),
                                submitLabel: .next,
                                onSubmit: { focusedField = .phoneNumber }
                            )
                            .focused($focusedField, equals: .fullName)

                            EditProfileTextField(
                                label: "Phone Number",
                                text: Binding(
                                    get: { state.phoneNumber },
                                    set: { state.eventSink(EditProfileEventPhoneChanged(value: $0)) }
                                ),
                                keyboard: .phonePad,
                                submitLabel: .done,
                                onSubmit: { focusedField = nil }
                            )
                            .focused($focusedField, equals: .phoneNumber)

                            EditProfileTextField(
                                label: "Email Address",
                                text: .constant(state.email),
                                enabled: false
                            )

                            // Gender
                            VStack(alignment: .leading, spacing: 8) {
                                Text("Gender")
                                    .font(.subheadline)
                                    .bold()
                                HStack(spacing: 16) {
                                    genderRow(state: state, gender: .female, label: "Female")
                                    genderRow(state: state, gender: .male, label: "Male")
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.bottom, 20)

                            Spacer().frame(height: 40)
                        }
                        .padding(16)
                    }
                }
            }
            .background(MiraTheme.background.ignoresSafeArea())
        }
    }

    private func genderRow(state: EditProfileState, gender: Gender, label: String) -> some View {
        Button {
            state.eventSink(EditProfileEventGenderSelected(value: gender))
        } label: {
            HStack(spacing: 4) {
                MiraRadioButton(isSelected: state.gender == gender) {
                    state.eventSink(EditProfileEventGenderSelected(value: gender))
                }
                Text(label).font(.body).foregroundColor(MiraTheme.onSurface)
            }
        }
        .buttonStyle(.plain)
    }
}

private struct EditProfileTextField: View {
    let label: String
    @Binding var text: String
    var placeholder: String = ""
    var enabled: Bool = true
    var keyboard: UIKeyboardType = .default
    var submitLabel: SubmitLabel = .next
    var onSubmit: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if !label.isEmpty {
                Text(label)
                    .font(.subheadline)
                    .bold()
            }
            TextField(placeholder, text: $text)
                .keyboardType(keyboard)
                .submitLabel(submitLabel)
                .onSubmit(onSubmit)
                .disabled(!enabled)
                .padding(.horizontal, 16)
                .frame(height: 56)
                .frame(maxWidth: .infinity)
                .background(MiraTheme.surfaceVariant.opacity(enabled ? 0.5 : 0.3))
                .cornerRadius(12)
        }
        .frame(maxWidth: .infinity)
        .padding(.bottom, 20)
    }
}

extension UIImage {
    func downscaled(maxDimension: CGFloat = 512) -> UIImage? {
        if size.width <= maxDimension && size.height <= maxDimension {
            return self
        }

        let aspectRatio = size.width / size.height
        let newSize: CGSize
        if size.width > size.height {
            newSize = CGSize(width: maxDimension, height: maxDimension / aspectRatio)
        } else {
            newSize = CGSize(width: maxDimension * aspectRatio, height: maxDimension)
        }

        let renderer = UIGraphicsImageRenderer(size: newSize)
        return renderer.image { _ in
            self.draw(in: CGRect(origin: .zero, size: newSize))
        }
    }
}
