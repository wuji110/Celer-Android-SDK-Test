package com.example.payment

import android.content.Context
import network.celer.geth.Account
import network.celer.geth.Geth
import network.celer.geth.KeyStore
import java.io.File

object KeyStoreHelper {
    private val password = "CelerNetwork"
    private var gethKeyStore: KeyStore? = null
    private var account: Account? = null
    private var keyStoreString: String = ""

    fun getAddress(): String {
        return account!!.address.hex
    }

    fun getPassword(): String {
        return password
    }

    fun getKeyStoreString(): String {
        return keyStoreString
    }

    fun generateFilePath(context: Context): String {
        val file = File(context.filesDir.path, "celer")
        if (!file.exists()) {
            file.mkdir()
        }
        return file.path
    }

    fun generateAccount(context: Context) {
        if (gethKeyStore == null) {
            val filePath = generateFilePath(context)
            gethKeyStore = KeyStore(filePath, Geth.LightScryptN, Geth.LightScryptP)
        }

        gethKeyStore?.let { gethKeyStore ->
            account = gethKeyStore.newAccount(password)
            account?.let { account ->
                keyStoreString = String(gethKeyStore.exportKey(account, password, password), Charsets.UTF_8)
            }
        }
    }
}