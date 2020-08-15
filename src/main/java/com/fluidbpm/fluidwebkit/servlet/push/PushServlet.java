/*
 * Koekiebox CONFIDENTIAL
 *
 * [2012] - [2017] Koekiebox (Pty) Ltd
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property
 * of Koekiebox and its suppliers, if any. The intellectual and
 * technical concepts contained herein are proprietary to Koekiebox
 * and its suppliers and may be covered by South African and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material is strictly
 * forbidden unless prior written permission is obtained from Koekiebox Innovations.
 */

package com.fluidbpm.fluidwebkit.servlet.push;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.login.NotificationsBean;
import com.fluidbpm.fluidwebkit.ds.FluidClientDS;
import com.fluidbpm.fluidwebkit.ds.FluidClientPool;
import com.fluidbpm.fluidwebkit.exception.WebSessionExpiredException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

/**
 * Background servlet used to check for login status.
 */
@WebServlet(value = "/webkit_push",
		name = "WebKit Push Servlet",
		asyncSupported = false,
		displayName = "WebKit Push Servlet",
		loadOnStartup = 1)
public class PushServlet extends HttpServlet {

	public static final String APPLICATION_JSON_STRING = "application/json";

	@Inject
	private transient FluidClientPool fcp;

	@Inject
	private transient NotificationsBean notificationsBean;

	private static final long WAIT_TIME_NOTI_FETCH_MILLIS = TimeUnit.SECONDS.toMillis(60);

	public static final class ErrorCode {
		public static final int USER_NOT_LOGGED_IN = 1;
	}

	/**
	 *
	 */
	public static class FieldMapping {
		//Error...
		public static final String ERROR = "error";
		public static final String ERROR_CODE = "error_code";

		public static final String DELAY = "delay";

		public static final String COMMANDS = "commands";
		public static final String COMMAND = "command";

		//Chat...
		public static final String TEXT_HEADER = "textHeader";
		public static final String TEXT = "text";
		public static final String MAKE_USE_OF_REMOTE_COMMAND = "makeUseOfRemoteCommand";
	}

	/**
	 *
	 * @param sessionId
	 * @return
	 */
	public FluidClientDS getFluidClientDS(String sessionId) {
		try {
			return this.fcp.get(sessionId);
		} catch (WebSessionExpiredException weExcept) {
			return null;
		}
	}

	/**
	 *
	 * @param httpServletRequestParam
	 * @param httpServletResponseParam
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doGet(HttpServletRequest httpServletRequestParam, HttpServletResponse httpServletResponseParam) throws ServletException, IOException {
		this.doPost(httpServletRequestParam, httpServletResponseParam);
	}

	/**
	 *
	 * @param httpServletRequestParam
	 * @param httpServletResponseParam
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doPost(
		HttpServletRequest httpServletRequestParam,
		HttpServletResponse httpServletResponseParam
	) throws ServletException, IOException {
		this.notificationsBean.setSessionIdFallback(null);
		HttpSession httpSession = httpServletRequestParam.getSession(false);
		Object loggedInUser = null;
		if (httpSession != null) {
			loggedInUser = httpSession.getAttribute(ABaseManagedBean.SessionVariable.USER);
		}

		JSONObject returnObject = new JSONObject();
		try {
			//User not logged in...
			if (loggedInUser == null) {
				returnObject.put(FieldMapping.ERROR, "User not logged in. Refresh browser.");
				returnObject.put(FieldMapping.ERROR_CODE, ErrorCode.USER_NOT_LOGGED_IN);
				//Invalidate...
				if (httpSession != null) {
					httpSession.invalidate();
				}
			} else {
				//User Logged in...
				this.notificationsBean.setSessionIdFallback(httpSession.getId());
				PushSessionUtil pushSessionUtil = new PushSessionUtil();
				boolean anyProcessed = false;

				//Check and Notify of any new User Notifications...
				anyProcessed = this.processWindowSize(httpServletRequestParam, httpSession) ? true: anyProcessed;
				anyProcessed = this.processUserNotifications(httpServletRequestParam, httpSession) ? true: anyProcessed;
				if (anyProcessed) {
					pushSessionUtil.clearEventBus(httpSession);
				}
			}
		} catch (JSONException jsonExcept) {
			//JSON issue...
			this.notificationsBean.raiseError(jsonExcept);
		}

		PrintWriter printWriter = httpServletResponseParam.getWriter();
		httpServletResponseParam.setContentType(APPLICATION_JSON_STRING);
		httpServletResponseParam.setHeader("Access-Control-Allow-Origin","*");
		httpServletResponseParam.setHeader("Access-Control-Allow-Methods","POST, GET");

		printWriter.write(returnObject.toString());
		printWriter.flush();
		printWriter.close();
	}

	/**
	 *
	 * @param httpServletRequestParam
	 * @param httpSessionParam
	 * @return
	 * @throws JSONException
	 */
	private boolean processUserNotifications(
		HttpServletRequest httpServletRequestParam,
		HttpSession httpSessionParam
	) throws JSONException {
		switch (this.notificationsBean.getNotificationState()) {
			case UnreadNotificationsFull:
				return false;
			default:
				if ((this.notificationsBean.getLastNotificationFetch() + WAIT_TIME_NOTI_FETCH_MILLIS) < System.currentTimeMillis()) {
					int lastUnreadCount = this.notificationsBean.getTotalNumberOfNotifications();
					this.notificationsBean.actionUpdateNotifications();
					int lastUnreadCountUpdated = this.notificationsBean.getTotalNumberOfNotifications();
					if (lastUnreadCount != lastUnreadCountUpdated) {
						new PushSessionUtil().publishCommandToEventBus(
								httpSessionParam,
								PushSessionUtil.Commands.REFRESH_USER_NOTIFICATION);
					}
				}
		}

		return false;
	}
	
	private boolean processWindowSize(
		HttpServletRequest httpServletRequestParam, 
		HttpSession httpSessionParam
	) {
		//Window width and height...
		String userBrowserWindWidth =
				httpServletRequestParam.getParameter(
						ABaseManagedBean.SessionVariable.USER_BROWSER_WINDOW_WIDTH);
		if (userBrowserWindWidth != null) {
			httpSessionParam.setAttribute(
					ABaseManagedBean.SessionVariable.USER_BROWSER_WINDOW_WIDTH,
					Integer.parseInt(userBrowserWindWidth));
		}

		String userBrowserWindHeight =
				httpServletRequestParam.getParameter(
						ABaseManagedBean.SessionVariable.USER_BROWSER_WINDOW_HEIGHT);
		if (userBrowserWindHeight != null) {
			httpSessionParam.setAttribute(
					ABaseManagedBean.SessionVariable.USER_BROWSER_WINDOW_HEIGHT,
					Integer.parseInt(userBrowserWindHeight));
		}

		return false;
	}
}
