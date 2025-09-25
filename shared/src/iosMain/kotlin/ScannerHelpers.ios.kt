import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.presentedViewController
import platform.AVFoundation.*
import platform.UIKit.*
import platform.Foundation.*
import platform.CoreImage.*
import kotlinx.cinterop.*

object UIKitRootProvider {
    fun currentRootController(): UIViewController? {
        val scenes = UIApplication.sharedApplication.connectedScenes
        val anyScene = scenes.anyObject() as? UIWindowScene
        val window = anyScene?.windows?.firstObject as? UIWindow
        return window?.rootViewController
    }
}

class SimpleQrScannerController(private val onResult: (String?) -> Unit) : UIViewController(nibName = null, bundle = null) {
    private var session: AVCaptureSession? = null
    private var previewLayer: AVCaptureVideoPreviewLayer? = null

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.blackColor
        setupCamera()
    }

    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)
        session?.stopRunning()
    }

    private fun setupCamera() {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        if (device == null) {
            onResult(null)
            dismissViewControllerAnimated(true, completion = null)
            return
        }
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, error = null)
        val session = AVCaptureSession()
        if (input != null && session.canAddInput(input)) session.addInput(input)

        val output = AVCaptureMetadataOutput()
        if (session.canAddOutput(output)) session.addOutput(output)
        output.setMetadataObjectsDelegate(queue = dispatch_get_main_queue()) { metadataObjects ->
            val obj = (metadataObjects?.firstObject as? AVMetadataMachineReadableCodeObject)
            val text = obj?.stringValue
            if (text != null) {
                onResult(text)
                session.stopRunning()
                dismissViewControllerAnimated(true, completion = null)
            }
        }
        output.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)

        this.session = session
        previewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
            videoGravity = AVLayerVideoGravityResizeAspectFill
            frame = view.layer.bounds
        }
        if (previewLayer != null) view.layer.addSublayer(previewLayer!!)

        // Add simple close button
        val button = UIButton.buttonWithType(UIButtonTypeSystem) as UIButton
        button.setTitle("Cancel", forState = UIControlStateNormal)
        button.setTitleColor(UIColor.whiteColor, forState = UIControlStateNormal)
        button.frame = CGRectMake(20.0, 40.0, 80.0, 40.0)
        button.addTarget(this, NSSelectorFromString("onCancel"), UIControlEventTouchUpInside)
        view.addSubview(button)

        session.startRunning()
    }

    @ObjCAction
    fun onCancel() {
        onResult(null)
        dismissViewControllerAnimated(true, completion = null)
    }
}
