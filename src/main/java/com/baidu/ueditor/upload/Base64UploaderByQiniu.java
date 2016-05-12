package com.baidu.ueditor.upload;

import com.baidu.ueditor.PathFormat;
import com.baidu.ueditor.define.AppInfo;
import com.baidu.ueditor.define.BaseState;
import com.baidu.ueditor.define.FileType;
import com.baidu.ueditor.define.State;
import com.tontisa.common.lang.Strings;
import com.tontisa.commons.cloudstore.UploadStatus;
import com.tontisa.commons.cloudstore.qiniu.QiniuCloudStorerUtil;

import java.util.Map;

import org.apache.commons.codec.binary.Base64;

public final class Base64UploaderByQiniu {

	public static State save(String content, Map<String, Object> conf) {
		
		byte[] data = decode(content);

		long maxSize = ((Long) conf.get("maxSize")).longValue();

		if (!validSize(data, maxSize)) {
			return new BaseState(false, AppInfo.MAX_SIZE);
		}

		String suffix = FileType.getSuffix("JPG");

		//maolujun
		String savePath = PathFormat.parse((String) conf.get("savePath"),
				(String) conf.get("filename"), conf);
		
		savePath = savePath + suffix;
		/*String physicalPath = (String) conf.get("rootPath") + savePath;

		State storageState = StorageManager.saveBinaryFile(data, physicalPath);*/

		//七牛存储
		String bucket = Strings.defaultString((String) conf.get("image.bucket"));
		QiniuCloudStorerUtil qiniuCloudStoreUtil = QiniuCloudStorerUtil.getInstance(conf);
		UploadStatus uploadStatus = qiniuCloudStoreUtil.upload(bucket, savePath, data);
		if (UploadStatus.FAIL == uploadStatus) {
			return new BaseState(false, AppInfo.NOT_ALLOW_FILE_TYPE);
		}
		State storageState = new BaseState(true);
		
		if (storageState.isSuccess()) {
			storageState.putInfo("url", PathFormat.format(savePath));
			storageState.putInfo("type", suffix);
			storageState.putInfo("original", "");
		}

		return storageState;
	}

	private static byte[] decode(String content) {
		return Base64.decodeBase64(content);
	}

	private static boolean validSize(byte[] data, long length) {
		return data.length <= length;
	}
	
}