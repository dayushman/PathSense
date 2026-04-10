// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "ScreenRecorderSDK",
    platforms: [
        .iOS(.v14),
    ],
    products: [
        .library(
            name: "ScreenRecorderSDK",
            targets: ["ScreenRecorderUI", "ScreenRecorderCore"]
        ),
    ],
    targets: [
        .binaryTarget(
            name: "ScreenRecorderCore",
            path: "ScreenRecorderCore.xcframework"
        ),
        .target(
            name: "ScreenRecorderUI",
            dependencies: ["ScreenRecorderCore"],
            path: "Sources/ScreenRecorderUI"
        ),
    ]
)
