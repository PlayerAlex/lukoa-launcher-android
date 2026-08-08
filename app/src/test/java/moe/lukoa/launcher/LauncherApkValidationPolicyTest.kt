package moe.lukoa.launcher

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherApkValidationPolicyTest {
    private val current = LauncherApkIdentity("moe.lukoa.launcher", 10, setOf("trusted"))

    @Test
    fun `newer apk with matching signer is accepted`() {
        assertNull(
            LauncherApkValidationPolicy.validate(
                current,
                LauncherApkIdentity("moe.lukoa.launcher", 11, setOf("trusted")),
            ),
        )
    }

    @Test
    fun `wrong package old version missing signer and wrong signer are blocked`() {
        assertTrue(LauncherApkValidationPolicy.validate(current, LauncherApkIdentity("other", 11, setOf("trusted")))!!.contains("不是"))
        assertTrue(LauncherApkValidationPolicy.validate(current, LauncherApkIdentity("moe.lukoa.launcher", 10, setOf("trusted")))!!.contains("不是新版本"))
        assertTrue(LauncherApkValidationPolicy.validate(current, LauncherApkIdentity("moe.lukoa.launcher", 11, emptySet()))!!.contains("无法读取"))
        assertTrue(LauncherApkValidationPolicy.validate(current, LauncherApkIdentity("moe.lukoa.launcher", 11, setOf("other")))!!.contains("签名"))
    }
}
