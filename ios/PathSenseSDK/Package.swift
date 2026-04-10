// swift-tools-version:5.8
import PackageDescription

let package = Package(
    name: "PathSenseSDK",
    platforms: [
        .iOS(.v14),
    ],
    products: [
        .library(name: "PathSenseCore", targets: ["PathSenseCore"]),
        .library(name: "PathSenseUI", targets: ["PathSenseUI"]),
        .library(name: "ScreenRecorderCore", targets: ["ScreenRecorderCore"]),
        .library(name: "ScreenRecorderUI", targets: ["ScreenRecorderUI"]),
    ],
    targets: [
        .binaryTarget(
            name: "PathSenseCore",
            path: "PathSenseCore.xcframework"
        ),
        .target(
            name: "PathSenseUI",
            dependencies: ["PathSenseCore"],
            path: "Sources/PathSenseUI"
        ),
        .binaryTarget(
            name: "ScreenRecorderCore",
            path: "ScreenRecorderCore.xcframework"
        ),
        .target(
            name: "ScreenRecorderUI",
            dependencies: ["ScreenRecorderCore", "PathSenseCore", "PathSenseUI"],
            path: "Sources/ScreenRecorderUI"
        ),
        .testTarget(
            name: "PathSenseUITests",
            dependencies: ["PathSenseUI"],
            path: "Tests/PathSenseUITests"
        ),
    ]
)
