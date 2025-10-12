import SwiftUI
import CoreImage.CIFilterBuiltins

/**
 * Native iOS QR code generator (CoreImage), the SwiftUI twin of the Android
 * `rememberQrCodePainter` used in EReceiptScreen.kt. Black code on white, no logo.
 */
struct MiraQRCode: View {
    let data: String
    var size: CGFloat = 140

    @State private var qrImage: UIImage?

    var body: some View {
        Group {
            if let image = qrImage {
                Image(uiImage: image)
                    .interpolation(.none)
                    .resizable()
                    .scaledToFit()
            } else {
                Color.clear
            }
        }
        .frame(width: size, height: size)
        .onAppear { generateIfNeeded() }
        .onChange(of: data) { _, _ in generateIfNeeded() }
    }

    private func generateIfNeeded() {
        guard !data.isEmpty else { return }
        
        // Move to background thread to avoid hanging UI
        DispatchQueue.global(qos: .userInitiated).async {
            let context = CIContext()
            let filter = CIFilter.qrCodeGenerator()
            filter.setValue(Data(data.utf8), forKey: "inputMessage")
            filter.setValue("M", forKey: "inputCorrectionLevel")
            
            if let output = filter.outputImage {
                let scaled = output.transformed(by: CGAffineTransform(scaleX: 10, y: 10))
                if let cgImage = context.createCGImage(scaled, from: scaled.extent) {
                    let image = UIImage(cgImage: cgImage)
                    DispatchQueue.main.async {
                        self.qrImage = image
                    }
                }
            }
        }
    }
}
