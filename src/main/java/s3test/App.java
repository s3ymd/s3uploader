package s3test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectRequest;

@SpringBootApplication
public class App {
	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}
}

@Configuration
class Config {
	@Bean
	public AmazonS3 s3client() {
		AmazonS3 s3client = AmazonS3ClientBuilder.standard().withRegion(Regions.AP_NORTHEAST_1).build();
		return s3client;
	}
}

@Controller
class FileController {
	// あらかじめAWS S3管理画面で作成したバケット名。
	// このバケットは公開設定する必要はない。
	private static final String BUCKET = "upload-sample-98273979347923";
	
	// S3クライアント。Configクラスで事前に準備しておく。
	@Autowired
	private AmazonS3 s3client;

	// ファイルのアップロードを行うためのフォームを表示する。
	@GetMapping("/upload")
	@ResponseBody
	public String upload() {
		StringBuilder sb = new StringBuilder();
		sb.append("<form method=\"post\" enctype=\"multipart/form-data\">");
		sb.append("<input type=\"file\" name=\"file\">");
		sb.append("<input type=\"submit\" value=\"upload\">");
		sb.append("</form>");
		return sb.toString();
	}

	// ファイルのアップロードを行う。
	// アップロードされたファイルにはランダムな番号（UUID）が付与される。
	@PostMapping("/upload")
	@ResponseBody
	public String upload(@RequestParam("file") MultipartFile multipartFile) throws IOException {
		// ランダムな番号 例：4698fd81-5941-4c49-b73f-9e97f8742d29
		String key = UUID.randomUUID().toString(); 
		
		// ファイルはEveryoneからREAD可能とする
		AccessControlList acl = new AccessControlList();
		acl.grantPermission(GroupGrantee.AllUsers, Permission.Read);
		
		// ファイル本体のストリーム
		InputStream is = multipartFile.getInputStream();
		
		// ファイルのMIMEタイプ
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType(multipartFile.getContentType());
		
		// S3へのアップロードを開始
		PutObjectRequest request;
		request = new PutObjectRequest(BUCKET, key, is, metadata);
		request.setAccessControlList(acl);
		s3client.putObject(request);
		
		// 生成したランダムな番号を画面に表示。ダウンロード時にこの番号が必要。
		return key;
	}

	// アップロードされたファイルのダウンロードを行う。
	// （アプリのベースURL）/download/（アップロード時に発行された番号）
	// の形式でアクセスする。
	// 内部的にはS3のURLに転送処理を行うだけ。
	@GetMapping("/download/{key}")
	public String download(@PathVariable("key") String key) {
		URL url = s3client.getUrl(BUCKET, key);
		return "redirect:" + url;
	}
}