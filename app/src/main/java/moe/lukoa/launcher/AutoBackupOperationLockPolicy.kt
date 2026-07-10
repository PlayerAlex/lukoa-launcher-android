package moe.lukoa.launcher

object AutoBackupOperationLockPolicy {
    const val LABEL = "自动备份"

    fun shouldDeferRetention(snapshot: OperationLockSnapshot?): Boolean {
        return snapshot?.label == LABEL && snapshot.ownerToken.isNotBlank()
    }
}
