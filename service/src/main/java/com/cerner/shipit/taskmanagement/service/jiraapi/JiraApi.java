package com.cerner.shipit.taskmanagement.service.jiraapi;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cerner.shipit.taskmanagement.exception.TaskManagementServiceException;
import com.cerner.shipit.taskmanagement.utility.constant.ErrorCodes;
import com.cerner.shipit.taskmanagement.utility.constant.ErrorMessages;
import com.cerner.shipit.taskmanagement.utility.constant.GeneralConstants;
import com.cerner.shipit.taskmanagement.utility.constant.MethodConstants;
import com.cerner.shipit.taskmanagement.utility.tos.AuthTO;
import com.cerner.shipit.taskmanagement.utility.tos.LoginResponseTO;
import com.cerner.shipit.taskmanagement.utility.tos.WorkLogDetailsTO;
import com.cerner.shipit.taskmanagement.utility.tos.WorkLogInfoTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class JiraApi {

	Logger logger = LoggerFactory.getLogger(JiraApi.class);

	public String getInProgressJiraDetailsByUserId(String userId) throws TaskManagementServiceException {
		logger.debug(GeneralConstants.LOGGER_FORMAT, GeneralConstants.METHOD_START,
				MethodConstants.GET_INPROGRESS_JIRADETAILS_BY_USERID);
		String jiraDetails;
		HttpURLConnection connection = null;
		try {
			URL jiraURL = new URL("https://jira2.cerner.com/rest/api/2/search?jql=assignee=%22" + userId
					+ "%22+AND+status%20in%20(\"IN%20PROGRESS\",\"IN%20REVIEW\")");
			connection = (HttpURLConnection) jiraURL.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();
			Reader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
			jiraDetails = IOUtils.toString(in);
		} catch (final UnsupportedEncodingException e) {
			logger.error(ErrorCodes.E01, ErrorMessages.FAILED_TO_FETCH_JIRA_DETAILS);
			throw new TaskManagementServiceException(ErrorCodes.E01, ErrorMessages.FAILED_TO_FETCH_JIRA_DETAILS);
		} catch (final ClientProtocolException e) {
			logger.error(ErrorCodes.E02, ErrorMessages.FAILED_TO_FETCH_JIRA_DETAILS);
			throw new TaskManagementServiceException(ErrorCodes.E02, ErrorMessages.FAILED_TO_FETCH_JIRA_DETAILS);
		} catch (final IOException e) {
			logger.error(ErrorCodes.E03, ErrorMessages.FAILED_TO_FETCH_JIRA_DETAILS);
			throw new TaskManagementServiceException(ErrorCodes.E03, ErrorMessages.FAILED_TO_FETCH_JIRA_DETAILS);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

		logger.debug(GeneralConstants.LOGGER_FORMAT, GeneralConstants.METHOD_END,
				MethodConstants.GET_INPROGRESS_JIRADETAILS_BY_USERID);
		return jiraDetails;
	}

	public String getWorkLogDataByJiraId(String issueKey) throws TaskManagementServiceException {
		logger.debug(GeneralConstants.LOGGER_FORMAT, GeneralConstants.METHOD_START,
				MethodConstants.GET_WORKLOG_DATA_BY_JIRAID);
		String jiraDetails;
		HttpURLConnection connection = null;
		try {
			URL jiraURL = new URL("https://jira2.cerner.com/rest/api/2/issue/" + issueKey + "/worklog");
			connection = (HttpURLConnection) jiraURL.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();
			Reader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
			jiraDetails = IOUtils.toString(in);
		} catch (final UnsupportedEncodingException e) {
			logger.error(ErrorCodes.E01, ErrorMessages.FAILED_TO_FETCH_WORKLOG_DETAILS);
			throw new TaskManagementServiceException(ErrorCodes.E01, ErrorMessages.FAILED_TO_FETCH_WORKLOG_DETAILS);
		} catch (final ClientProtocolException e) {
			logger.error(ErrorCodes.E02, ErrorMessages.FAILED_TO_FETCH_WORKLOG_DETAILS);
			throw new TaskManagementServiceException(ErrorCodes.E02, ErrorMessages.FAILED_TO_FETCH_WORKLOG_DETAILS);
		} catch (final IOException e) {
			logger.error(ErrorCodes.E03, ErrorMessages.FAILED_TO_FETCH_WORKLOG_DETAILS);
			throw new TaskManagementServiceException(ErrorCodes.E03, ErrorMessages.FAILED_TO_FETCH_WORKLOG_DETAILS);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

		logger.debug(GeneralConstants.LOGGER_FORMAT, GeneralConstants.METHOD_END,
				MethodConstants.GET_WORKLOG_DATA_BY_JIRAID);
		return jiraDetails;
	}

	public int addWorkLog(WorkLogInfoTO workLogInfo) throws TaskManagementServiceException {
		HttpURLConnection connection = null;
		int response = 0;
		try {
			String auth = new String(
					Base64.encodeBase64((workLogInfo.getUserName() + ":" + workLogInfo.getPassword()).getBytes()));
			List<WorkLogDetailsTO> workLog = workLogInfo.getWorklogs();
			Iterator<WorkLogDetailsTO> iterator = workLog.iterator();
			while (iterator.hasNext()) {
				WorkLogDetailsTO temp = iterator.next();
				URL jiraURL;
				jiraURL = new URL("https://jira2.cerner.com/rest/api/latest/issue/" + temp.getId()
						+ "/worklog?adjustEstimate=AUTO&newEstimate&reduceBy");
				temp.setId("");
				connection = (HttpURLConnection) jiraURL.openConnection();
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestProperty("Accept", "application/json");
				connection.setRequestProperty("Authorization", "Basic " + auth);
				connection.setDoOutput(true);
				DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				ObjectMapper obj = new ObjectMapper();
				String jsonStr = obj.writeValueAsString(temp);
				wr.writeBytes(jsonStr);
				wr.flush();
				wr.close();
				connection.connect();
				if (connection.getResponseCode() == 201) {
					response = connection.getResponseCode();
				} else if (connection.getResponseCode() == 403) {
					throw new TaskManagementServiceException(ErrorCodes.W05,
							ErrorMessages.WORKLOG_FAILED_TO_ADD_WITH_FORBIDDEN_ERROR);
				} else {
					throw new TaskManagementServiceException(ErrorCodes.W02, ErrorMessages.WORKLOG_FAILED_TO_ADD);
				}
			}
		} catch (MalformedURLException e) {
			throw new TaskManagementServiceException(ErrorCodes.W03,
					ErrorMessages.WORKLOG_FAILED_TO_ADD_DUE_TO_MALFORMEDURLEXCEPTION);
		} catch (IOException e) {
			throw new TaskManagementServiceException(ErrorCodes.W04,
					ErrorMessages.WORKLOG_FAILED_TO_ADD_DUE_TO_IOEXCEPTION);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

		return response;

	}

	public LoginResponseTO authenticateUser(String userName, String password) throws TaskManagementServiceException {
		URL jiraURL;
		LoginResponseTO response = new LoginResponseTO();
		try {
			jiraURL = new URL("https://jira2.cerner.com");
			HttpURLConnection connection = (HttpURLConnection) jiraURL.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "*/*");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);
			connection.setDoInput(true);
			String userCredentials = userName + ":" + password;
			String basicAuth = "Basic " + new String(new Base64().encode(userCredentials.getBytes()));
			connection.setRequestProperty("Authorization", basicAuth);
			connection.connect();
			if (connection.getResponseCode() != 200) {
				throw new TaskManagementServiceException(ErrorCodes.E05, ErrorMessages.FAILED_TO_AUTHENTICATE_USER);
			} else {
				response.setStatus(connection.getResponseCode());
			}
		} catch (final IOException e) {
			throw new TaskManagementServiceException(ErrorCodes.E05, ErrorMessages.FAILED_TO_AUTHENTICATE_USER);
		}
		return response;
	}

	public String getJiraSearchDetails(String issueKey) throws TaskManagementServiceException {
		logger.debug(GeneralConstants.LOGGER_FORMAT, GeneralConstants.METHOD_START,
				MethodConstants.GET_JIRASEARCHDETAILS);
		String jiraDetails;
		try {
			URL jiraURL = new URL("https://jira2.cerner.com/rest/api/2/search?jql=issue=%22" + issueKey + "%22");
			HttpURLConnection connection = (HttpURLConnection) jiraURL.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();
			Reader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
			jiraDetails = IOUtils.toString(in);
		} catch (final UnsupportedEncodingException e) {
			logger.error(ErrorCodes.JS01, ErrorMessages.FAILED_TO_FETCH_JIRA_DETAILS);
			throw new TaskManagementServiceException(ErrorCodes.JS01, ErrorMessages.FAILED_TO_FETCH_JIRA_DETAILS);
		} catch (final ClientProtocolException e) {
			logger.error(ErrorCodes.JS02, ErrorMessages.FAILED_TO_FETCH_JIRA_DETAILS);
			throw new TaskManagementServiceException(ErrorCodes.JS02, ErrorMessages.FAILED_TO_FETCH_JIRA_DETAILS);
		} catch (final IOException e) {
			logger.error(ErrorCodes.JS03, ErrorMessages.FAILED_TO_FETCH_JIRA_DETAILS);
			throw new TaskManagementServiceException(ErrorCodes.JS03, ErrorMessages.FAILED_TO_FETCH_JIRA_DETAILS);
		}

		logger.debug(GeneralConstants.LOGGER_FORMAT, GeneralConstants.METHOD_END,
				MethodConstants.GET_JIRASEARCHDETAILS);
		return jiraDetails;

	}

	public LoginResponseTO authenticateUserforCookies(String userName, String password)
			throws TaskManagementServiceException {
		URL jiraURL;
		int responseCode = 0;
		LoginResponseTO loginresponseTO = new LoginResponseTO();
		AuthTO authTO = new AuthTO();
		try {
			jiraURL = new URL("https://jira2.cerner.com/rest/auth/1/session");
			HttpURLConnection connection = (HttpURLConnection) jiraURL.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Accept", "*/*");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);
			connection.setDoInput(true);
			authTO.setUsername(userName);
			authTO.setPassword(password);
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			ObjectMapper obj = new ObjectMapper();
			String jsonStr = obj.writeValueAsString(authTO);
			wr.writeBytes(jsonStr);
			wr.flush();
			wr.close();
			connection.connect();
			responseCode = connection.getResponseCode();
			if (responseCode != 200) {
				throw new TaskManagementServiceException(ErrorCodes.E05, ErrorMessages.FAILED_TO_AUTHENTICATE_USER);
			} else {
				loginresponseTO.setStatus(responseCode);
				loginresponseTO.setMessage(connection.getResponseMessage());
				loginresponseTO.setHeaders(connection.getHeaderFields());
			}
		} catch (final IOException e) {
			throw new TaskManagementServiceException(ErrorCodes.E05, ErrorMessages.FAILED_TO_AUTHENTICATE_USER);
		}
		return loginresponseTO;
	}
}
