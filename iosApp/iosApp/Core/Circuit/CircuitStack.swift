import SwiftUI
import ComposeApp

/**
 * Renders a Circuit screen graph natively in SwiftUI for a single tab.
 *
 * Each tab owns its `CircuitNavigation` store (injected via the environment),
 * so navigation never bleeds across tabs. Unknown screens fail loudly with a
 * logged, identifiable error instead of a silent dead-end.
 */
struct CircuitStack: View {
    @EnvironmentObject private var navigation: CircuitNavigation

    var body: some View {
        NavigationStack(path: navigation.stackBinding) {
            renderScreen(navigation.root.base)
                .navigationDestination(for: AnyScreen.self) { wrapper in
                    renderScreen(wrapper.base)
                        .toolbar(.hidden, for: .tabBar)
                }
        }
    }

    @ViewBuilder
    private func renderScreen(_ screen: any Circuit_runtime_screenScreen) -> some View {
        switch screen {
        // --- Auth ---
        case let screen as AuthRouteWelcome:
            WelcomeView(screen: screen)
        case let screen as AuthRouteLogin:
            LoginView(screen: screen)
        case let screen as AuthRouteRegister:
            RegisterView(screen: screen)

        // --- Home & Discovery ---
        case is BottomNavKeyHome:
            HomeView()
        case is BottomNavKeyDiscover:
            ExploreCategoriesView()
        case let services as ServiceRouteServices:
            ServicesDiscoveryView(categoryId: services.categoryId)
        case let detail as ServiceRouteServiceDetail:
            ServiceDetailView(serviceId: detail.serviceId)
        case is ProductRouteProducts:
            ProductsView()
        case is ProductRouteExploreCategories:
            ExploreCategoriesView()
        case let detail as ProductRouteProductDetail:
            ProductDetailView(productId: detail.productId)

        // --- Specialists ---
        case is SpecialistRouteSpecialists:
            SpecialistsView()
        case let detail as SpecialistRouteSpecialistDetail:
            SpecialistDetailView(specialistId: detail.specialistId)

        // --- Booking Flow ---
        case let screen as BookingRouteBooking:
            BookingView(screen: screen)
        case let checkout as BookingRouteAppointmentCheckout:
            CheckoutView(screen: checkout)
        case let success as BookingRoutePaymentSuccess:
            PaymentSuccessView(appointmentId: success.appointmentId)
        case let receipt as BookingRouteEReceipt:
            EReceiptView(appointmentId: receipt.appointmentId)

        // --- My Bookings & Appointments ---
        case is BottomNavKeyBooking:
            MyBookingsView()
        case is AppointmentRouteAppointments:
            AppointmentsView()
        case let detail as AppointmentRouteAppointmentDetail:
            AppointmentDetailView(appointmentId: detail.appointmentId)

        // --- Notifications ---
        case let screen as NotificationRouteNotifications:
            NotificationsView(screen: screen)

        // --- Cart ---
        case is BottomNavKeyCart:
            CartView()
        case is CartRouteCart:
            CartView()
        case let checkout as CartRouteCheckout:
            CartCheckoutView(screen: checkout)
        case let screen as CartRouteOrders:
            OrdersView(screen: screen)
        case let detail as CartRouteOrderDetail:
            OrderDetailView(screen: detail)
        case let success as CartRoutePaymentSuccess:
            CartPaymentSuccessView(screen: success)

        // --- Chat ---
        case is BottomNavKeyChat:
            ChatListView()
        case is ChatRouteChatList:
            ChatListView()
        case let detail as ChatRouteChatDetail:
            ChatDetailView(screen: detail)

        // --- Profile ---
        case is BottomNavKeyProfile:
            ProfileView()
        case is ProfileRouteEditProfile:
            EditProfileView()
        case is ProfileRouteAddresses:
            AddressListView()
        case let form as ProfileRouteAddressForm:
            AddressFormView(addressId: form.addressId)
        case is ProfileRouteFavourites:
            FavouritesView()

        default:
            UnknownScreenView(screen: screen)
        }
    }
}

/** Loud, logged fallback for a screen with no native renderer. */
struct UnknownScreenView: View {
    let screen: any Circuit_runtime_screenScreen

    var body: some View {
        ContentUnavailableView(
            "Unsupported Screen",
            systemImage: "exclamationmark.triangle",
            description: Text("This screen has no native renderer yet.\nScreen: \(String(describing: screen))")
        )
        .onAppear {
            print("CircuitStack: no renderer for screen \(String(describing: screen))")
            print("CircuitStack: screen type \(type(of: screen))")
        }
    }
}

/**
 * Hashable identity for a Kotlin screen, bridged through the Kotlin object's
 * own `equals`/`hashCode` instead of `String(describing:)` (which is not a
 * stable identity for ObjC-exported Kotlin objects).
 */
struct AnyScreen: Hashable {
    let base: any Circuit_runtime_screenScreen

    static func == (lhs: AnyScreen, rhs: AnyScreen) -> Bool {
        KotlinObjectEquality.shared.areEqual(a: lhs.base, b: rhs.base)
    }

    func hash(into hasher: inout Hasher) {
        // KotlinObjectEquality.hashCode(of:) returns Int32; convert to Swift Int.
        hasher.combine(Int(KotlinObjectEquality.shared.hashCode(of: base)))
    }
}
