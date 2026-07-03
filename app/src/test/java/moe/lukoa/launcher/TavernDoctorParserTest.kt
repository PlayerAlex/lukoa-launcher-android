package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernDoctorParserTest {
    @Test
    fun `parse reads doctor report and deduplicates candidate directories`() {
        val report = TavernDoctorParser.parse(
            """
            ==== Lukoa doctor ====
            doctor.tavernDir=~/SillyTavern
            doctor.port=8000
            doctor.termuxRepo.label=清华源
            doctor.termuxRepo.uri=https://mirrors.tuna.tsinghua.edu.cn/termux/termux-main
            doctor.tool.git=1
            doctor.tool.node=true
            doctor.tool.npm=ok
            doctor.tool.curl=missing
            doctor.tavernDir.exists=1
            doctor.tavernDir.packageJson=1
            doctor.tavernDir.startEntry=0
            doctor.tavernDir.gitRepo=1
            doctor.process.pidFile=0
            doctor.process.detected=1
            doctor.process.httpOk=false
            doctor.process.portListening=1
            doctor.process.portConflict=0
            doctor.summary.level=warning
            doctor.summary.message=目录存在，但启动入口缺失
            candidate.1=~/SillyTavern
            candidate.2=~/SillyTavern
            candidate.3=~/AltTavern
            """.trimIndent(),
        )

        assertNotNull(report)
        requireNotNull(report)
        assertEquals("~/SillyTavern", report.tavernDir)
        assertEquals(8000, report.port)
        assertEquals(TavernDoctorLevel.Warning, report.summaryLevel)
        assertEquals("目录存在，但启动入口缺失", report.summaryMessage)
        assertTrue(report.gitAvailable == true)
        assertTrue(report.nodeAvailable == true)
        assertTrue(report.npmAvailable == true)
        assertTrue(report.curlAvailable == false)
        assertTrue(report.portListening == true)
        assertFalse(report.portConflict == true)
        assertEquals(listOf("~/SillyTavern", "~/AltTavern"), report.candidateDirectories)
    }

    @Test
    fun `parse returns null for non doctor output`() {
        assertNull(TavernDoctorParser.parse("hello"))
    }
}
