// Genera un icono placeholder 1024x1024 para iOS.
// Uso: swift make_placeholder_icon.swift <ruta-salida.png>
import AppKit

let side: CGFloat = 1024
let out = CommandLine.arguments.count > 1
    ? CommandLine.arguments[1]
    : "icon-1024.png"

let image = NSImage(size: NSSize(width: side, height: side))
image.lockFocus()
let ctx = NSGraphicsContext.current!.cgContext

// Fondo: degradado violeta -> azul (paleta de GitDash)
let colors = [
    NSColor(srgbRed: 0.42, green: 0.28, blue: 0.85, alpha: 1).cgColor,
    NSColor(srgbRed: 0.10, green: 0.14, blue: 0.49, alpha: 1).cgColor,
]
let gradient = CGGradient(
    colorsSpace: CGColorSpaceCreateDeviceRGB(),
    colors: colors as CFArray,
    locations: [0, 1]
)!
ctx.drawLinearGradient(
    gradient,
    start: CGPoint(x: 0, y: side),
    end: CGPoint(x: side, y: 0),
    options: []
)

// Monograma "GD"
let paragraph = NSMutableParagraphStyle()
paragraph.alignment = .center
let attrs: [NSAttributedString.Key: Any] = [
    .font: NSFont.systemFont(ofSize: 460, weight: .heavy),
    .foregroundColor: NSColor.white,
    .paragraphStyle: paragraph,
]
let text = "GD" as NSString
let textSize = text.size(withAttributes: attrs)
text.draw(
    at: NSPoint(x: (side - textSize.width) / 2, y: (side - textSize.height) / 2),
    withAttributes: attrs
)

image.unlockFocus()

guard
    let tiff = image.tiffRepresentation,
    let rep = NSBitmapImageRep(data: tiff),
    let png = rep.representation(using: .png, properties: [:])
else {
    FileHandle.standardError.write("no se pudo generar el PNG\n".data(using: .utf8)!)
    exit(1)
}
try! png.write(to: URL(fileURLWithPath: out))
print("escrito: \(out)")
