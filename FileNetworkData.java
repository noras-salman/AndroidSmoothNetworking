
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by noras on 2018-11-15.
 */

public class FileNetworkData {

    //READ_EXTERNAL_STORAGE
    public static NetworkData sendExternalStorageFile(String path) throws IOException {
        File file = new File(path);
        return NetworkData.newFileDataInstance(getBaseName(path),fullyReadFileToBytes(file));
    }

    private static String getBaseName(String path){
        String[] tokens = path.split(".+?/(?=[^/]+$)");
        if(tokens.length==2){
            return tokens[1];
        }
        return path;
    }

    public static NetworkData sendAssetFile(Context context,String path) throws IOException {
        return NetworkData.newFileDataInstance(getBaseName(path),byteArrayFromInputStream(context.getAssets().open(path)));
    }

    public static byte[] byteArrayFromInputStream(InputStream inputStream) throws IOException {

        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        return output.toByteArray();
    }


    private static byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis = new FileInputStream(f);
        int read = fis.read(bytes, 0, size);
        if (read < size) {
            int remain = size - read;
            while (remain > 0) {
                read = fis.read(tmpBuff, 0, remain);
                System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                remain -= read;
            }
        }
        fis.close();
        return bytes;
    }


    // DIRECTORY_MUSIC   DIRECTORY_PICTURES
    public static void writeToDownload(Context context,NetworkData networkData) throws Exception {
        if(!networkData.isFileData())
            throw new Exception("Not file data");
        String path=Environment.getExternalStoragePublicDirectory(  Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()+"/"+getBaseName(networkData.getMetaData().fileName);
        FileOutputStream out = new FileOutputStream(new File(path));
        out.write(networkData.getRawData());
        out.close();
    }


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }


}
