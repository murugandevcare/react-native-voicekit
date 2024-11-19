import Foundation

@objc(ListenEventEmitter)
class ListenEventEmitter: RCTEventEmitter {
  public static var shared: ListenEventEmitter!

  override init() {
    super.init()
    ListenEventEmitter.shared = self
  }

  override func supportedEvents() -> [String]! {
    ["RNListen.result", "RNListen.partial-result"]
  }
}
