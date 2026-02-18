// swift-tools-version:5.8
import PackageDescription

let package = Package(
    name: "PathSenseSDK",
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
            path: "PathSenseCore.xcframework"
        ),
        .target(
            name: "PathSenseUI",
            dependencies: ["PathSenseCore"],
            path: "Sources/PathSenseUI"
        ),
        .testTarget(
            name: "PathSenseUITests",
            dependencies: ["PathSenseUI"],
            path: "Tests/PathSenseUITests"
        ),
    ]
)
