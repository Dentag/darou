import DarouUI
import SwiftUI

@main
struct DarouApp: App {
    init() {
        guard let serverOrigin = Bundle.main.object(forInfoDictionaryKey: "DarouServerOrigin") as? String else {
            fatalError("DarouServerOrigin is missing from Info.plist")
        }
        InitIosAppKt.doInitIosApp(serverOrigin: serverOrigin)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
