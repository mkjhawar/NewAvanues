// Avanues iOS - Unit Tests
// Phase 0: Basic smoke tests

import XCTest
@testable import Avanues

final class AvanuesTests: XCTestCase {

    func testAppStateInitialValues() {
        let state = AppState()
        XCTAssertEqual(state.currentMode, .hub)
        XCTAssertEqual(state.colorPalette, .hydra)
        XCTAssertEqual(state.materialStyle, .water)
        XCTAssertEqual(state.appearanceMode, .auto)
        XCTAssertFalse(state.isVoiceActive)
        XCTAssertEqual(state.voiceLocale, "en-US")
    }

    func testAppStateModeNavigation() {
        let state = AppState()
        state.currentMode = .browser
        XCTAssertEqual(state.currentMode, .browser)

        state.currentMode = .settings
        XCTAssertEqual(state.currentMode, .settings)
    }

    func testColorPalettePersistence() {
        let state = AppState()
        state.colorPalette = .sol
        // Verify @AppStorage writes the correct string
        let stored = UserDefaults.standard.string(forKey: "theme_palette")
        XCTAssertEqual(stored, "SOL")
    }

    func testMaterialStylePersistence() {
        let state = AppState()
        state.materialStyle = .glass
        let stored = UserDefaults.standard.string(forKey: "theme_style")
        XCTAssertEqual(stored, "Glass")
    }

    func testAppearanceModePersistence() {
        let state = AppState()
        state.appearanceMode = .dark
        let stored = UserDefaults.standard.string(forKey: "theme_appearance")
        XCTAssertEqual(stored, "Dark")
    }
}
