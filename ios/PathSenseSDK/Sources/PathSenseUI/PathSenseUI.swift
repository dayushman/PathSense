import Foundation

public enum PathSenseUI {
    @available(*, deprecated, message: "Use PathSense.enable(), PathSense.disable(), and PathSense.isEnabled instead")
    public static var isEnabled: Bool {
        get { PathSense.isEnabled }
        set {
            if newValue { PathSense.enable() }
            else { PathSense.disable() }
        }
    }
}
