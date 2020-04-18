package com.mtaketani.createIssueCsv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * IssueのCSVファイルを出力
 */
public class CreateIssueCsv 
{
    public static void main( String[] args )
    {
    	System.setProperty("file.encoding", "UTF-8");
        
    	System.out.println("検索したいユーザ名を入力ください。");
        Scanner scan = new Scanner(System.in);
        String user = scan.nextLine();
        if(StringUtils.isEmpty(user)) {
        	System.out.println("ユーザ名が入っていないので処理を終了します。");
        	scan.close();
        	return;
        }
        
    	System.out.println("検索したいリポジトリ名を入力ください。");
        String repo = scan.nextLine();
        if(StringUtils.isEmpty(repo)) {
        	System.out.println("リポジトリ名が入っていないので処理を終了します。");
        	scan.close();
        	return;
        }
        System.out.println("出力ファイルをフルパスで入力ください。");
        String filePath = scan.nextLine();
        if(StringUtils.isEmpty(filePath)) {
        	System.out.println("ファイル名が入っていないので処理を終了します。");
        	scan.close();
        	return;
        }
        System.out.println("ページ数を入力ください（1ページ 100Issue）");
        int page = scan.nextInt();
        scan.close();
    	
        
		HttpURLConnection  urlConn = null;
		InputStream in = null;
		BufferedReader reader = null;
		Issue[] issues = null;
		try {
			
			for(int i = 0; i < page; i++) {
				String strUrl = "https://api.github.com/repos/" + user 
		                + "/" + repo + "/issues?state=all&per_page=100&page=" 
				        + Integer.toString(i + 1);
		
				URL url = new URL(strUrl);
				//Get Connection
				urlConn = (HttpURLConnection) url.openConnection();

				urlConn.setRequestMethod("GET");

				urlConn.connect();

				int status = urlConn.getResponseCode();

				if (status == HttpURLConnection.HTTP_OK) {

					in = urlConn.getInputStream();

					reader = new BufferedReader(new InputStreamReader(in));

					StringBuilder output = new StringBuilder();
					String line;

					//JSONオブジェクト
					ObjectMapper mapper = new ObjectMapper();

					while ((line = reader.readLine()) != null) {
						output.append(line);
					}
					System.out.println(output.toString());
					if("[]".equals(output.toString())) {
						break;
					}
					issues = mapper.readValue(output.toString(), Issue[].class);
				}else {
					return;
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
				if (urlConn != null) {
					urlConn.disconnect();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		//output Issues
		if(issues != null) {
			
			File file = new File(filePath);
			
			//exist file
			if(file.exists()) {
				System.out.print("ファイルが存在します。削除してから実行ください。");
				return;
			}

			try(PrintWriter pw = new PrintWriter(
					new BufferedWriter(
						new OutputStreamWriter(
							new FileOutputStream(filePath),"Shift-JIS")))) {
				
				pw.write("#,ステータス,タイトル,作成者,作成日,終了日,URL\r\n");
				//body
	            for(int i = 0; i < issues.length; i++) {
	            	Issue issue = issues[i];
	            	
	            	String closedDate = 
	            	    (StringUtils.isNotEmpty(issue.closed_at)) ? issue.closed_at : "          ";
	            	pw.write(issue.number
							+ "," + issue.state
							+ ",\"" + issue.title + "\""
							+ "," + issue.user.login
							+ "," + issue.created_at.substring(0, 10)
							+ "," + closedDate.substring(0, 10)
							+ "," + issue.html_url + "\r\n");	
	            }

	        } catch (Exception e) {
	            e.printStackTrace();
	        }
			
		}
		System.out.print("処理を終了");
    }
}

/**
 * GithubのIssue 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class Issue {
    public String html_url;
    public int number;
    public String title;
    public User user;
    public String state;
    public String created_at;
    public String closed_at;
}

/**
 * GithubのUser 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class User {
    public String login;
    public String html_url;
}
