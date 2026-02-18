// swift-tools-version:5.8
import PackageDescription

let package = Package(
    name: "PathSense",
    platforms: [
        .iOS(.v13),
    ],
    products: [
        .library(name: "PathSenseCore", targets: ["PathSenseCore"]),
        .library(name: "PathSenseUI", targets: ["PathSenseUI"]),
    ],
    targets: [
        .binaryTarget(
            name: "PathSenseCore",
            path: "ios/PathSenseSDK/PathSenseCore.xcframework"
        ),
        .target(
            name: "PathSenseUI",
            dependencies: ["PathSenseCore"],
            path: "ios/PathSenseSDK/Sources/PathSenseUI"
        ),
        .testTarget(
            name: "PathSenseUITests",
            dependencies: ["PathSenseUI"],
            path: "ios/PathSenseSDK/Tests/PathSenseUITests"
        ),
    ]
)
