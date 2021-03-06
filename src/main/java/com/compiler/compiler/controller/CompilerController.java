package com.compiler.compiler.controller;

import com.compiler.compiler.model.Response;
import com.compiler.compiler.model.Result;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Date;

enum Langage {
	Python,
	C,
	Cpp,
	Java
}

@RestController
@RequestMapping("/compiler")
public class CompilerController {
	
	Logger logger = LogManager.getLogger(CompilerController.class);
	
	// Python Compiler
	@RequestMapping(
			value = "python",
			method = RequestMethod.POST
	)
	public ResponseEntity<Object> compile_python(@RequestPart(value = "outputFile", required = true) MultipartFile outputFile,
	                                        @RequestPart(value = "sourceCode", required = true) MultipartFile sourceCode,
	                                        @RequestParam(value = "inputFile", required = false) MultipartFile inputFile,
	                                        @RequestParam(value = "timeLimit", required = true) int timeLimit,
	                                        @RequestParam(value = "memoryLimit", required = true) int memoryLimit
	) throws Exception {
		return compiler(outputFile, sourceCode, inputFile, timeLimit, memoryLimit, Langage.Python);
	}
	
	// C Compiler
	@RequestMapping(
			value = "c",
			method = RequestMethod.POST
	)
	public ResponseEntity<Object> compile_c(@RequestPart(value = "outputFile", required = true) MultipartFile outputFile,
	                                      @RequestPart(value = "sourceCode", required = true) MultipartFile sourceCode,
	                                      @RequestParam(value = "inputFile", required = false) MultipartFile inputFile,
	                                      @RequestParam(value = "timeLimit", required = true) int timeLimit,
	                                      @RequestParam(value = "memoryLimit", required = true) int memoryLimit
	) throws Exception {
		return compiler(outputFile, sourceCode, inputFile, timeLimit, memoryLimit, Langage.C);
	}
	
	// C++ Compiler
	@RequestMapping(
			value = "cpp",
			method = RequestMethod.POST
	)
	public ResponseEntity<Object> compile_cpp(@RequestPart(value = "outputFile", required = true) MultipartFile outputFile,
	                                        @RequestPart(value = "sourceCode", required = true) MultipartFile sourceCode,
	                                        @RequestParam(value = "inputFile", required = false) MultipartFile inputFile,
	                                        @RequestParam(value = "timeLimit", required = true) int timeLimit,
	                                        @RequestParam(value = "memoryLimit", required = true) int memoryLimit
	) throws Exception {
		return compiler(outputFile, sourceCode, inputFile, timeLimit, memoryLimit, Langage.Cpp);
	}
	
	// Java Compiler
	@RequestMapping(
			value = "java",
			method = RequestMethod.POST
	)
	public ResponseEntity<Object> compile_java(@RequestPart(value = "outputFile", required = true) MultipartFile outputFile,
	                                      @RequestPart(value = "sourceCode", required = true) MultipartFile sourceCode,
	                                      @RequestParam(value = "inputFile", required = false) MultipartFile inputFile,
	                                      @RequestParam(value = "timeLimit", required = true) int timeLimit,
	                                      @RequestParam(value = "memoryLimit", required = true) int memoryLimit
	) throws Exception {
		return compiler(outputFile, sourceCode, inputFile, timeLimit, memoryLimit, Langage.Java);
	}
	
	// save file
	private void saveUploadedFiles(MultipartFile file, String name) throws IOException {
		if (file.isEmpty())
			return;
		byte[] bytes = file.getBytes();
		Path path = Paths.get(name);
		Files.write(path, bytes);
	}
	
	// create Python entrypoint.sh file
	private void createPythonEntrypointFile(String fileName, int timeLimit, int memoryLimit, MultipartFile inputFile) {
		String executionCommand = inputFile == null
				? "timeout --signal=SIGTERM " + timeLimit + " python3 main.py" + "\n"
				: "timeout --signal=SIGTERM " + timeLimit + " python3 main.py" + " < " + inputFile.getOriginalFilename() + "\n";
		String content = "#!/usr/bin/env bash\n" +
				"ulimit -s " + memoryLimit + "\n" +
				executionCommand +
				"exit $?\n";
		OutputStream os = null;
		try {
			os = new FileOutputStream(new File("utility_py/entrypoint.sh"));
			os.write(content.getBytes(), 0, content.length());
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// create Java entrypoint.sh file
	private void createJavaEntrypointFile(String fileName, int timeLimit, int memoryLimit, MultipartFile inputFile) {
		String executionCommand = inputFile == null
				? "timeout --signal=SIGTERM " + timeLimit + " java " + fileName.substring(0,fileName.length() - 5) + "\n"
				: "timeout --signal=SIGTERM " + timeLimit + " java " + fileName.substring(0,fileName.length() - 5) + " < " + inputFile.getOriginalFilename() + "\n";
		String content = "#!/usr/bin/env bash\n" +
				"mv main.java " + fileName+ "\n" +
				"javac " + fileName + "\n" +
				"ret=$?\n" +
				"if [ $ret -ne 0 ]\n" +
				"then\n" +
				"  exit 2\n" +
				"fi\n" +
				"ulimit -s " + memoryLimit + "\n" +
				 executionCommand +
				"exit $?\n";
		OutputStream os = null;
		try {
			os = new FileOutputStream(new File("utility/entrypoint.sh"));
			os.write(content.getBytes(), 0, content.length());
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// create C entrypoint.sh file
	private void createCEntrypointFile(String fileName, int timeLimit, int memoryLimit, MultipartFile inputFile) {
		String executionCommand = inputFile == null
				? "timeout --signal=SIGTERM " + timeLimit + " ./exec " + "\n"
				: "timeout --signal=SIGTERM " + timeLimit + " ./exec " + " < " + inputFile.getOriginalFilename() + "\n";
		String content = "#!/usr/bin/env bash\n" +
				"gcc main.c" + " -o exec" + "\n" +
				"ret=$?\n" +
				"if [ $ret -ne 0 ]\n" +
				"then\n" +
				"  exit 2\n" +
				"fi\n" +
				"ulimit -s " + memoryLimit + "\n" +
				executionCommand +
				"exit $?\n";
		OutputStream os = null;
		try {
			os = new FileOutputStream(new File("utility_c/entrypoint.sh"));
			os.write(content.getBytes(), 0, content.length());
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// create CPP entrypoint.sh file
	private void createCppEntrypointFile(String fileName, int timeLimit, int memoryLimit, MultipartFile inputFile) {
		String executionCommand = inputFile == null
				? "timeout --signal=SIGTERM " + timeLimit + " ./exec " + "\n"
				: "timeout --signal=SIGTERM " + timeLimit + " ./exec " + " < " + inputFile.getOriginalFilename() + "\n";
		String content = "#!/usr/bin/env bash\n" +
				"g++ main.cpp" + " -o exec" + "\n" +
				"ret=$?\n" +
				"if [ $ret -ne 0 ]\n" +
				"then\n" +
				"  exit 2\n" +
				"fi\n" +
				"ulimit -s " + memoryLimit + "\n" +
				executionCommand +
				"exit $?\n";
		OutputStream os = null;
		try {
			os = new FileOutputStream(new File("utility_cpp/entrypoint.sh"));
			os.write(content.getBytes(), 0, content.length());
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Compile method
	private ResponseEntity<Object> compiler(
			MultipartFile outputFile,
			MultipartFile sourceCode,
			MultipartFile inputFile,
			int timeLimit,
			int memoryLimit,
			Langage langage
	) throws Exception {
		String folder = "utility";
		String file = "main";
		if(langage == Langage.C) {
			folder += "_c";
			file += ".c";
		} else if(langage == Langage.Java) {
			file += ".java";
		} else if(langage == Langage.Cpp) {
			folder += "_cpp";
			file += ".cpp";
		} else {
			folder += "_py";
			file += ".py";
		}
		
		if(memoryLimit < 0 || memoryLimit > 1000)
			return ResponseEntity
					.badRequest()
					.body("Error memoryLimit must be between 0Mb and 1000Mb");
		
		if(timeLimit < 0 || timeLimit > 15)
			return ResponseEntity
					.badRequest()
					.body("Error timeLimit must be between 0 Sec and 15 Sec");
		
		LocalDateTime date = LocalDateTime.now();
		
		createEntrypointFile(sourceCode, inputFile, timeLimit, memoryLimit, langage);
		
		logger.info("entrypoint.sh file has been created");
		
		saveUploadedFiles(sourceCode, folder + "/" + file);
		saveUploadedFiles(outputFile, folder + "/" + outputFile.getOriginalFilename());
		if(inputFile != null)
			saveUploadedFiles(inputFile, folder + "/" + inputFile.getOriginalFilename());
		logger.info("Files has been uploaded");
		
		String imageName = "compile" + new Date().getTime();
		
		Result result = runCode(folder, imageName, outputFile);
		
		String statusResponse = result.getVerdict();
		logger.info("Status response is " + statusResponse);
		
		// delete files
		deleteFile(folder, file);
		deleteFile(folder,outputFile.getOriginalFilename());
		deleteFile(folder,inputFile.getOriginalFilename());
		
		
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(new Response(result.getOutput(), result.getExpectedOutput(), statusResponse, date));
	}
	
	private int buildImage(String folder, String imageName) throws InterruptedException, IOException {
		String[] dockerCommand = new String[] {"docker", "image", "build", folder, "-t", imageName};
		ProcessBuilder processbuilder = new ProcessBuilder(dockerCommand);
		Process process = processbuilder.start();
		return process.waitFor();
	}
	
	private Result runCode(String folder, String imageName, MultipartFile outputFile) throws InterruptedException, IOException {
		logger.info("Building the docker image");
		int status = buildImage(folder, imageName);
		if(status == 0)
			logger.info("Docker image has been built");
		else
			logger.info("Error while building image");
		
		logger.info("Running the container");
		String[] dockerCommand = new String[] {"docker", "run", "--rm", imageName};
		ProcessBuilder processbuilder = new ProcessBuilder(dockerCommand);
		Process process = processbuilder.start();
		status = process.waitFor();
		logger.info("End of the execution of the container");
		
		BufferedReader outputReader = new BufferedReader(new InputStreamReader(outputFile.getInputStream()));
		StringBuilder outputBuilder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		StringBuilder builder = new StringBuilder();
		
		boolean result = checkCode(outputReader, outputBuilder, reader, builder);
		String statusResponse = statusResponse(status, result);
		
		return new Result(statusResponse, builder.toString(), outputBuilder.toString());
		
	}
	
	private boolean checkCode(BufferedReader outputReader, StringBuilder outputBuilder, BufferedReader reader, StringBuilder builder) throws IOException {
		String line = null;
		String outputLine = null;
		boolean ans = true;
		
		while ( (line = reader.readLine()) != null && (outputLine = outputReader.readLine()) != null) {
			if(!line.equals(outputLine))
				ans = false;
			builder.append(line);
			builder.append(System.getProperty("line.separator"));
			
			outputBuilder.append(outputLine);
			outputBuilder.append(System.getProperty("line.separator"));
		}
		
		if(line != null) {
			builder.append(line);
			builder.append(System.getProperty("line.separator"));
		}
		
		if(outputLine != null) {
			outputBuilder.append(outputLine);
			outputBuilder.append(System.getProperty("line.separator"));
		}
		
		while ( (line = reader.readLine()) != null) {
			ans = false;
			builder.append(line);
			builder.append(System.getProperty("line.separator"));
		}
		
		while ( (outputLine = outputReader.readLine()) != null) {
			ans = false;
			outputBuilder.append(outputLine);
			outputBuilder.append(System.getProperty("line.separator"));
		}
		return ans;
	}
	
	private boolean deleteFile(String folder, String file) {
		if(folder != null && file != null) {
			String fileName = folder + "/" + file;
			new File(fileName).delete();
			logger.info("file " + fileName + " has been deleted");
			return true;
		}
		return false;
	}
	
	private String statusResponse(int status, boolean ans) {
		String statusResponse;
		if(status == 0) {
			if(ans)
				statusResponse = "Accepted";
			else
				statusResponse = "Wrong Answer";
		}
		else if(status == 2)
			statusResponse = "Compilation Error";
		else if(status == 1)
			statusResponse = "Runtime Error";
		else if(status == 139)
			statusResponse = "Out Of Memory";
		else
			statusResponse = "Time Limit Exceeded";
		return statusResponse;
	}
	
	private void createEntrypointFile(MultipartFile sourceCode, MultipartFile inputFile, int timeLimit, int memoryLimit, Langage langage) {
		if(langage == Langage.Java) {
			createJavaEntrypointFile(sourceCode.getOriginalFilename(), timeLimit, memoryLimit, inputFile);
		} else if(langage == Langage.C) {
			createCEntrypointFile(sourceCode.getOriginalFilename(), timeLimit, memoryLimit, inputFile);
		} else if(langage == Langage.Cpp) {
			createCppEntrypointFile(sourceCode.getOriginalFilename(), timeLimit, memoryLimit, inputFile);
		} else {
			createPythonEntrypointFile(sourceCode.getOriginalFilename(), timeLimit, memoryLimit, inputFile);
		}
	}
	
	
}
