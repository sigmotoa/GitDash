import UIKit
import GoogleMobileAds
import GitDashKit

// IDs de AdMob para iOS. (El App ID va en Info.plist como GADApplicationIdentifier.)
private enum AdUnits {
    static let banner = "ca-app-pub-3940256099942544/2501205051"
    static let rewardedInterstitial = "ca-app-pub-3940256099942544/3980089055"
}

private var currentRootVC: UIViewController? {
    UIApplication.shared.connectedScenes
        .compactMap { $0 as? UIWindowScene }
        .flatMap { $0.windows }
        .first { $0.isKeyWindow }?.rootViewController
}

/// Se llama una vez al arrancar (`iOSApp.init`). Inicializa el SDK y conecta las
/// fábricas que el código Kotlin usa para el banner y el anuncio recompensado.
func installAds() {
    GADMobileAds.sharedInstance().start(completionHandler: nil)

    IosAdBridge.shared.bannerFactory = {
        let view = GADBannerView(adSize: GADAdSizeBanner)
        view.adUnitID = AdUnits.banner
        view.rootViewController = currentRootVC
        view.load(GADRequest())
        return view
    }

    IosAdBridge.shared.rewardedFactory = {
        GmaRewardedController()
    }
}

/// Carga y muestra un anuncio recompensado; si algo falla, concede la recompensa
/// igualmente (mismo criterio que el fallback de Android).
private final class GmaRewardedController: NSObject, RewardedAdController, GADFullScreenContentDelegate {

    private var ad: GADRewardedInterstitialAd?
    private var onReward: (() -> Void)?
    private var onCancelled: (() -> Void)?
    private var rewardEarned = false

    func show(onReward: @escaping () -> Void, onCancelled: @escaping () -> Void) {
        self.onReward = onReward
        self.onCancelled = onCancelled

        GADRewardedInterstitialAd.load(
            withAdUnitID: AdUnits.rewardedInterstitial,
            request: GADRequest()
        ) { [weak self] ad, error in
            guard let self else { return }
            guard let ad, error == nil, let vc = currentRootVC else {
                self.onReward?(); self.clear(); return
            }
            self.ad = ad
            ad.fullScreenContentDelegate = self
            ad.present(fromRootViewController: vc) { [weak self] in
                self?.rewardEarned = true
                self?.onReward?()
            }
        }
    }

    func adDidDismissFullScreenContent(_ ad: GADFullScreenPresentingAd) {
        if !rewardEarned { onCancelled?() }
        clear()
    }

    func ad(_ ad: GADFullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        onReward?()
        clear()
    }

    private func clear() {
        ad = nil; onReward = nil; onCancelled = nil; rewardEarned = false
    }
}
