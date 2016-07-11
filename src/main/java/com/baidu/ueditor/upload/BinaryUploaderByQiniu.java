package com.baidu.ueditor.upload;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.baidu.ueditor.PathFormat;
import com.baidu.ueditor.define.AppInfo;
import com.baidu.ueditor.define.BaseState;
import com.baidu.ueditor.define.FileType;
import com.baidu.ueditor.define.State;
import com.tontisa.common.lang.Strings;
import com.tontisa.commons.cloudstore.UploadStatus;
import com.tontisa.commons.cloudstore.qiniu.QiniuCloudStorerUtil;

/**
 * 基于七牛云存储
 * @author renlei
 *
 */
public class BinaryUploaderByQiniu {

	public static final State save(HttpServletRequest request,
			Map<String, Object> conf) {
		boolean isAjaxUpload = request.getHeader( "X_Requested_With" ) != null;

		if (!ServletFileUpload.isMultipartContent(request)) {
			return new BaseState(false, AppInfo.NOT_MULTIPART_CONTENT);
		}

		ServletFileUpload upload = new ServletFileUpload(
				new DiskFileItemFactory());

        if ( isAjaxUpload ) {
            upload.setHeaderEncoding( "UTF-8" );
        }

		try {
			FileItem fileItem = null;
			//读取上传的文件
			List<FileItem> items = upload.parseRequest(request);
			for (FileItem item : items) {
				if (!item.isFormField()) {
					fileItem = item;
					break;
				} else {
					fileItem = null;
				} 
			}
			if (fileItem == null) {
				return new BaseState(false, AppInfo.NOTFOUND_UPLOAD_DATA);
			}
			
			String savePath = (String) conf.get("savePath");
			String originFileName = fileItem.getName();
			String suffix = FileType.getSuffixByFilename(originFileName);

			originFileName = originFileName.substring(0,
					originFileName.length() - suffix.length());
			savePath = savePath + suffix;
			
			long maxSize = ((Long) conf.get("maxSize")).longValue();
			if (fileItem.getSize() > maxSize) {
				return new BaseState(false, AppInfo.MAX_SIZE);
			}
			
			//String fileName = fileItem.getName();
			//String suffix = FileType.getSuffixByFilename(fileName);
			
			if (!validType(suffix, (String[]) conf.get("allowFiles"))) {
				return new BaseState(false, AppInfo.NOT_ALLOW_FILE_TYPE);
			}
			//maolujun
			savePath = PathFormat.parse(savePath, originFileName, conf);
			
			//七牛存储
			String bucket = Strings.defaultString((String) conf.get("image.bucket"));
			QiniuCloudStorerUtil qiniuCloudStoreUtil = QiniuCloudStorerUtil.getInstance(conf);
			UploadStatus uploadStatus = qiniuCloudStoreUtil.upload(bucket, savePath, fileItem.get());
			if (UploadStatus.FAIL == uploadStatus) {
				return new BaseState(false, AppInfo.NOT_ALLOW_FILE_TYPE);
			}
			
			
			State storageState = new BaseState(true);
			if (storageState.isSuccess()) {
				//返回七牛完整路径--此种方式可以兼容以前直接上传在本地的图片访问方式，唯一的缺点就是七牛空间域名固定，不可修改
				//storageState.putInfo("url", bucketObj.getFileUrl(saveFileName));
				//处理文件名特殊字符~!@#$%^&(){}[]【】';,.-=+_。，；：“‘、空格-->~%21@%23$%25%5E&%28){}%5B]';,.-=+_。，；：“‘、_1468218495036.jpg
				String url = qiniuCloudStoreUtil.getFileURL(bucket, savePath).replace("%", "%25").replace(" ", "%20").replace("^", "%5E").replace("!", "%21").replace("#", "%23");
				storageState.putInfo("url", url);
				storageState.putInfo("type", suffix);
				storageState.putInfo("original", originFileName + suffix);
			}
			return storageState;
		} catch (FileUploadException e) {
			return new BaseState(false, AppInfo.PARSE_REQUEST_ERROR);
		} catch (Exception e) {
		}
		return new BaseState(false, AppInfo.IO_ERROR);
	}

	private static boolean validType(String type, String[] allowTypes) {
		List<String> list = Arrays.asList(allowTypes);

		return list.contains(type);
	}
	
	public static void main(String[] args) {
		String t = "QQ图片20160520112312    的_1465004368252";
		System.out.println(t.replace(" ", "%20"));
	}
}
