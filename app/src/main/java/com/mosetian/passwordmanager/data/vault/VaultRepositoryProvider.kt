package com.mosetian.passwordmanager.data.vault

import android.content.Context
import com.mosetian.passwordmanager.data.local.DatabaseProvider
import com.mosetian.passwordmanager.data.security.VaultCryptoManager

object VaultRepositoryProvider {
    fun createPersistent(context: Context): VaultRepository {
        val db = DatabaseProvider.get(context)
        return PersistentVaultRepository(
            entryDao = db.entryDao(),
            entryDetailDao = db.entryDetailDao(),
            customGroupDao = db.customGroupDao(),
            cryptoManager = VaultCryptoManager()
        ).also { it.migratePlaintextData() }
    }

    fun createInMemory(): VaultRepository = InMemoryVaultRepository()
}
