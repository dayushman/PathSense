import PathSenseCore
import SwiftUI

/// Bridges imperative actions (like `clearCanvas()`) from SwiftUI to the
/// underlying `PathSenseContainerView`.
public class PathSenseAction {
    fileprivate weak var container: PathSenseContainerView?

    public init() {}

    /// Clear all rendered paths.
    public func clearCanvas() {
        container?.clearCanvas()
    }

    /// Enable or disable path capture.
    public var isCaptureEnabled: Bool {
        get { container?.isCaptureEnabled ?? true }
        set { container?.isCaptureEnabled = newValue }
    }
}

/// A SwiftUI view that wraps its content with a PathSense drawing overlay.
///
/// Usage:
/// ```swift
/// @State private var action = PathSenseAction()
///
/// var body: some View {
///     PathSenseOverlay(config: .init(), action: $action) {
///         YourContentView()
///     }
/// }
/// ```
public struct PathSenseOverlay<Content: View>: UIViewControllerRepresentable {
    private let config: PathSenseConfig
    private let action: Binding<PathSenseAction>?
    private let content: Content

    public init(
        config: PathSenseConfig = PathSenseConfig(),
        action: Binding<PathSenseAction>? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.config = config
        self.action = action
        self.content = content()
    }

    public func makeUIViewController(context: Context) -> PathSenseHostingController<Content> {
        let controller = PathSenseHostingController(
            config: config,
            rootView: content
        )
        action?.wrappedValue.container = controller.containerView
        return controller
    }

    public func updateUIViewController(_ controller: PathSenseHostingController<Content>, context: Context) {
        controller.containerView.overlayConfig = config.overlayConfig
        controller.updateContent(content)
        action?.wrappedValue.container = controller.containerView
    }
}

/// Hosting controller that manages the PathSenseContainerView with embedded
/// SwiftUI content. Uses a child UIHostingController for proper environment
/// propagation.
public final class PathSenseHostingController<Content: View>: UIViewController {
    let containerView: PathSenseContainerView
    private var hostingController: UIHostingController<Content>

    init(config: PathSenseConfig, rootView: Content) {
        self.hostingController = UIHostingController(rootView: rootView)
        self.hostingController.view.backgroundColor = .clear
        self.containerView = PathSenseContainerView(
            clientView: hostingController.view,
            config: config
        )
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    public override func viewDidLoad() {
        super.viewDidLoad()
        // Add hosting controller as child for proper SwiftUI environment propagation
        addChild(hostingController)
        view.addSubview(containerView)
        hostingController.didMove(toParent: self)
    }

    public override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        containerView.frame = view.bounds
    }

    func updateContent(_ content: Content) {
        hostingController.rootView = content
    }
}
