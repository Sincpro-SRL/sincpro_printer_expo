require 'json'

package = JSON.parse(File.read(File.join(__dir__, '..', 'package.json')))

Pod::Spec.new do |s|
  s.name           = 'SincproPrinter'
  s.version        = package['version']
  s.summary        = package['description']
  s.description    = package['description']
  s.license        = package['license']
  s.author         = package['author']
  s.homepage       = package['homepage']
  s.platforms      = { :ios => '16.4' }
  s.swift_version  = '5.9'
  s.source         = { git: 'https://github.com/Sincpro-SRL/sincpro_printer_expo' }
  s.static_framework = true

  s.dependency 'ExpoModulesCore'

  # Bixolon Label Printer SDK v1.1.16 (dynamic xcframework: device arm64 + simulator arm64/x86_64).
  s.vendored_frameworks = 'Frameworks/frmLabelPrinterSDK.xcframework'
  s.frameworks = 'ExternalAccessory', 'CoreBluetooth', 'PDFKit'

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
  }

  s.source_files = '{entrypoints,services,adapters,domain,infrastructure}/**/*.swift'
end
