package com.baidu.ueditor.hunter;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.baidu.ueditor.PathFormat;
import com.baidu.ueditor.define.AppInfo;
import com.baidu.ueditor.define.BaseState;
import com.baidu.ueditor.define.MIMEType;
import com.baidu.ueditor.define.MultiState;
import com.baidu.ueditor.define.State;
import com.tontisa.common.lang.Strings;
import com.tontisa.commons.cloudstore.UploadStatus;
import com.tontisa.commons.cloudstore.qiniu.QiniuCloudStorerUtil;

/**
 * 图片抓取器
 * @author hancong03@baidu.com
 * @author renlei
 *
 */
public class ImageHunter {

	private String filename = null;
	private String savePath = null;
	private String rootPath = null;
	private List<String> allowTypes = null;
	private long maxSize = -1;
	
	private List<String> filters = null;
	
	//maolujun
	private Map<String, Object> conf = null;
	
	public ImageHunter ( Map<String, Object> conf ) {
		
		this.filename = (String)conf.get( "filename" );
		this.savePath = (String)conf.get( "savePath" );
		this.rootPath = (String)conf.get( "rootPath" );
		this.maxSize = (Long)conf.get( "maxSize" );
		this.allowTypes = Arrays.asList( (String[])conf.get( "allowFiles" ) );
		this.filters = Arrays.asList( (String[])conf.get( "filter" ) );
		//maolujun
		this.conf = conf;
		
	}
	
	public State capture ( String[] list ) {
		
		MultiState state = new MultiState( true );
		
		for ( String source : list ) {
			state.addState( captureRemoteData( source ) );
		}
		
		return state;
		
	}

	public State captureRemoteData ( String urlStr ) {
		
		HttpURLConnection connection = null;
		URL url = null;
		String suffix = null;
		byte[] data = null;
		InputStream is = null;
		
		try {
			url = new URL( urlStr );

			if ( !validHost( url.getHost() ) ) {
				return new BaseState( false, AppInfo.PREVENT_HOST );
			}
			connection = (HttpURLConnection) url.openConnection();
			connection.setInstanceFollowRedirects( true );
			connection.setUseCaches( true );
		
			if ( !validContentState( connection.getResponseCode() ) ) {
				return new BaseState( false, AppInfo.CONNECTION_ERROR );
			}
			
			suffix = MIMEType.getSuffix( connection.getContentType() );
			if ( !validFileType( suffix ) ) {
				return new BaseState( false, AppInfo.NOT_ALLOW_FILE_TYPE );
			}
			
			if ( !validFileSize( connection.getContentLength() ) ) {
				return new BaseState( false, AppInfo.MAX_SIZE );
			}
			
			//*******
			long maxSize = ((Long) conf.get("maxSize")).longValue();

			//maolujun
			String savePath = PathFormat.parse((String) conf.get("savePath"),
					(String) conf.get("filename"), conf);
			
			savePath = savePath + suffix;
			/*String physicalPath = (String) conf.get("rootPath") + savePath;

			State storageState = StorageManager.saveBinaryFile(data, physicalPath);*/

			//七牛存储
			String bucket = Strings.defaultString((String) conf.get("image.bucket"));
			QiniuCloudStorerUtil qiniuCloudStoreUtil = QiniuCloudStorerUtil.getInstance(conf);
			is = connection.getInputStream();
			ByteArrayOutputStream output = new ByteArrayOutputStream();
		    byte[] buffer = new byte[4096];
		    int n = 0;
		    while (-1 != (n = is.read(buffer))) {
		        output.write(buffer, 0, n);
		    }
			data = output.toByteArray();
			UploadStatus uploadStatus = qiniuCloudStoreUtil.upload(bucket, savePath, data);
			buffer = null;
			
			if (UploadStatus.FAIL == uploadStatus) {
				return new BaseState(false, AppInfo.NOT_ALLOW_FILE_TYPE);
			}
			//*********
			
			
			//String savePath = this.getPath( this.savePath, this.filename, suffix );
			//String physicalPath = this.rootPath + savePath;

			//State state = StorageManager.saveFileByInputStream( connection.getInputStream(), physicalPath );
			State state = new BaseState(true);
			if ( state.isSuccess() ) {
				//返回七牛完整路径--此种方式可以兼容以前直接上传在本地的图片访问方式，唯一的缺点就是七牛空间域名固定，不可修改
				//storageState.putInfo("url", bucketObj.getFileUrl(saveFileName));
				//处理文件名特殊字符~!@#$%^&(){}[]【】';,.-=+_。，；：“‘、空格-->~%21@%23$%25%5E&%28){}%5B]';,.-=+_。，；：“‘、_1468218495036.jpg
				String fullUrl = qiniuCloudStoreUtil.getFileURL(bucket, savePath).replace("%", "%25").replace(" ", "%20").replace("^", "%5E").replace("!", "%21").replace("#", "%23");
				state.putInfo( "url", fullUrl);
				state.putInfo( "source", urlStr );
			}
			
			return state;
			
		} catch ( Exception e ) {
			return new BaseState( false, AppInfo.REMOTE_FAIL );
		} finally {
			if (is != null) {
				try {
					is.close();
					is = null;
				} catch (Exception e2) {
				}
			}
		}
		
	}
	
	private String getPath ( String savePath, String filename, String suffix  ) {
		//maolujun
		return PathFormat.parse( savePath + suffix, filename, this.conf );
		
	}
	
	private boolean validHost ( String hostname ) {
		try {
			InetAddress ip = InetAddress.getByName(hostname);
			
			if (ip.isSiteLocalAddress()) {
				return false;
			}
		} catch (UnknownHostException e) {
			return false;
		}
		
		return !filters.contains( hostname );
		
	}
	
	private boolean validContentState ( int code ) {
		
		return HttpURLConnection.HTTP_OK == code;
		
	}
	
	private boolean validFileType ( String type ) {
		
		return this.allowTypes.contains( type );
		
	}
	
	private boolean validFileSize ( int size ) {
		return size < this.maxSize;
	}
	
}
