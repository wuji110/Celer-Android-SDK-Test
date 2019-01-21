package com.example.payment

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import com.example.payment.FaucetHelper.FaucetCallBack
import com.example.payment.FaucetHelper.FaucetType
import kotlinx.android.synthetic.main.activity_main.*


class OffChainPaymentActivity : AppCompatActivity() {

    private val TAG = "OffChainPaymentActivity"
    private val clientSideDepositAmount = "5000" // 5000 WEI
    private val serverSideDepositAmount = "15000" // 150000 WEI
    var handler: Handler = Handler()
    //判断是否是第一次注册成功
    private var isFirst: Boolean  = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        isFirst = sharedPreferencesGet2Boolean("isFirst")
        if (! isFirst) {
            createWalletButton?.visibility = View.INVISIBLE
            getTokenFromFaucetButton?.visibility = View.INVISIBLE
            createCelerClientButton?.visibility = View.INVISIBLE
            joinCelerButton?.visibility = View.INVISIBLE
            sendPaymentButton?.visibility = View.VISIBLE
        }
        initActions()
    }


    override fun onRestart() {
        super.onRestart()
        isFirst = sharedPreferencesGet2Boolean("isFirst")
        if(!isFirst) {
            startActivity(Intent(this, OffChainPaymentActivity::class.java))
        }
    }

    private fun showLog(str: String) {
        Log.d(TAG, str)
        handler.post {
            logTextView?.movementMethod = ScrollingMovementMethod()
            logTextView?.append("\n\n" + str)
        }
    }

    private fun initActions() {
        //step 1: Create new wallet
        createWalletButton?.setOnClickListener {
            KeyStoreHelper.generateAccount(this)
            showLog("Step 1: createWallet success : ${KeyStoreHelper.getAddress()}")

//            if (isFirst) {
//                createWalletButton?.visibility = View.VISIBLE
//            } else {
//                createWalletButton?.visibility = View.INVISIBLE
//            }
        }

        //step 2: Get token from faucet
        getTokenFromFaucetButton?.setOnClickListener {

            showLog("Getting some TESTNET token from faucet...")

            //val faucetType = FaucetType.RopstenTestNetETHFromMetaMask
            val faucetType = FaucetType.PrivateTestNetETH
            //val faucetType = FaucetType.PrivateTestNetETH
            //The private TESTNET should not be used by default unless you have problems using Ropsten TESTNET

            FaucetHelper().getTokenFromFaucet(
                    faucetType = faucetType,
                    walletAddress = KeyStoreHelper.getAddress(),
                    faucetCallBack = object : FaucetCallBack {

                        override fun onSuccess(response: String) {
                            showLog("Step 2: getTokenFromFaucet success! " +
                                    "\nTransaction info: " + response +
                                    "\nPlease wait for transaction to complete. " +
                                    "\nYou need to have enough balance before joining Celer. ")

                            when (faucetType) {
                                FaucetType.RopstenTestNetETHFromMetaMask -> {
                                    showLog("Check your transaction status on https://ropsten.etherscan.io/address/$response")
                                    showLog("Check your wallet balance on https://ropsten.etherscan.io/address/${KeyStoreHelper.getAddress()}")
                                    showLog("If you do not have on-chain balance, do not try to join Celer. Please wait till you have some on-chain balance.")
                                }

                                FaucetType.PrivateTestNetETH -> {
                                    showLog("As you are on private TESTNET, we do not currently support viewing on-chain transactions or on-chain balances. " +
                                            "If you want to get the full support and be able to view on-chain transactions on Ropsten, " +
                                            "\nplease use our standard Ropsten cNode profile and the MetaMask Ropsten faucet")
                                }
                            }
                        }

                        override fun onFailure(error: String) {
                            showLog("getTokenFromFaucet error: $error")

                            when (faucetType) {
                                FaucetType.RopstenTestNetETHFromMetaMask -> {
                                    showLog("MetaMask Ropsten ETH faucet failed. This instability is not due to Celer SDK. " +
                                            "\nPlease try again or find another way to transfer some Ropsten ETH to this address: ${KeyStoreHelper.getAddress()}" )
                                    showLog("If this problem happens a lot and you do not have a convenient way to get some Ropsten ETH, " +
                                            "\n please consider using the private TESTNET profile described in CelerClientAPIHelper")
                                }

                                FaucetType.PrivateTestNetETH -> {
                                    showLog("As you are on private TESTNET, we do not currently support viewing on-chain transactions or on-chain balances. " +
                                            "If you want to get the full support and be able to view on-chain transactions on EtherScan, " +
                                            "\nplease use our standard Ropsten cNode profile and the MetaMask Ropsten faucet")
                                }
                            }

                        }

                    })

//            if (isFirst) {
//                getTokenFromFaucetButton?.visibility = View.VISIBLE
//            } else {
//                getTokenFromFaucetButton?.visibility = View.INVISIBLE
//            }

        }


        //step 3: Create Celer Client
        createCelerClientButton?.setOnClickListener {

            val result = CelerClientAPIHelper.initCelerClient(
                    KeyStoreHelper.getKeyStoreString(),
                    KeyStoreHelper.getPassword(),
                    CelerClientAPIHelper.getPrivateTestNetProfile(this))

            showLog("Step 3: $result")

//            if (isFirst) {
//                createCelerClientButton?.visibility = View.VISIBLE
//            } else {
//                createCelerClientButton?.visibility = View.INVISIBLE
//            }

        }





        //step 4: Join Celer
        joinCelerButton?.setOnClickListener {

            val result = CelerClientAPIHelper.joinCeler(clientSideDepositAmount,
                    serverSideDepositAmount)

//            showLog(result)
            showLog("Step 4: $result")


            if (result.contains("successful")) {



                isFirst =  false
                sharedPreferencesSave2Boolean("isFirst", isFirst)
                sendPaymentButton?.visibility = View.VISIBLE
            } else {
                sendPaymentButton?.visibility = View.INVISIBLE
            }

            if (!isFirst) {
                sendPaymentButton?.visibility = View.VISIBLE
            } else {
                sendPaymentButton?.visibility = View.INVISIBLE
            }

        }




        //step 5: Check balance

        checkBalanceButton?.setOnClickListener {
            val result = CelerClientAPIHelper.checkBalance()

            showLog("Current Balance $result")
        }


        //step 6: Send payment

        sendPaymentButton?.setOnClickListener {

            val result = CelerClientAPIHelper.sendPayment(
                    "0x200082086aa9f3341678927e7fc441196a222ac1",
                    "1")
            showLog(result)

            if (!result.contains("successful") || !isFirst) {
                sendPaymentButton?.visibility = View.VISIBLE
            } else {
                sendPaymentButton?.visibility = View.INVISIBLE
            }
        }






//        //step 3: Create Celer Client
//        createCelerClientButton?.setOnClickListener {
//            val result = CelerClientAPIHelper.initCelerClient(
//                    keyStoreString = KeyStoreHelper.getKeyStoreString(),
//                    passwordStr = KeyStoreHelper.getPassword(),
//                    profile = CelerClientAPIHelper.getRopstenTestNetProfile(this))
//            showLog("Step 3: $result")
//        }
//
//        //step 4: Join Celer
//        joinCelerButton?.setOnClickListener {
//            showLog("Step 4. Joining Celer... It is an on-chain operation and takes up to 8 mins. Please wait patiently.")
//            val result = CelerClientAPIHelper.joinCeler(clientSideDepositAmount, serverSideDepositAmount)
//            showLog("Step 4: $result")
//            if (result.contains("successful")) {
//                sendPaymentButton?.visibility = View.VISIBLE
//            } else {
//                sendPaymentButton?.visibility = View.INVISIBLE
//            }
//        }
//
//        //step 5: Check balance
//        checkBalanceButton?.setOnClickListener {
//            val result = CelerClientAPIHelper.checkBalance()
//            showLog("Current balance: $result")
//        }
//
//        //step 6: Send payment
//        sendPaymentButton?.setOnClickListener {
//            val result = CelerClientAPIHelper.sendPayment(
//                    "0x200082086aa9f3341678927e7fc441196a222ac1",
//                    "1")
//            showLog(result)
//        }
    }

    //存储key对应的数据
    private fun Activity.sharedPreferencesSave2Boolean(key: String, info: Boolean): Unit {
        // 1.获得SharedPreferences对象
        val sp: SharedPreferences = getSharedPreferences(key, MODE_PRIVATE or MODE_MULTI_PROCESS)
        // 2.获得Editor对象
        val et: SharedPreferences.Editor = sp.edit()
        // 3.存储数据
//        et.putString(key, info)
        et.putBoolean(key, info)
        // 4.提价
        et.commit()
    }

    //取key对应的数据
    private fun Activity.sharedPreferencesGet2Boolean(key: String): Boolean {
        // 1.获得SharedPreferences对象
        val sp: SharedPreferences = getSharedPreferences(key, MODE_PRIVATE or MODE_MULTI_PROCESS)
        // 2.取数据
//        var result: String = sp.getString(key, "")
        val result: Boolean = sp.getBoolean(key, true)

//        if (result != null) {
//            return result
//        } else {
//            return true
//        }
        return result
//        if (!"".equals(result)) {
//            return result
//        } else {
//            return ""
//        }
    }

//        //清空缓存对应key的数据
//        fun Activity.SharedPreferencesCleardata(key: String): Unit {
//            // 1.获得SharedPreferences对象
//            var sp: SharedPreferences = getSharedPreferences(key, MODE_PRIVATE or MODE_MULTI_PROCESS)
//            // 2.获得Editor对象  清除   提交
//            sp.edit().clear().commit()
//        }


}
