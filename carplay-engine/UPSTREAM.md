# CarPlay receiver engine

Source: https://github.com/shihabal3amri/DiPlay
Commit: 615b32ce0b40cd0b99ce94ba23a512001553684f
Path: shared/

Receiver code is GPL-3.0; Fiorino Auto remains AGPL-3.0. Preserve all upstream source headers. Native SpeexDSP and Concentus include their own BSD license files. Bouncy Castle and JmDNS are dependencies with their respective notices. BYD HUD artwork with separate noncommercial terms is omitted. No accessory identity or APK signing key is included.

Local adaptations: Gradle 8.14/AGP 8.13/Kotlin 2.2 build declaration and SDK 36; host-only manifest without the upstream Android Auto sample service. Engine behavior remains upstream unless recorded here.

Host-test compatibility: expose the local JDK HTTP server API only on the host test compile classpath. The SoftAP test fixture invokes its Android-hidden passphrase setter reflectively to compile against the public SDK 36.
