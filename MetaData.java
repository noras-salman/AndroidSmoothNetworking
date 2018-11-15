
import java.io.ByteArrayOutputStream;

/**
 * Created by noras on 2018-11-15.
 */

public class MetaData {
    String dataType;
    String fileName;

    public static final String META_TYPE_MESSAGE = "S";
    public static final String META_TYPE_RAW_BYTE = "R";
    public static final String META_TYPE_FILE = "F";


    public MetaData(byte[] array) {

        byte[] type = new byte[1];
        byte[] name = new byte[256];

        System.arraycopy(array, 0, type, 0, 1);
        System.arraycopy(array, 1, name, 0, 256);

        dataType = new String(type).replace("\0", "");
        fileName = new String(name).replace("\0", "");

    }

    public MetaData(String dataType) {
        this.dataType = dataType;
        this.fileName = "";
    }

    public MetaData(String dataType, String fileName) {
        this.dataType = dataType;
        this.fileName = fileName;
    }

    public String getDataType() {
        return dataType;
    }

    public String getFileName() {
        return fileName;
    }


    public byte[] asByteArray() {

        byte[] DATA_TYPE = new byte[1];
        byte[] dataTypeBytes = dataType.getBytes();

        for (int i = 0; i < dataTypeBytes.length; i++) {
            DATA_TYPE[i] = dataTypeBytes[i];
        }

            /*  https://en.wikipedia.org/wiki/Comparison_of_file_systems#Limits  */
        byte[] FILE_NAME = new byte[256];
        byte[] fileNameBytes = fileName.getBytes();

        for (int i = 0; i < fileNameBytes.length; i++) {
            FILE_NAME[i] = fileNameBytes[i];
        }


        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        builder.write(DATA_TYPE, 0, DATA_TYPE.length);
        builder.write(FILE_NAME, 0, FILE_NAME.length);

        return builder.toByteArray();

    }


}