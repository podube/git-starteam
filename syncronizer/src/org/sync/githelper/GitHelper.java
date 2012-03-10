/*****************************************************************************
    This file is part of Git-Starteam.

    Git-Starteam is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Git-Starteam is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Git-Starteam.  If not, see <http://www.gnu.org/licenses/>.
******************************************************************************/
package org.sync.githelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.sync.ErrorEater;
import org.sync.RepositoryHelper;

public class GitHelper implements RepositoryHelper {

	private Thread gitQueryWorker;
	private Thread gitErrorStreamEater;
	private HashSet<String> trackedFiles;
	private int trackedFilesReturnCode;
	private String gitExecutable;
	
	public GitHelper(String preferedPath) {
		if(findExecutable(preferedPath)) {
			grabTrackedFiles();
		}
	}

	private boolean findExecutable(String preferedPath) {
		String os = System.getProperty("os.name");
		if(null != preferedPath) {
			String fileExtension = "";
			if(os.equalsIgnoreCase("Windows")) {
				fileExtension = ".exe";
			}
			File gitExec = new File(preferedPath + File.separator + "git" + fileExtension);
			if(gitExec.exists() && gitExec.canExecute()) {
				try {
					gitExecutable = gitExec.getCanonicalPath();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else {
			if(os.equalsIgnoreCase("Windows")) {
				File gitExec = new File("C:" + File.separator + "Program Files" + File.separator + 
						"Git" + File.separator + "bin" + File.separator + "git.exe");
				if(gitExec.exists() && gitExec.canExecute()) {
					try {
						gitExecutable = gitExec.getCanonicalPath();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					gitExec = new File("C:" + File.separator + "Program Files (x86)" + File.separator + 
							"Git" + File.separator + "bin" + File.separator + "git.exe");
					if(gitExec.exists() && gitExec.canExecute()) {
						try {
							gitExecutable = gitExec.getCanonicalPath();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			} else {
				gitExecutable = "git";
			}
		}
		return (null != gitExecutable);
	}

	private void grabTrackedFiles() {
		trackedFilesReturnCode = Integer.MAX_VALUE;
		trackedFiles = null;
		ProcessBuilder process = new ProcessBuilder();
		process.command(gitExecutable, "ls-files");
		process.directory(new File(System.getProperty("user.dir")));
		try {
			Process lsFiles = process.start();
			gitQueryWorker = new Thread(new GitLsFilesReader(lsFiles.getInputStream()));
			gitErrorStreamEater = new Thread(new ErrorEater(lsFiles.getErrorStream()));
			gitQueryWorker.start();
			gitErrorStreamEater.start();
			trackedFilesReturnCode = lsFiles.waitFor();
			gitQueryWorker.join();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<String> getListOfTrackedFile() {
		if(Integer.MAX_VALUE == trackedFilesReturnCode) {
			return null;
		}
		return (Set<String>) trackedFiles.clone();
	}

	private class GitLsFilesReader implements Runnable {

		private InputStream input;
		private GitLsFilesReader(InputStream in) {
			input = in;
		}
		
		@Override
		public void run() {
			HashSet<String> listOfTrackedFiles = new HashSet<String>();
			InputStreamReader reader = null;
			BufferedReader buffer = null;
			
			try {
				reader = new InputStreamReader(input);
				buffer = new BufferedReader(reader);
				
				String file = null;
				while(null != (file = buffer.readLine())) {
					listOfTrackedFiles.add(file);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if(null != buffer) {
					try {
						buffer.close();
					} catch (IOException e) {
					}
				}
				if(null != reader) {
					try {
						reader.close();
					} catch (IOException e) {
					}
				}
			}
			trackedFiles = listOfTrackedFiles;
		}
		
	}
}