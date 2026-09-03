import UIKit
import AppTrackingTransparency
import GoogleMobileAds
import GitDashKit

// IDs de AdMob. Se leen del Info.plist, que los recibe de Configuration/*.xcconfig
// (los reales van en Secrets.xcconfig, gitignored). Si no hay, se usan los IDs de
// PRUEBA públicos de Google. El App ID va como GADApplicationIdentifier.
private enum AdUnits {
    private static func infoPlist(_ key: String) -> String? {
        (Bundle.main.object(forInfoDictionaryKey: key) as? String)?
            .trimmingCharacters(in: .whitespaces)
            .nilIfEmpty
    }
    static var banner: String {
        infoPlist("GADBannerUnitID") ?? "ca-app-pub-3940256099942544/2934735716"
    }
    static var rewardedInterstitial: String {
        infoPlist("GADRewardedUnitID") ?? "ca-app-pub-3940256099942544/6978759866"
    }
}

private extension String {
    var nilIfEmpty: String? { isEmpty ? nil : self }
}

// Pon a `false` para saltarte el anuncio recompensado y conceder la recompensa
// (generación del PDF) directamente. Útil para depurar el flujo de informes.
private let rewardedAdEnabled = true

private func requestTrackingWhenReady() {
    DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
        if #available(iOS 14, *) {
            ATTrackingManager.requestTrackingAuthorization { _ in }
        }
    }
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
    // El SDK se puede iniciar de inmediato; el prompt de App Tracking
    // Transparency se pide en cuanto la app está activa.
    requestTrackingWhenReady()
    GADMobileAds.sharedInstance().start(completionHandler: nil)

    IosAdBridge.shared.bannerFactory = {
        let view = GADBannerView(adSize: GADAdSizeBanner)
        view.adUnitID = AdUnits.banner
        view.rootViewController = currentRootVC
        view.load(GADRequest())
        return view
    }

    if rewardedAdEnabled {
        IosAdBridge.shared.rewardedFactory = {
            GmaRewardedController()
        }
    }
    // Si queda a nil, el `actual` de iOS concede la recompensa sin mostrar anuncio.
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
                // Sin anuncio disponible: concede la recompensa igualmente.
                self.finish(rewarded: true)
                return
            }
            self.ad = ad
            ad.fullScreenContentDelegate = self
            ad.present(fromRootViewController: vc) { [weak self] in
                self?.rewardEarned = true
            }
        }
    }

    // La acción premiada se ejecuta DESPUÉS de cerrar el anuncio, no mientras se
    // muestra (si no, el share sheet se presentaría sobre el anuncio).
    func adDidDismissFullScreenContent(_ ad: GADFullScreenPresentingAd) {
        finish(rewarded: rewardEarned)
    }

    func ad(_ ad: GADFullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        finish(rewarded: true)
    }

    private func finish(rewarded: Bool) {
        let reward = onReward
        let cancel = onCancelled
        clear()
        DispatchQueue.main.async {
            if rewarded { reward?() } else { cancel?() }
        }
    }

    private func clear() {
        ad = nil; onReward = nil; onCancelled = nil; rewardEarned = false
    }
}
