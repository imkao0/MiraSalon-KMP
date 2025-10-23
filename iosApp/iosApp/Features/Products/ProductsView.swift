import SwiftUI
import ComposeApp

struct ProductsView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    
    var body: some View {
        ExploreCategoriesView()
    }
}
