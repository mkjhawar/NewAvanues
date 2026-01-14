// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "AvaElementsRenderer",
    platforms: [
        .iOS(.v15),
        .macOS(.v12)
    ],
    products: [
        .library(
            name: "AvaElementsRenderer",
            targets: ["AvaElementsRenderer"]
        )
    ],
    dependencies: [
        .package(url: "https://github.com/pointfreeco/swift-snapshot-testing", from: "1.12.0"),
        .package(url: "https://github.com/SDWebImage/SDWebImage", from: "5.18.0")
    ],
    targets: [
        .target(
            name: "AvaElementsRenderer",
            dependencies: [
                .product(name: "SDWebImage", package: "SDWebImage")
            ],
            path: "src/iosMain/swift",
            exclude: [],
            sources: nil,
            resources: nil,
            publicHeadersPath: nil,
            cSettings: nil,
            cxxSettings: nil,
            swiftSettings: [
                .enableUpcomingFeature("BareSlashRegexLiterals"),
                .enableUpcomingFeature("ConciseMagicFile"),
                .enableUpcomingFeature("ExistentialAny"),
                .enableUpcomingFeature("ForwardTrailingClosures"),
                .enableUpcomingFeature("ImplicitOpenExistentials"),
                .enableUpcomingFeature("StrictConcurrency")
            ],
            linkerSettings: nil
        ),
        .testTarget(
            name: "AvaElementsRendererTests",
            dependencies: [
                "AvaElementsRenderer",
                .product(name: "SnapshotTesting", package: "swift-snapshot-testing")
            ],
            path: "Tests",
            exclude: ["__Snapshots__"],
            sources: nil,
            resources: [
                .copy("Fixtures")
            ],
            swiftSettings: [
                .define("SNAPSHOT_TESTING")
            ]
        )
    ],
    swiftLanguageVersions: [.v5]
)
