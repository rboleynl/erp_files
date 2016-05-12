package com.baidu.ueditor.upload;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.baidu.ueditor.define.State;
import com.tontisa.common.lang.Strings;

public class Uploader {
	private HttpServletRequest request = null;
	private Map<String, Object> conf = null;

	public Uploader(HttpServletRequest request, Map<String, Object> conf) {
		this.request = request;
		this.conf = conf;
	}

	public final State doExec() {
		String filedName = (String) this.conf.get("fieldName");
		State state = null;

		if ("true".equals(this.conf.get("isBase64"))) {
			//替换为七牛存储方式_renlei
			if (this.conf.get("qiniu.flag") != null && Strings.equals("true", (String)this.conf.get("qiniu.flag"))) {
				state = Base64UploaderByQiniu.save(this.request.getParameter(filedName),
						this.conf);
			}else{
				state = Base64Uploader.save(this.request.getParameter(filedName),
						this.conf);
			}
		} else {
			//替换为七牛存储方式_renlei
			if (this.conf.get("qiniu.flag") != null && Strings.equals("true", (String)this.conf.get("qiniu.flag"))) {
				state = BinaryUploaderByQiniu.save(this.request, this.conf);
			}else{
				state = BinaryUploader.save(this.request, this.conf);
			}
		}

		return state;
	}
}
