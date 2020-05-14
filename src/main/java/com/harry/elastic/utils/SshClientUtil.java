package com.harry.elastic.utils;

import com.sshtools.j2ssh.SftpClient;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.sftp.SftpFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SshClientUtil {

    public static void main(String[] args) {
        writeToLinux();
    }

    public static void readFromLinux() {
        SshClient client = new SshClient();
        try {
            client.connect("192.168.1.184", (s, sshPublicKey) -> true);//Linux服务器IP
            //设置用户名和密码
            PasswordAuthenticationClient pwd = new PasswordAuthenticationClient();
            pwd.setUsername("root");
            pwd.setPassword("password");
            int result = client.authenticate(pwd);
            if (result == AuthenticationProtocolState.COMPLETE) {//如果连接完成
                SftpClient sftpClient = client.openSftpClient();
                List<SftpFile> list = sftpClient.ls("/home/bes/test/");
                for (SftpFile f : list) {
                    System.out.println(String.format("filename:%s,absolote path:%s", f.getFilename(), f.getAbsolutePath()));
                    if (f.getFilename().endsWith(".log")) {
                        OutputStream os = new FileOutputStream("d:/" + f.getFilename());
                        sftpClient.get("/home/bes/test/" + f.getFilename(), os);
                        //以行为单位读取文件start
                        Path path = Paths.get("d:/" + f.getFilename());
                        BufferedReader reader = null;
                        try {
                            System.out.println("以行为单位读取文件内容，一次读一整行：");
                            reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
                            String tempString;
                            int line = 1;//行号
                            while ((tempString = reader.readLine()) != null) {//一次读入一行，直到读入null为文件结束
                                System.out.println("line " + line + ": " + tempString);//显示行号
                                line++;
                            }
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (reader != null) {
                                try {
                                    reader.close();
                                } catch (IOException e1) {
                                }
                            }
                        }
                        //以行为单位读取文件end
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeToLinux() {
        SshClient client = new SshClient();
        try {
            client.connect("192.168.1.184", (s, sshPublicKey) -> true);//Linux服务器IP
            //设置用户名和密码
            PasswordAuthenticationClient pwd = new PasswordAuthenticationClient();
            pwd.setUsername("root");
            pwd.setPassword("password");
            int result = client.authenticate(pwd);
            if (result == AuthenticationProtocolState.COMPLETE) {//如果连接完成
                SftpClient sftpClient = client.openSftpClient();
                sftpClient.cd("/home/bes/test/");

                Path path = Paths.get("test02.log");
                StringBuffer context=new StringBuffer();
                String line=System.getProperty("line.separator");
                context.append("你好，中国").append(line);
                context.append("10:27:29.676 [ssh-connection 1] DEBUG com.sshtools.j2ssh.io.DynamicBuffer - Buffer size: 32768").append(line);
                context.append("10:27:29.676 [ssh-connection 1] DEBUG com.sshtools.j2ssh.subsystem.SubsystemMessageStore - Received SSH_FXP_NAME subsystem message").append(line);
                context.append("10:27:29.676 [ssh-connection 1] DEBUG com.sshtools.j2ssh.transport.Service - Finished processing SSH_MSG_CHANNEL_DATA").append(line);

                sftpClient.put(new ByteArrayInputStream(context.toString().getBytes(StandardCharsets.UTF_8)),path.toString());
                List<SftpFile> ls = sftpClient.ls();
                ls.forEach(file->{
                    System.out.println(String.format("%s==%s==>%b",
                            file.getFilename(),
                            path.getFileName(),
                            String.valueOf(file.getFilename()).equals(String.valueOf(path.getFileName()))
                            )
                    );
                    if (String.valueOf(file.getFilename()).equals(String.valueOf(path.getFileName()))){
                        System.out.println(file.getAbsolutePath());
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
