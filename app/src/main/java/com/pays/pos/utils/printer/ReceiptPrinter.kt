package com.pays.pos.utils.printer

import android.app.Activity
import android.content.Context
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener

class ReceiptPrinter private constructor() : ReceiveListener {
    private var mContext: Context? = null
    private var mPrinter: Printer? = null
    fun runPrintReceiptSequence(context: Context?, datas: String) {
        mContext = context
        /* 初始化对象 */if (!initializeObject()) {
        }
        /* 创建打印 数据 */if (!createReceiptData(datas)) {
            finalizeObject()
        }
        /*打印*/if (!printData(ipAddress=datas)) {
            finalizeObject()
        }
    }

    /**
     * 初始化打印对象  返回true 是否生成
     */
    private fun initializeObject(): Boolean {
        mPrinter = try {
            // ((SpnModelsItem) mSpnLang.getSelectedItem()).getModelConstant()
            //            mPrinter = new Printer(((SpnModelsItem) mSpnSeries.getSelectedItem()).getModelConstant(),
            Printer(Printer.TM_U220, Printer.MODEL_ANK, mContext)
            //
        } catch (e: Exception) {
            //ShowMsg.showException(e, "Printer", mContext)
            return false
        }
        mPrinter!!.setReceiveEventListener(this)
        return true
    }

    /**
     * 打印结果监听
     */
    override fun onPtrReceive(
        printerObj: Printer,
        code: Int,
        status: PrinterStatusInfo,
        printJobId: String
    ) {
        (mContext as Activity?)!!.runOnUiThread {
         //   ShowMsg.showResult(code, makeErrorMessage(status), mContext)
            Thread { //执行完之后断开当前打印
                disconnectPrinter()
            }.start()
        }
    }

    private fun createReceiptData(datas: String): Boolean {
        var method = ""
        //        Bitmap logoData = BitmapFactory.decodeResource(getResources(), R.drawable.store);
        var textData: StringBuffer? = StringBuffer("BIG5") //繁体字符集BIG5  中文 GBK
        if (mPrinter == null) {
            return false
        }
        try {
            //语言需要
            textData!!.append(datas)
            mPrinter!!.addTextLang(Printer.LANG_ZH_TW)
            method = "addTextAlign"
            mPrinter!!.addTextAlign(Printer.ALIGN_CENTER)
            mPrinter!!.addTextSize(Printer.PARAM_DEFAULT, Printer.PARAM_DEFAULT)
            mPrinter!!.addText(datas)

            //2D符號  二維碼MODEL_2才能掃碼識別  寬 3 to 16 ，默認3  高1～255  Size：0～65535
            mPrinter!!.addSymbol(
                "12321421",
                Printer.SYMBOL_QRCODE_MODEL_2,
                Printer.PARAM_DEFAULT,
                9,
                9,
                0
            )
            //空1行
            mPrinter!!.addFeedLine(1)
            mPrinter!!.addText("在線訂座入座終端編號2    174.81\n")
            //空5行
            mPrinter!!.addFeedLine(5)


/*
            //條形碼
             mPrinter.addBarcode("012345678902",
                        Printer.BARCODE_UPC_A,
                        Printer.HRI_BELOW,
                        Printer.FONT_A,
                        2,
                        100); //Width:2 to 6 ; Height:1 to 255
                mPrinter.addBarcode("012345678902",
                        Printer.BARCODE_UPC_E,
                        Printer.HRI_BELOW,
                        Printer.FONT_B,
                        2,
                        100); //Width:2 to 6 ; Height:1 to 255
                mPrinter.addBarcode("012345678902",
                        Printer.BARCODE_EAN13,
                        Printer.HRI_BELOW,
                        Printer.FONT_C,
                        2,
                        100); //Width:2 to 6 ; Height:1 to 255
                mPrinter.addBarcode("0123456",
                        Printer.BARCODE_EAN8,
                        Printer.HRI_BELOW,
                        Printer.FONT_D,
                        2,
                        100); //Width:2 to 6 ; Height:1 to 255
                //BARCODE_CODE39 类型 数字长度为11～15  ；HRI_BELOW 位置  ;字体还是FONT_A默认大小最好看,FONT_C 小一点
                mPrinter.addBarcode("012094578902215",
                        Printer.BARCODE_CODE39,
                        Printer.HRI_BELOW,
                        Printer.FONT_A,
                        2,
                        100); //Width:2 to 6 ; Height:1 to 255
                mPrinter.addText("力亨營業數" + "\n");
                mPrinter.addFeedLine(1);
                mPrinter.addCut(Printer.CUT_FEED);*/
        } catch (e: Exception) {
            //ShowMsg.showException(e, method, mContext)
            return false
        }
        textData = null
        return true
    }

    /**
     * 打印数据
     */
    private fun printData(ipAddress: String): Boolean {
        if (mPrinter == null) {
            return false
        }
        //连接打印设备
        if (!connectPrinter(ipAddress)) {
            return false
        }
        //当前的打印状态
        val status = mPrinter!!.status

//        dispPrinterWarnings(status);

        //打印机不可用时弹窗提示
        if (!isPrintable(status)) {
           // ShowMsg.showMsg(makeErrorMessage(status), mContext)
            try {
                //断开
                mPrinter!!.disconnect()
            } catch (ex: Exception) {
                // Do nothing
            }
            return false
        }
        try {
            //通过打印对象发送已经添加在mPrinter中的数据
            mPrinter!!.sendData(Printer.PARAM_DEFAULT)
        } catch (e: Exception) {
           // ShowMsg.showException(e, "sendData", mContext)
            try {
                mPrinter!!.disconnect()
            } catch (ex: Exception) {
                // Do nothing
            }
            return false
        }
        return true
    }

    /**
     * 连接打印机
     *
     * @return
     */
    private fun connectPrinter(ipAddress:String): Boolean {
        var isBeginTransaction = false
        if (mPrinter == null) {
            return false
        }
        try {
            //连接 USB设备地址我的 EPSON TM-T88IV型号地址: USB:/dev/bus/usb/004/002  必须通过开启搜索设备设置连接机型
            mPrinter?.connect(ipAddress, Printer.PARAM_DEFAULT);
           // mPrinter!!.connect(MainActivity.getPrinterTarget(), Printer.PARAM_DEFAULT)
        } catch (e: Exception) {
            //ShowMsg.showException(e, "connect fail", mContext)
            return false
        }
        try {
            mPrinter!!.beginTransaction()
            isBeginTransaction = true
        } catch (e: Exception) {
            //ShowMsg.showException(e, "beginTransaction", mContext)
        }
        if (isBeginTransaction == false) {
            try {
                mPrinter!!.disconnect()
            } catch (e: Epos2Exception) {
                // Do nothing
                return false
            }
        }
        return true
    }

    /**
     * 打印是否可用
     */
    private fun isPrintable(status: PrinterStatusInfo?): Boolean {
        if (status == null) {
            return false
        }
        if (status.connection == Printer.FALSE) {
            return false
        } else if (status.online == Printer.FALSE) {
            return false
        } else {
            //print available
        }
        return true
    }

    /**
     * 断开当前打印
     */
    private fun disconnectPrinter() {
        if (mPrinter == null) {
            return
        }
        try {
            mPrinter!!.endTransaction()
        } catch (e: Exception) {
            (mContext as Activity?)!!.runOnUiThread {
                /*ShowMsg.showException(
                    e,
                    "endTransaction",
                    mContext
                )*/
            }
        }
        try {
            mPrinter!!.disconnect()
        } catch (e: Exception) {
            (mContext as Activity?)!!.runOnUiThread {
                /*ShowMsg.showException(
                    e,
                    "disconnect",
                    mContext
                )*/
            }
        }
        finalizeObject()
    }

    /**
     * 完成打印，清空命令缓冲，关闭释放打印对象
     */
    private fun finalizeObject() {
        if (mPrinter == null) {
            return
        }
        mPrinter!!.clearCommandBuffer()
        mPrinter!!.setReceiveEventListener(null)
        mPrinter = null
    }

    /**
     * 错误信息
     */
    /*private fun makeErrorMessage(status: PrinterStatusInfo): String {
        var msg = ""
        if (status.online == Printer.FALSE) {
            msg += mContext!!.getString(R.string.handlingmsg_err_offline)
        }
        if (status.connection == Printer.FALSE) {
            msg += mContext!!.getString(R.string.handlingmsg_err_no_response)
        }
        if (status.coverOpen == Printer.TRUE) {
            msg += mContext!!.getString(R.string.handlingmsg_err_cover_open)
        }
        if (status.paper == Printer.PAPER_EMPTY) {
            msg += mContext!!.getString(R.string.handlingmsg_err_receipt_end)
        }
        if (status.paperFeed == Printer.TRUE || status.panelSwitch == Printer.SWITCH_ON) {
            msg += mContext!!.getString(R.string.handlingmsg_err_paper_feed)
        }
        if (status.errorStatus == Printer.MECHANICAL_ERR || status.errorStatus == Printer.AUTOCUTTER_ERR) {
            msg += mContext!!.getString(R.string.handlingmsg_err_autocutter)
            msg += mContext!!.getString(R.string.handlingmsg_err_need_recover)
        }
        if (status.errorStatus == Printer.UNRECOVER_ERR) {
            msg += mContext!!.getString(R.string.handlingmsg_err_unrecover)
        }
        if (status.errorStatus == Printer.AUTORECOVER_ERR) {
            if (status.autoRecoverError == Printer.HEAD_OVERHEAT) {
                msg += mContext!!.getString(R.string.handlingmsg_err_overheat)
                msg += mContext!!.getString(R.string.handlingmsg_err_head)
            }
            if (status.autoRecoverError == Printer.MOTOR_OVERHEAT) {
                msg += mContext!!.getString(R.string.handlingmsg_err_overheat)
                msg += mContext!!.getString(R.string.handlingmsg_err_motor)
            }
            if (status.autoRecoverError == Printer.BATTERY_OVERHEAT) {
                msg += mContext!!.getString(R.string.handlingmsg_err_overheat)
                msg += mContext!!.getString(R.string.handlingmsg_err_battery)
            }
            if (status.autoRecoverError == Printer.WRONG_PAPER) {
                msg += mContext!!.getString(R.string.handlingmsg_err_wrong_paper)
            }
        }
        if (status.batteryLevel == Printer.BATTERY_LEVEL_0) {
            msg += mContext!!.getString(R.string.handlingmsg_err_battery_real_end)
        }
        return msg
    }*/

    companion object {
        /**
         * 这种方式采用双锁机制，安全且在多线程情况下能保持高性能
         */
        var instance: ReceiptPrinter? = null
            get() {
                if (field == null) {
                    synchronized(ReceiptPrinter::class.java) {
                        if (field == null) {
                            field = ReceiptPrinter()
                        }
                    }
                }
                return field
            }
            private set
    }
}
