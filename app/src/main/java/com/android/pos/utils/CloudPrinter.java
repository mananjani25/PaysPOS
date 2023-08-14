package com.android.pos.utils;

import java.io.ByteArrayOutputStream;

import com.sunmi.externalprinterlibrary2.printer.CloudPrinterInfo;
import com.sunmi.externalprinterlibrary2.style.ErrorLevel;
import java.nio.charset.StandardCharsets;
import android.text.TextUtils;
import com.sunmi.externalprinterlibrary2.style.HriStyle;
import com.sunmi.externalprinterlibrary2.style.BarcodeType;
import com.sunmi.externalprinterlibrary2.task.BaseTask;
import com.sunmi.externalprinterlibrary2.utils.BitmapUtil;
import com.sunmi.externalprinterlibrary2.style.ImageAlgorithm;
import android.graphics.Bitmap;
import java.util.Arrays;
import com.sunmi.externalprinterlibrary2.style.AlignStyle;
import java.io.UnsupportedEncodingException;
import com.sunmi.externalprinterlibrary2.style.UnderlineStyle;
import com.sunmi.externalprinterlibrary2.task.ModelTask;
import com.sunmi.externalprinterlibrary2.task.SnTask;
import com.sunmi.externalprinterlibrary2.PropCallback;
import com.sunmi.externalprinterlibrary2.task.StatusTask;
import com.sunmi.externalprinterlibrary2.StatusCallback;
import com.sunmi.externalprinterlibrary2.utils.BytesUtil;
import com.sunmi.externalprinterlibrary2.style.CutterMode;
import com.sunmi.externalprinterlibrary2.task.SetTask;
import com.sunmi.externalprinterlibrary2.utils.EscUtil;
import com.sunmi.externalprinterlibrary2.task.TransTask;
import com.sunmi.externalprinterlibrary2.exceptions.PrinterException;
import com.sunmi.externalprinterlibrary2.ResultCallback;
import com.sunmi.externalprinterlibrary2.task.Priority;
import com.sunmi.externalprinterlibrary2.task.PriorityExecutor;
import com.sunmi.externalprinterlibrary2.ConnectCallback;
import android.content.Context;
import com.sunmi.externalprinterlibrary2.io.LanDevicePort;
import com.sunmi.externalprinterlibrary2.io.BtDevicePort;
import com.sunmi.externalprinterlibrary2.io.UsbDevicePort;
import com.sunmi.externalprinterlibrary2.style.EncodeType;
import java.util.concurrent.ExecutorService;
import com.sunmi.externalprinterlibrary2.io.DevicePort;
import com.sunmi.externalprinterlibrary2.api.Print;

public class CloudPrinter implements Print
{
    private static final int MAX_BUFFER_SIZE = 20000000;
    private final CloudPrinterInfo cloudPrinterInfo;
    private final DevicePort devicePort;
    private ExecutorService executorService;
    private byte[] cacheBuffer;
    private EncodeType type;

    public CloudPrinter(final String name, final int vid, final int pid) {
        this.cloudPrinterInfo = new CloudPrinterInfo(name, vid, pid);
        this.devicePort = (DevicePort)new UsbDevicePort(this.cloudPrinterInfo.vid, this.cloudPrinterInfo.pid);
    }

    public CloudPrinter(final String name, final String mac) {
        this.cloudPrinterInfo = new CloudPrinterInfo(name, mac);
        this.devicePort = (DevicePort)new BtDevicePort(this.cloudPrinterInfo.mac);
    }

    public CloudPrinter(final String name, final String address, final int port) {
        this.cloudPrinterInfo = new CloudPrinterInfo(name, address, port);
        this.devicePort = (DevicePort)new LanDevicePort(this.cloudPrinterInfo.address, this.cloudPrinterInfo.port);
    }

    public CloudPrinterInfo getCloudPrinterInfo() {
        return this.cloudPrinterInfo;
    }

    public boolean isConnected() {
        return this.devicePort.isConnected();
    }

    public void connect(final Context context, final ConnectCallback callback) {
        if (this.executorService == null) {
            this.executorService = (ExecutorService)new PriorityExecutor(true);
        }
        this.executorService.execute(new BaseTask(this.devicePort, EscUtil.customQuerySn(), Priority.HIGH) {
            public void run() {
                this.devicePort.connect(context, callback);
                if (this.devicePort instanceof LanDevicePort) {
                    if (CloudPrinter.this.isConnected()) {
                        byte[] recvBuffer = new byte[20];
                        int ret = this.devicePort.sendData(this.sendData);
                        if (ret == this.sendData.length) {
                            ret = this.devicePort.recvData(recvBuffer);
                            if (ret > 0) {
                                String sn = (new String(recvBuffer)).trim();
                                if (CloudPrinter.this.cloudPrinterInfo.name.startsWith("Printer")) {
                                    sn = sn.substring(sn.length() - 4);
                                    if (CloudPrinter.this.cloudPrinterInfo.name.endsWith(sn)) {
                                        if (callback != null) {
                                            callback.onConnect();
                                        }
                                    } else if (callback != null) {
                                        callback.onFailed("Ip change!");
                                    }
                                } else if (sn.length() == 14) {
                                    if (callback != null) {
                                        callback.onConnect();
                                    }
                                } else if (callback != null) {
                                    callback.onFailed("Invalid printer!");
                                }

                                return;
                            }
                        }
                    }

                    if (callback != null) {
                        callback.onFailed("Invalid printer!");
                    }
                }

            }
        });
    }

    public void release(final Context context) {
        if (this.executorService != null) {
            this.executorService.shutdownNow();
            this.executorService = null;
        }
        this.devicePort.release(context);
    }

    public void commitTransBuffer(final ResultCallback callback) {
        if (this.cacheBuffer == null || this.cacheBuffer.length == 0) {
            throw new PrinterException("Invalid print data!");
        }
        if (this.executorService != null) {
            this.executorService.execute((Runnable)new TransTask(this.devicePort, this.cacheBuffer, callback));
            this.cacheBuffer = null;
            return;
        }
        throw new PrinterException("The printer is not connected yet!");
    }

    public void clearTransBuffer() {
        this.cacheBuffer = null;
    }

    public void setPrintDensity(final int density) {
        if (this.executorService == null) {
            throw new PrinterException("The printer is not connected yet!");
        }
        this.executorService.execute((Runnable)new SetTask(this.devicePort, EscUtil.customDensity(density)));
    }

    public void setPrintSpeed(final int speed) {
        if (this.executorService == null) {
            throw new PrinterException("The printer is not connected yet!");
        }
        this.executorService.execute((Runnable)new SetTask(this.devicePort, EscUtil.customSpeed(speed)));
    }

    public void setPrintCutter(final CutterMode mode) {
        if (this.executorService == null) {
            throw new PrinterException("The printer is not connected yet!");
        }
        this.executorService.execute((Runnable)new SetTask(this.devicePort, EscUtil.customCutter(mode.ordinal())));
    }

    public void selectAsciiCharFont(final int select) {
        if (this.executorService == null) {
            throw new PrinterException("The printer is not connected yet!");
        }
        this.executorService.execute((Runnable)new SetTask(this.devicePort, EscUtil.customSetFont(20, select)));
    }

    public void selectCjkCharFont(final int select) {
        if (this.executorService == null) {
            throw new PrinterException("The printer is not connected yet!");
        }
        this.executorService.execute((Runnable)new SetTask(this.devicePort, EscUtil.customSetFont(21, select)));
    }

    public void selectOtherCharFont(final int select) {
        if (this.executorService == null) {
            throw new PrinterException("The printer is not connected yet!");
        }
        this.executorService.execute((Runnable)new SetTask(this.devicePort, EscUtil.customSetFont(22, select)));
    }

    public void setEncodeMode(final EncodeType type) {
        if (this.executorService == null) {
            throw new PrinterException("The printer is not connected yet!");
        }
        byte[] set;
        if (type == EncodeType.UTF_8) {
            set = EscUtil.customSetFont(3, 1);
        }
        else if (type == EncodeType.BIG5) {
            set = BytesUtil.byteMerger(EscUtil.customSetFont(3, 0), EscUtil.customSetFont(1, 1));
        }
        else if (type == EncodeType.SHIFT_JIS) {
            set = BytesUtil.byteMerger(EscUtil.customSetFont(3, 0), EscUtil.customSetFont(1, 11));
        }
        else if (type == EncodeType.JIS_0208) {
            set = BytesUtil.byteMerger(EscUtil.customSetFont(3, 0), EscUtil.customSetFont(1, 12));
        }
        else if (type == EncodeType.KSC_5601) {
            set = BytesUtil.byteMerger(EscUtil.customSetFont(3, 0), EscUtil.customSetFont(1, 21));
        }
        else if (type == EncodeType.ASCII) {
            set = BytesUtil.byteMerger(EscUtil.customSetFont(3, 0), EscUtil.customSetFont(1, 128));
        }
        else {
            set = BytesUtil.byteMerger(EscUtil.customSetFont(3, 0), EscUtil.customSetFont(1, 0));
        }
        this.executorService.execute((Runnable)new SetTask(this.devicePort, set));
        this.type = type;
    }

    public void restoreDefaultSettings() {
        if (this.executorService == null) {
            throw new PrinterException("The printer is not connected yet!");
        }
        final byte[] data = BytesUtil.byteMerger(new byte[][] { EscUtil.customDensity(255), EscUtil.customSpeed(255), EscUtil.customCutter(0), EscUtil.customSetFont(255, 255) });
        this.executorService.execute((Runnable)new SetTask(this.devicePort, data));
    }

    public void getDeviceState(final StatusCallback statusCallback) {
        if (statusCallback != null) {
            if (this.executorService == null) {
                throw new PrinterException("The printer is not connected yet!");
            }
            this.executorService.execute((Runnable)new StatusTask(this.devicePort, statusCallback));
        }
    }

    public void getDeviceSN(final PropCallback propCallback) {
        if (propCallback != null) {
            if (this.executorService == null) {
                throw new PrinterException("The printer is not connected yet!");
            }
            this.executorService.execute((Runnable)new SnTask(this.devicePort, propCallback));
        }
    }

    public void getDeviceModel(final PropCallback propCallback) {
        if (propCallback != null) {
            if (this.executorService == null) {
                throw new PrinterException("The printer is not connected yet!");
            }
            this.executorService.execute((Runnable)new ModelTask(this.devicePort, propCallback));
        }
    }

    public void appendRawData(final byte[] data) {
        this.asyncSendData(data);
    }

    public void initStyle() {
        this.asyncSendData(EscUtil.init());
    }

    public void setPrintWidth(final int printWidth) {
        this.asyncSendData(EscUtil.printArea(printWidth));
    }

    public void setLeftSpace(final int leftSpace) {
        this.asyncSendData(EscUtil.setLeftSpace(leftSpace));
    }

    public void restoreDefaultLineSpacing() {
        this.asyncSendData(EscUtil.setDefaultLineSpace());
    }

    public void setLineSpacing(final int lineSpacing) {
        this.asyncSendData(EscUtil.setLineSpace(lineSpacing));
    }

    public void setBlackWhiteReverseMode(final boolean enable) {
        this.asyncSendData(EscUtil.setInverse((int)(enable ? 1 : 0)));
    }

    public void setUnderlineMode(final UnderlineStyle mode) {
        int value = 0;
        if (mode == UnderlineStyle.ONE) {
            value = 1;
        }
        else if (mode == UnderlineStyle.TWO) {
            value = 2;
        }
        this.asyncSendData(EscUtil.setUnderline(value));
    }

    public void setBoldMode(final boolean enable) {
        this.asyncSendData(EscUtil.setBold((int)(enable ? 1 : 0)));
    }

    public void setUpsideDownMode(final boolean enable) {
        this.asyncSendData(EscUtil.setUpside((int)(enable ? 1 : 0)));
    }

    public void setCharacterSize(int characterWidth, int characterHeight) {
        if (characterHeight < 1 || characterHeight > 8) {
            throw new PrinterException("Invalid print param!");
        }
        if (characterWidth < 1 || characterWidth > 8) {
            throw new PrinterException("Invalid print param!");
        }
        this.asyncSendData(EscUtil.setZoom(--characterWidth, --characterHeight));
    }

    public void setAsciiSize(final int size) {
        this.asyncSendData(EscUtil.customSetFont(10, size));
    }

    public void setCjkSize(final int size) {
        this.asyncSendData(EscUtil.customSetFont(11, size));
    }

    public void setOtherSize(final int size) {
        this.asyncSendData(EscUtil.customSetFont(12, size));
    }

    public void appendText(final String text) {
        try {
            if (this.type == null) {
                this.type = EncodeType.GB18030;
            }
            final byte[] textData = text.getBytes(this.type.getType());
            this.asyncSendData(textData);
        }
        catch (UnsupportedEncodingException e) {
            throw new PrinterException(e.getMessage());
        }
    }

    public void printText(final String text) {
        if (!text.endsWith("\n")) {
            this.appendText(text + "\n");
        }
        else {
            this.appendText(text);
        }
    }

    public void printColumnsText(final String[] colsTextArr, final int[] colsWidthArr, final AlignStyle[] colsAlign) {
        if (colsTextArr == null || colsWidthArr == null || colsAlign == null) {
            throw new PrinterException("Invalid print param!");
        }
        if (colsTextArr.length != colsWidthArr.length || colsTextArr.length != colsAlign.length) {
            throw new PrinterException("Invalid print param!");
        }
        byte[] send = { 29, 33, 0 };
        try {
            send = BytesUtil.byteMerger(send, this.printRow(colsTextArr, colsWidthArr, colsAlign));
            this.asyncSendData(send);
        }
        catch (Exception e) {
            throw new PrinterException(e.getMessage());
        }
    }

    public void dotsFeed(final int dots) {
        this.asyncSendData(EscUtil.skipDots(dots));
    }

    public void lineFeed(final int lines) {
        this.asyncSendData(EscUtil.skipLine(lines));
    }

    public void horizontalTab(final int n) {
        if (n < 0) {
            throw new PrinterException("Invalid print param!");
        }
        final byte[] tabs = new byte[n];
        Arrays.fill(tabs, EscUtil.TAB);
        this.asyncSendData(tabs);
    }

    public void setAbsolutePrintPosition(final int horizontalPosition) {
        this.asyncSendData(EscUtil.moveAbsolutePos(horizontalPosition));
    }

    public void setRelativePrintPosition(final int horizontalPosition) {
        this.asyncSendData(EscUtil.moveRelativePos(horizontalPosition));
    }

    public void setAlignment(final AlignStyle alignment) {
        final int align = alignment.ordinal();
        this.asyncSendData(EscUtil.setAlign(align));
    }

    public void printImage(final Bitmap bitmap, final ImageAlgorithm mode) {
        if (bitmap == null) {
            throw new PrinterException("Invalid print param!");
        }
        final boolean adv = mode == ImageAlgorithm.DITHERING;
        this.asyncSendData(EscUtil.printBitmap(BitmapUtil.getBytesFromBitmap(bitmap, adv)));
    }

    public void printBarcode(final String text, final BarcodeType type, final int height, final int size, final HriStyle style) {
        if (TextUtils.isEmpty((CharSequence)text)) {
            throw new PrinterException("Invalid print param!");
        }
        if (size < 2 || size > 6 || height < 1 || height > 255) {
            throw new PrinterException("Invalid print param!");
        }
        final byte[] src = text.getBytes(StandardCharsets.US_ASCII);
        final int len = src.length;
        switch (type.ordinal()) {
            case 0: {
                if (len < 11 || len > 12) {
                    throw new PrinterException("Invalid print param!");
                }
                for (int i = 0; i < len; ++i) {
                    if (src[i] < 48 && src[i] > 57) {
                        throw new PrinterException("Invalid print param!");
                    }
                }
                break;
            }
            case 1: {
                if (len < 6 || len > 7) {
                    throw new PrinterException("Invalid print param!");
                }
                for (int i = 0; i < len; ++i) {
                    if (src[i] < 48 && src[i] > 57) {
                        throw new PrinterException("Invalid print param!");
                    }
                }
                break;
            }
            case 2: {
                if (len < 12 || len > 13) {
                    throw new PrinterException("Invalid print param!");
                }
                for (int i = 0; i < len; ++i) {
                    if (src[i] < 48 && src[i] > 57) {
                        throw new PrinterException("Invalid print param!");
                    }
                }
                break;
            }
            case 3: {
                if (len < 7 || len > 8) {
                    throw new PrinterException("Invalid print param!");
                }
                for (int i = 0; i < len; ++i) {
                    if (src[i] < 48 && src[i] > 57) {
                        throw new PrinterException("Invalid print param!");
                    }
                }
                break;
            }
            case 4: {
                if (len < 1 || len > 255) {
                    throw new PrinterException("Invalid print param!");
                }
                for (int i = 0; i < len; ++i) {
                    if (src[i] != 32 && src[i] != 36 && src[i] != 37 && src[i] != 43 && (src[i] < 45 || src[i] > 57) && (src[i] < 65 || src[i] > 90)) {
                        throw new PrinterException("Invalid print param!");
                    }
                }
                break;
            }
            case 5: {
                if (len < 1 || len > 255 || len % 2 != 0) {
                    throw new PrinterException("Invalid print param!");
                }
                for (int i = 0; i < len; ++i) {
                    if (src[i] < 48 || src[i] > 57) {
                        throw new PrinterException("Invalid print param!");
                    }
                }
                break;
            }
            case 6: {
                if (len < 1 || len > 255) {
                    throw new PrinterException("Invalid print param!");
                }
                for (int i = 0; i < len; ++i) {
                    if (src[i] != 36 && src[i] != 43 && (src[i] < 45 || src[i] > 58) && (src[i] < 65 || src[i] > 68)) {
                        throw new PrinterException("Invalid print param!");
                    }
                }
                break;
            }
            case 7:
            case 8: {
                break;
            }
            default: {
                throw new PrinterException("Invalid print param!");
            }
        }
        final byte[] send = BytesUtil.byteMerger(new byte[][] { EscUtil.getBarcodeWidth(size), EscUtil.getBarcodeHeight(height), EscUtil.getBarcodeHri(style.ordinal()), EscUtil.printBarcode(65 + type.ordinal(), len, src) });
        this.asyncSendData(send);
    }

    public void printQrcode(final String text, final int size, final ErrorLevel level) {
        if (TextUtils.isEmpty((CharSequence)text)) {
            throw new PrinterException("Invalid print param!");
        }
        if (size < 1 || size > 16) {
            throw new PrinterException("Invalid print param!");
        }
        final byte[] data = text.getBytes(StandardCharsets.UTF_8);
        final byte[] send = BytesUtil.byteMerger(new byte[][] { EscUtil.getQrcode(data), EscUtil.getQrcodeSize(size), EscUtil.getQrcodeError(48 + level.ordinal()), EscUtil.printQrcode() });
        this.asyncSendData(send);
    }

    public void cutPaper(final boolean full) {
        this.asyncSendData(EscUtil.cut((int)(full ? 0 : 1)));
    }

    public void postCutPaper(final boolean full, final int dis) {
        this.asyncSendData(EscUtil.cutMore(full ? 65 : 66, dis));
    }

    public void openCashBox() {
        if (this.executorService == null) {
            throw new PrinterException("The printer is not connected yet!");
        }
        this.executorService.execute((Runnable)new SetTask(this.devicePort, EscUtil.openCash()));
    }

    private void asyncSendData(final byte[] data) throws PrinterException {
        if (data == null) {
            return;
        }
        if (this.cacheBuffer != null && this.cacheBuffer.length + data.length > 20000000) {
            throw new PrinterException("Data buffer exceeded, please wait!");
        }
        if (this.cacheBuffer == null) {
            this.cacheBuffer = data;
        }
        else {
            this.cacheBuffer = BytesUtil.byteMerger(this.cacheBuffer, data);
        }
    }

    private byte[] printRow(final String[] colsTextArr, final int[] colsWidthArr, final AlignStyle[] colsAlign) throws Exception {
        if (this.type == null) {
            this.type = EncodeType.GB18030;
        }
        final ByteArrayOutputStream temp = new ByteArrayOutputStream();
        final int cols = colsTextArr.length;
        int totalWidth = 0;
        for (final int w : colsWidthArr) {
            totalWidth += w;
        }
        final int[] pos = new int[cols];
        while (true) {
            int row_pos = 0;
            boolean add = false;
            final byte[] unit = new byte[totalWidth + 1];
            Arrays.fill(unit, (byte)32);
            unit[unit.length - 1] = 10;
            for (int i = 0; i < cols; ++i) {
                final int col_width = colsWidthArr[i];
                final byte[] text = colsTextArr[i].getBytes(this.type.getType());
                final int length = text.length - pos[i];
                if (length <= col_width) {
                    switch (colsAlign[i].ordinal()) {
                        case 0: {
                            System.arraycopy(text, pos[i], unit, row_pos, length);
                            break;
                        }
                        case 1: {
                            final int space = col_width - length;
                            System.arraycopy(text, pos[i], unit, row_pos + space / 2, length);
                            break;
                        }
                        case 2: {
                            final int space = col_width - length;
                            System.arraycopy(text, pos[i], unit, row_pos + space, length);
                            break;
                        }
                        default: {
                            throw new PrinterException("Invalid print param!");
                        }
                    }
                    row_pos += col_width;
                    final int[] array = pos;
                    final int n = i;
                    array[n] += length;
                }
                else if (col_width > 1) {
                    int j;
                    for (j = 0; j < col_width; ++j) {
                        if ((text[pos[i] + j] & 0xFF) < 128) {
                            unit[row_pos + j] = text[pos[i] + j];
                        }
                        else {
                            if (j + 1 == col_width) {
                                break;
                            }
                            if ((text[pos[i] + j + 1] & 0xFF) > 57) {
                                System.arraycopy(text, pos[i] + j, unit, row_pos + j, 2);
                                ++j;
                            }
                            else {
                                if (j + 3 >= col_width) {
                                    break;
                                }
                                System.arraycopy(text, pos[i] + j, unit, row_pos + j, 4);
                                j += 3;
                            }
                        }
                    }
                    final int[] array2 = pos;
                    final int n2 = i;
                    array2[n2] += j;
                    row_pos += col_width;
                    add = true;
                }
                else if (col_width == 1) {
                    if (text[pos[i]] < 127) {
                        System.arraycopy(text, pos[i], unit, row_pos, 1);
                        final int[] array3 = pos;
                        final int n3 = i;
                        array3[n3] += col_width;
                        add = true;
                    }
                    row_pos += col_width;
                }
            }
            temp.write(unit);
            if (!add) {
                return temp.toByteArray();
            }
        }
    }
}