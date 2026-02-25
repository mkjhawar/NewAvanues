/**
 * OssLicenseRegistry.kt - Comprehensive OSS license data for the license dialog.
 *
 * Groups all runtime dependencies by license type → copyright holder → libraries.
 * Update this file when adding/removing dependencies.
 *
 * Auto-audit: Run `./gradlew :apps:avanues:dependencies --configuration releaseRuntimeClasspath`
 * and cross-reference against this registry.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings

/** A single open-source library entry. */
data class OssLibrary(
    val name: String,
    val version: String = "",
    val note: String = ""   // e.g. "modified" or "compileOnly"
)

/** A copyright holder with their libraries under a specific license. */
data class OssHolder(
    val name: String,
    val libraries: List<OssLibrary>
) {
    val count: Int get() = libraries.size
}

/** A license type grouping all holders that use it. */
data class OssLicenseGroup(
    val licenseName: String,
    val licenseUrl: String,
    val licenseText: String,
    val holders: List<OssHolder>
) {
    val totalLibraries: Int get() = holders.sumOf { it.count }
}

/**
 * Central registry of all OSS dependencies shipped in the Avanues APK.
 *
 * Excludes test-only deps (JUnit, MockK, Robolectric, Espresso, LeakCanary).
 * Excludes compileOnly deps that don't ship (Vivoka VSDK).
 */
object OssLicenseRegistry {

    fun groups(): List<OssLicenseGroup> = listOf(
        apacheLicense2(),
        mitLicense(),
        bsd3Clause(),
        bsd2Clause()
    )

    val totalLibraries: Int get() = groups().sumOf { it.totalLibraries }

    // =========================================================================
    // Apache License 2.0
    // =========================================================================

    private fun apacheLicense2() = OssLicenseGroup(
        licenseName = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        licenseText = APACHE_2_0_TEXT,
        holders = listOf(
            OssHolder("Google", listOf(
                OssLibrary("Jetpack Compose", "BOM 2024.06.00"),
                OssLibrary("Compose Material 3"),
                OssLibrary("Compose Material 3 Adaptive", "1.0.0-beta01"),
                OssLibrary("Material Icons Extended"),
                OssLibrary("Google Material", "1.11.0"),
                OssLibrary("Hilt / Dagger", "2.51.1"),
                OssLibrary("AndroidX Core KTX", "1.12.0"),
                OssLibrary("AndroidX Activity Compose", "1.8.1"),
                OssLibrary("AndroidX AppCompat", "1.6.1"),
                OssLibrary("AndroidX Navigation Compose", "2.7.6"),
                OssLibrary("AndroidX Lifecycle", "2.6.2"),
                OssLibrary("AndroidX SplashScreen", "1.0.1"),
                OssLibrary("AndroidX DataStore", "1.1.1"),
                OssLibrary("AndroidX WebKit", "1.8.0"),
                OssLibrary("AndroidX Security Crypto", "1.1.0-alpha06"),
                OssLibrary("AndroidX Work", "2.9.0"),
                OssLibrary("AndroidX Camera", "1.3.1"),
                OssLibrary("AndroidX Window", "1.2.0"),
                OssLibrary("Gson", "2.10.1"),
                OssLibrary("Play Services", "19.1.0", "Nearby, Location, Fitness, Base"),
                OssLibrary("Firebase Remote Config", "BOM 34.3.0"),
                OssLibrary("TensorFlow Lite", "2.14.0"),
                OssLibrary("Protocol Buffers", "3.25.2"),
            )),
            OssHolder("JetBrains", listOf(
                OssLibrary("Kotlin", "1.9.24"),
                OssLibrary("Kotlin Coroutines", "1.8.1"),
                OssLibrary("Kotlin Multiplatform", "1.9.24"),
                OssLibrary("JetBrains Compose", "1.6.11"),
                OssLibrary("kotlinx-datetime", "0.5.0"),
                OssLibrary("kotlinx-serialization", "1.6.0"),
                OssLibrary("kotlinx-atomicfu", "0.23.2"),
                OssLibrary("Ktor HTTP Client", "2.3.7"),
            )),
            OssHolder("Square", listOf(
                OssLibrary("OkHttp", "4.12.0"),
                OssLibrary("Wire", "5.4.0", "Runtime + gRPC client"),
                OssLibrary("SQLDelight", "2.0.1"),
            )),
            OssHolder("gRPC Authors", listOf(
                OssLibrary("gRPC", "1.62.2", "OkHttp, Netty, Protobuf, Stub"),
                OssLibrary("gRPC Kotlin", "1.4.1"),
            )),
            OssHolder("Microsoft", listOf(
                OssLibrary("ONNX Runtime", "1.17.0"),
            )),
            OssHolder("Apache Software Foundation", listOf(
                OssLibrary("Commons Compress", "1.25.0"),
                OssLibrary("Apache TVM", "0.22.0", "Modified: FFI signature fix"),
            )),
            OssHolder("Aakira", listOf(
                OssLibrary("Napier", "2.7.1", "KMP logging"),
            )),
        )
    )

    // =========================================================================
    // MIT License
    // =========================================================================

    private fun mitLicense() = OssLicenseGroup(
        licenseName = "MIT License",
        licenseUrl = "https://opensource.org/licenses/MIT",
        licenseText = MIT_TEXT,
        holders = listOf(
            OssHolder("Adriel Cafe", listOf(
                OssLibrary("Voyager", "1.0.0", "Navigator, ScreenModel, Tabs, Transitions"),
            )),
        )
    )

    // =========================================================================
    // BSD 3-Clause
    // =========================================================================

    private fun bsd3Clause() = OssLicenseGroup(
        licenseName = "BSD 3-Clause License",
        licenseUrl = "https://opensource.org/licenses/BSD-3-Clause",
        licenseText = BSD_3_CLAUSE_TEXT,
        holders = listOf(
            OssHolder("Zetetic", listOf(
                OssLibrary("SQLCipher", "4.5.4", "Database encryption"),
            )),
        )
    )

    // =========================================================================
    // BSD 2-Clause
    // =========================================================================

    private fun bsd2Clause() = OssLicenseGroup(
        licenseName = "BSD 2-Clause License",
        licenseUrl = "https://opensource.org/licenses/BSD-2-Clause",
        licenseText = BSD_2_CLAUSE_TEXT,
        holders = listOf(
            OssHolder("Sentry", listOf(
                OssLibrary("Sentry Android", "7.0.0", "Crash reporting"),
            )),
        )
    )

    // =========================================================================
    // License Texts
    // =========================================================================

    private const val APACHE_2_0_TEXT = """Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/

TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

1. Definitions.

"License" shall mean the terms and conditions for use, reproduction, and distribution as defined by Sections 1 through 9 of this document.

"Licensor" shall mean the copyright owner or entity authorized by the copyright owner that is granting the License.

"Legal Entity" shall mean the union of the acting entity and all other entities that control, are controlled by, or are under common control with that entity.

"You" (or "Your") shall mean an individual or Legal Entity exercising permissions granted by this License.

"Source" form shall mean the preferred form for making modifications, including but not limited to software source code, documentation source, and configuration files.

"Object" form shall mean any form resulting from mechanical transformation or translation of a Source form, including but not limited to compiled object code, generated documentation, and conversions to other media types.

"Work" shall mean the work of authorship, whether in Source or Object form, made available under the License.

"Derivative Works" shall mean any work, whether in Source or Object form, that is based on (or derived from) the Work.

"Contribution" shall mean any work of authorship submitted to the Licensor for inclusion in the Work.

"Contributor" shall mean Licensor and any Legal Entity on behalf of whom a Contribution has been received by the Licensor.

2. Grant of Copyright License. Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable copyright license to reproduce, prepare Derivative Works of, publicly display, publicly perform, sublicense, and distribute the Work and such Derivative Works in Source or Object form.

3. Grant of Patent License. Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable patent license to make, have made, use, offer to sell, sell, import, and otherwise transfer the Work.

4. Redistribution. You may reproduce and distribute copies of the Work or Derivative Works thereof in any medium, with or without modifications, and in Source or Object form, provided that You meet the following conditions:

(a) You must give any other recipients of the Work or Derivative Works a copy of this License; and

(b) You must cause any modified files to carry prominent notices stating that You changed the files; and

(c) You must retain, in the Source form of any Derivative Works that You distribute, all copyright, patent, trademark, and attribution notices from the Source form of the Work; and

(d) If the Work includes a "NOTICE" text file, You must include a readable copy of the attribution notices contained within such NOTICE file.

5. Submission of Contributions. Unless You explicitly state otherwise, any Contribution intentionally submitted for inclusion in the Work by You to the Licensor shall be under the terms and conditions of this License, without any additional terms or conditions.

6. Trademarks. This License does not grant permission to use the trade names, trademarks, service marks, or product names of the Licensor.

7. Disclaimer of Warranty. Unless required by applicable law or agreed to in writing, Licensor provides the Work on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.

8. Limitation of Liability. In no event shall any Contributor be liable to You for damages, including any direct, indirect, special, incidental, or consequential damages.

9. Accepting Warranty or Additional Liability. While redistributing the Work, You may choose to offer acceptance of support, warranty, indemnity, or other liability obligations consistent with this License.

END OF TERMS AND CONDITIONS"""

    private const val MIT_TEXT = """MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE."""

    private const val BSD_3_CLAUSE_TEXT = """BSD 3-Clause License

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."""

    private const val BSD_2_CLAUSE_TEXT = """BSD 2-Clause "Simplified" License

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."""
}
