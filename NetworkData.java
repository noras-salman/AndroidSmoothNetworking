
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static com.nasable.themeing.MetaData.META_TYPE_FILE;
import static com.nasable.themeing.MetaData.META_TYPE_MESSAGE;
import static com.nasable.themeing.MetaData.META_TYPE_RAW_BYTE;

/**
 * Created by noras on 2018-11-15.
 */

public  class NetworkData {
    MetaData metaData;
    byte[] rawData;


    public static NetworkData newStringDataInstance(String data){
        return new NetworkData(new MetaData(META_TYPE_MESSAGE),data.getBytes());
    }

    public static NetworkData newFileDataInstance(String fileName,byte[] data){
        return new NetworkData(new MetaData(META_TYPE_FILE,fileName), data );
    }

    public static NetworkData newRawDataInstance(byte[] data){
        return new NetworkData(new MetaData(META_TYPE_RAW_BYTE), data );
    }


    public NetworkData(byte[] array) {

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(array);
        int metaLength = 256 +1;
        int restLength = array.length - metaLength;
        byte[] meta = new byte[metaLength];
        byte[] rest = new byte[restLength];

        System.arraycopy(array, 0, meta, 0, metaLength);
        System.arraycopy(array, metaLength, rest, 0, restLength);

        this.rawData = rest;
        this.metaData = new MetaData(meta);


    }

    public NetworkData(MetaData metaData, byte[] rawData) {
        this.metaData = metaData;
        this.rawData = rawData;

    }

    public boolean isStringData() {
        return metaData.getDataType().equals(META_TYPE_MESSAGE);
    }

    public boolean isFileData() {
        return metaData.getDataType().equals(META_TYPE_FILE);
    }

    public boolean isRawData() {
        return metaData.getDataType().equals(META_TYPE_RAW_BYTE);
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public byte[] getRawData() {
        return rawData;
    }

    public String getStringData() {
        return new String(rawData);
    }

    public byte[] asByteArray() {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        builder.write(this.metaData.asByteArray(), 0, this.metaData.asByteArray().length);
        builder.write(this.rawData, 0, this.rawData.length);
        return builder.toByteArray();
    }



}