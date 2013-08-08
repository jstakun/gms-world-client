/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import android.os.Handler;
import com.jstakun.gms.android.ui.lib.R;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author jstakun
 */
public class MessageStack {

    public static final int LAYER_LOADED = 2;
    public static final int MAP_LOADED = 0;
    public static final int LOADING = 1;
    
    public static final int STATUS_MESSAGE = 200;
    public static final int STATUS_GONE = 201;
    public static final int STATUS_VISIBLE = 202;
    
    private Stack<MessageTimerTask> messageStack = new Stack<MessageTimerTask>();
    private Handler uiHandler;
    private MessageCondition messageCondition;

    public MessageStack(MessageCondition messageCondition) {
       this.messageCondition = messageCondition;
    }

    public void setHandler(Handler uiHandler) {
        this.uiHandler = uiHandler;
    }
    
    public void addMessage(String message, int validity, int condition, int setLoadingImage) {
         if (setLoadingImage == MAP_LOADED || setLoadingImage == LOADING || setLoadingImage == LAYER_LOADED) {
            if ((setLoadingImage == MAP_LOADED && !messageCondition.isLoading(MessageCondition.LAYER_LOADING)) ||
                    (setLoadingImage == LAYER_LOADED && !messageCondition.isLoading(MessageCondition.MAP_LOADING)))
            {
                uiHandler.sendEmptyMessage(STATUS_GONE);
            } else if (setLoadingImage == LOADING) {
                uiHandler.sendEmptyMessage(STATUS_VISIBLE);
            }
        }
        messageStack.push(new MessageTimerTask(message, validity, this, condition));
        updateStatusBar();
    }

    private void updateStatusBar() {
        uiHandler.sendEmptyMessage(STATUS_MESSAGE);
    }

    public void removeConditionalMessage(boolean hideLoadingImage, int loaderType) {
        if (hideLoadingImage) {
            if ( (loaderType == LAYER_LOADED && !messageCondition.isLoading(MessageCondition.MAP_LOADING)) ||
                (loaderType == MAP_LOADED && !messageCondition.isLoading(MessageCondition.LAYER_LOADING)) )
            {
                uiHandler.sendEmptyMessage(STATUS_GONE);
            }
        }
        
        updateStatusBar();
    }

    public String getMessage() {
        //System.out.println("Calling getMessage");

        //String msg = null;
        while (!messageStack.empty()) {
            MessageTimerTask message = messageStack.peek();
            if (message.isActive()) {
                return message.getMessage();
                //break;
            } else {
                messageStack.pop();
            }
        }

        //System.out.println("Returning: " + msg);
        return Locale.getMessage(R.string.Status_bar_default);
    }

    private class MessageTimerTask extends TimerTask {

        private boolean status = true;
        private String message;
        private MessageStack messageStack;
        private int condition = -1;

        public MessageTimerTask(String message, int sec, MessageStack messageStack, int condition) {
            this.message = message;
            this.messageStack = messageStack;

            if (condition != -1) {
                this.condition = condition;
            } else if (sec > 0) {
                initTimer(sec);
            }
        }

        private void initTimer(int sec) {
            Timer timer = new Timer();
            timer.schedule(this, sec * 1000);
        }

        public void run() {
            status = false;
            messageStack.updateStatusBar();
        }

        private boolean isActive() {
            if (condition != -1) {
                status = messageCondition.isLoading(condition);
            }

            return status;
        }

        private String getMessage() {
            return message;
        }
    }
}
